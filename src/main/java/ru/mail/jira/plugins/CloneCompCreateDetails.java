/*
 * Created by Andrey Markelov 06-09-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.ofbiz.core.entity.GenericValue;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.util.AttachmentUtils;
import com.atlassian.jira.web.action.issue.CreateIssueDetails;
import com.atlassian.jira.web.action.issue.IssueCreationHelperBean;

/**
 * Clone tasks for components during creation.
 * 
 * @author Andrey Markelov
 */
public class CloneCompCreateDetails
extends CreateIssueDetails
{
    /**
     * Unique ID.
     */
    private static final long serialVersionUID = 4922348018971733978L;

    /**
     * Link type.
     */
    private static final String LINK_TYPE = "Depends";

    /**
     * Plug-In data manager.
     */
    private final CloneCompMgr cloneCompMgr;

    /**
     * Constructor.
     */
    public CloneCompCreateDetails(
        IssueFactory issueFactory,
        IssueCreationHelperBean issueCreationHelperBean,
        IssueService issueService,
        CloneCompMgr cloneCompMgr)
    {
        super(issueFactory, issueCreationHelperBean, issueService);
        this.cloneCompMgr = cloneCompMgr;
    }

    @Override
    protected String doExecute()
    throws Exception
    {
        boolean applyPlugin = isApplyPlugin(getProject().getLong("id"));

        if (!applyPlugin)
        {
            return super.doExecute();
        }

        //--> loop by components
        List<MutableIssue> newIssues = new ArrayList<MutableIssue>();
        MutableIssue issue = getIssueObject();
        Collection<GenericValue> comps = issue.getComponents();
        if (comps != null && comps.size() > 1)
        {
            GenericValue gv = null;
            Iterator<GenericValue> iter = comps.iterator();
            while (iter.hasNext())
            {
                gv = iter.next();

                MutableIssue nissue = ComponentManager.getInstance().getIssueFactory().getIssue();
                //--> summary
                nissue.setSummary(issue.getSummary());
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
                Collection<GenericValue> nComps = new LinkedList<GenericValue>();
                nComps.add(gv);
                nissue.setComponents(nComps);
                //--> assignee
                String compLead = gv.getString("lead");
                nissue.setAssigneeId(compLead);
                //--> reporter
                nissue.setReporter(getLoggedInUser());
                //--> priority
                nissue.setPriority(issue.getPriority());
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

                //--> custom fields
                @SuppressWarnings("rawtypes")
                List cfs = getCustomFields(issue);
                if (cfs != null)
                {
                    for (Object cf : cfs)
                    {
                        if (cf instanceof CustomField)
                        {
                            CustomField cfObj = (CustomField)cf;
                            Object cfVal = issue.getCustomFieldValue(cfObj);
                            if (cfVal != null)
                            {
                                nissue.setCustomFieldValue(cfObj, cfVal);
                            }
                        }
                    }
                }

                //--> create issue
                try
                {
                    ComponentManager.getInstance().getIssueManager().createIssue(getLoggedInUser(), nissue);
                }
                catch (CreateException crex)
                {
                    addErrorMessage(crex.getMessage());
                    return ERROR;
                }
                newIssues.add(nissue);

                iter.remove();
            }
        }
        //<--

        String rc_str = super.doExecute();
        if (!rc_str.equals(ERROR) && applyPlugin)
        {
            Collection<Attachment> atts = issue.getAttachments();
            if (atts != null)
            {
                AttachmentManager am = ComponentManager.getInstance().getAttachmentManager();
                for (Attachment att : atts)
                {
                    File attFile = AttachmentUtils.getAttachmentFile(att);
                    String filename = att.getFilename();
                    String contentType = att.getMimetype();
                    Date createTime = att.getCreated();
                    for (MutableIssue nissue : newIssues)
                    {
                        File newFile = new File(attFile.getAbsolutePath() + nissue.getKey());
                        FileUtils.copyFile(attFile, newFile);
                        am.createAttachment(newFile, filename, contentType, getLoggedInUser(), nissue.getGenericValue(), null, createTime);
                    }
                }
            }

            IssueLinkTypeManager issueLinkTypeManager = ComponentManager.getComponentInstanceOfType(IssueLinkTypeManager.class);
            Collection<IssueLinkType> types = issueLinkTypeManager.getIssueLinkTypesByName(LINK_TYPE);
            if (types == null || types.isEmpty())
            {
                return rc_str;
            }

            IssueLinkType ilt = types.iterator().next();
            if (ilt != null)
            {
                IssueLinkManager ilm = ComponentManager.getInstance().getIssueLinkManager();
                for (MutableIssue nissue : newIssues)
                {
                    try
                    {
                        ilm.createIssueLink(nissue.getId(), issue.getId(), ilt.getId(), null, getLoggedInUser());
                    }
                    catch (CreateException crex)
                    {
                        //--> nothing
                    }
                }
            }
        }

        return rc_str;
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
}
