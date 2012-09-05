/*
 * Created by Andrey Markelov 06-09-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

/**
 * Plug-In data manager.
 * 
 * @author Andrey Markelov
 */
public interface CloneCompMgr
{
    /**
     * Get a collection of project keys.
     */
    String[] getProjectKeys();

    /**
     * Set a collection of project keys.
     */
    void setProjectKeys(String[] projectKeys);
}
