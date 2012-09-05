/*
 * Created by Andrey Markelov 06-09-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

/**
 * Issue Cloner Plug-In Configuration.
 * 
 * @author Andrey Markelov
 */
public class IssueClonerConfig
    extends JiraWebActionSupport
{
    /**
     * Unique ID.
     */
   private static final long serialVersionUID = -3579983449298580955L;

    /**
     * Application properties.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * Plug-In data manager.
     */
    private final CloneCompMgr cloneCompMgr;

    /**
     * Is saved?
     */
    private boolean isSaved = false;

    /**
     * Project manager.
     */
    private ProjectManager prMgr;

    /**
     * Saved groups.
     */
    private List<String> savedProjKeys;

    /**
     * Selected groups.
     */
    private String[] selectedProjKeys = new String[0];

    /**
     * Constructor.
     */
    public IssueClonerConfig(
        ApplicationProperties applicationProperties,
        CloneCompMgr cloneCompMgr,
        ProjectManager prMgr)
    {
        this.applicationProperties = applicationProperties;
        this.cloneCompMgr = cloneCompMgr;
        this.prMgr = prMgr;

        selectedProjKeys = cloneCompMgr.getProjectKeys();
        savedProjKeys = selectedProjKeys == null ? null : Arrays.asList(selectedProjKeys);
    }

    @Override
    protected String doExecute()
    throws Exception
    {
        cloneCompMgr.setProjectKeys(selectedProjKeys);
        if (selectedProjKeys != null)
        {
            savedProjKeys = Arrays.asList(cloneCompMgr.getProjectKeys());
        }
        setSaved(true);

        return getRedirect("MailRuIssueClonerConfig!default.jspa?saved=true");
    }

    public Map<String, String> getAllProjects()
    {
        Map<String, String> allProjs = new TreeMap<String, String>();

        List<Project> projs = prMgr.getProjectObjects();
        if (projs != null)
        {
            for (Project proj : projs)
            {
                allProjs.put(proj.getId().toString(), getProjView(proj.getName(), proj.getDescription()));
            }
        }

        return allProjs;
    }

    /**
     * Get context path.
     */
    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }

    private String getProjView(String name, String descr)
    {
        if (descr != null && !descr.isEmpty())
        {
            return (name + ": " + descr);
        }

        return name;
    }

    public List<String> getSavedProjKeys()
    {
        return savedProjKeys;
    }

    public String[] getSelectedProjKeys()
    {
        return selectedProjKeys;
    }

    public boolean hasAdminPermission()
    {
        User user = getLoggedInUser();
        if (user == null)
        {
            return false;
        }

        return getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser());
    }

    public boolean isSaved()
    {
        return isSaved;
    }

    public void setSaved(boolean isSaved)
    {
        this.isSaved = isSaved;
    }

    public void setSavedProjKeys(List<String> savedProjKeys)
    {
        this.savedProjKeys = savedProjKeys;
    }

    public void setSelectedProjKeys(String[] selectedProjKeys)
    {
        this.selectedProjKeys = selectedProjKeys;
    }
}
