/*
 * Created by Andrey Markelov 06-09-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.AttachmentUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *Event listener.
 * 
 * @author Andrey Markelov
 */
public class IssueClonerByComponents
    implements InitializingBean, DisposableBean
{
    /*
     * Logger.
     */
    private static Log log = LogFactory.getLog(IssueClonerByComponents.class);

    /**
     * Link type.
     */
    private static final String LINK_TYPE = "Depends";

    /**
     * Plug-In data manager.
     */
    private final CloneCompMgr cloneCompMgr;

    /**
     * Event publisher.
     */
    private EventPublisher eventPublisher;

    /**
     * Constructor.
     */
    public IssueClonerByComponents(
        EventPublisher eventPublisher,
        CloneCompMgr cloneCompMgr)
    {
        this.eventPublisher = eventPublisher;
        this.cloneCompMgr = cloneCompMgr;
    }

    @Override
    public void afterPropertiesSet()
    throws Exception
    {
        //--> register ourselves with the EventPublisher
        eventPublisher.register(this);
    }

    @Override
    public void destroy()
    throws Exception
    {
        //--> unregister ourselves with the EventPublisher
        eventPublisher.unregister(this);
    }

    /**
     * Check that Plug-In is configured for the project.
     */
    private boolean isApplyPlugin(Long projId)
    {
        String[] projKeys = cloneCompMgr.getProjectKeys();
        if (projKeys != null)
        {
            int inx = Arrays.binarySearch(projKeys, projId.toString());
            if (inx >= 0)
            {
                return true;
            }
            else
            {
                return false;
            }
        }

        return false;
    }

    @EventListener
    public void onIssueEvent(IssueEvent issueEvent)
    {
        if (!issueEvent.getEventTypeId().equals(EventType.ISSUE_CREATED_ID))
        {
            return;
        }

        Issue issue = issueEvent.getIssue();
        Collection<ProjectComponent> comps = issue.getComponentObjects();
        if (comps == null || comps.size() < 2)
        {
            return;
        }

        Project project = issue.getProjectObject();
        if (project == null || !isApplyPlugin(project.getId()))
        {
            return;
        }

        List<Issue> newIssues = new ArrayList<Issue>();
        ProjectComponent gv = null;
        Iterator<ProjectComponent> iter = comps.iterator();
        while (iter.hasNext())
        {
            gv = iter.next();

            MutableIssue nissue = ComponentManager.getInstance().getIssueFactory().getIssue();
            //--> summary
            nissue.setSummary(String.format("[%s] %s", gv.getName(), issue.getSummary()));
            //--> project
            if (issue.getProjectObject() != null)
            {
                nissue.setProjectId(issue.getProjectObject().getId());
            }
            //--> issue type
            if (issue.getIssueTypeObject() != null)
            {
                nissue.setIssueTypeId(issue.getIssueTypeObject().getId());
            }
            //--> components
            Collection<ProjectComponent> nComps = new LinkedList<ProjectComponent>();
            nComps.add(gv);
            nissue.setComponentObjects(nComps);
            //--> assignee
            String compLead = gv.getLead();
            nissue.setAssigneeId(compLead);
            //--> reporter
            nissue.setReporter(issueEvent.getUser());
            //--> priority
            nissue.setPriorityObject(issue.getPriorityObject());
            //--> description
            nissue.setDescription(issue.getDescription());
            //--> env
            nissue.setEnvironment(issue.getEnvironment());
            //--> due date
            nissue.setDueDate(issue.getDueDate());
            //--> estimate
            nissue.setEstimate(issue.getEstimate());
            //--> labels
            nissue.setLabels(issue.getLabels());
            nissue.setAffectedVersions(issue.getAffectedVersions());
            nissue.setWorkflowId(issue.getWorkflowId());
            nissue.setParentId(issue.getParentId());

            //--> status
            if (issue.getStatusObject() != null)
            {
                nissue.setStatusId(issue.getStatusObject().getId());
            }

            //--> resolution
            if (issue.getResolutionObject() != null)
            {
                nissue.setResolutionId(issue.getResolutionObject().getId());
            }

            nissue.setFixVersions(issue.getFixVersions());
            nissue.setResolutionDate(issue.getResolutionDate());
            nissue.setTimeSpent(issue.getTimeSpent());
            nissue.setVotes(issue.getVotes());
            nissue.setCreated(issue.getCreated());
            nissue.setSecurityLevelId(issue.getSecurityLevelId());
            nissue.setOriginalEstimate(issue.getOriginalEstimate());

            List<CustomField> cfs = ComponentManager.getInstance().getCustomFieldManager().getCustomFieldObjects(issue);
            if (cfs != null)
            {
                for (CustomField cf : cfs)
                {
                    Object cfVal = issue.getCustomFieldValue(cf);
                    if (cfVal != null)
                    {
                        nissue.setCustomFieldValue(cf, cfVal);
                    }
                }
            }

            //--> create issue
            try
            {
                Issue newIssueObj = ComponentManager.getInstance().getIssueManager().createIssueObject(issueEvent.getUser(), nissue);
                newIssues.add(newIssueObj);
            }
            catch (CreateException crex)
            {
                log.error("IssueClonerByComponents::onIssueEvent - Cannot create dependent issues", crex);
            }
        }

        Collection<Attachment> atts = issue.getAttachments();
        if (atts != null)
        {
            AttachmentManager am = ComponentManager.getInstance().getAttachmentManager();
            for (Attachment att : atts)
            {
                File attFile = AttachmentUtils.getAttachmentFile(att);
                String filename = att.getFilename();
                String contentType = att.getMimetype();
                for (Issue nissue : newIssues)
                {
                    File newFile = new File(attFile.getAbsolutePath() + nissue.getKey());
                    try
                    {
                        FileUtils.copyFile(attFile, newFile);
                        am.createAttachment(newFile, filename, contentType, issueEvent.getUser(), nissue);
                    }
                    catch (Exception ex)
                    {
                        log.error("IssueClonerByComponents::onIssueEvent - Cannot copy attachment", ex);
                    }
                }
            }
        }

        IssueLinkTypeManager issueLinkTypeManager = ComponentManager.getComponentInstanceOfType(IssueLinkTypeManager.class);
        Collection<IssueLinkType> types = issueLinkTypeManager.getIssueLinkTypesByName(LINK_TYPE);
        if (types == null || types.isEmpty())
        {
            return;
        }

        IssueLinkType ilt = types.iterator().next();
        if (ilt != null)
        {
            IssueLinkManager ilm = ComponentManager.getInstance().getIssueLinkManager();
            for (Issue nissue : newIssues)
            {
                try
                {
                    ilm.createIssueLink(issue.getId(), nissue.getId(), ilt.getId(), null, issueEvent.getUser());
                }
                catch (CreateException crex)
                {
                    log.error("IssueClonerByComponents::onIssueEvent - Cannot create link", crex);
                }
            }
        }
    }
}
