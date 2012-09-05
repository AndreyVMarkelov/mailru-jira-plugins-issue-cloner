/*
 * Created by Andrey Markelov 06-09-2012.
 * Copyright Mail.Ru Group 2012. All rights reserved.
 */
package ru.mail.jira.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Implementation of <code>CloneCompMgr</code>.
 * 
 * @author Andrey Markelov
 */
public class CloneCompMgrImpl
    implements CloneCompMgr
{
    /**
     * Plug-In data name.
     */
    private static final String PLUGIN_KEY = "MAIL_RU_CLONE_COMPS_PLUGIN";

    /**
     * Plu-In data key.
     */
    private static final String PROJ_KEYS = "PROJ_KEYS";

    /**
     * Value separator.
     */
    private static final String VAL_SEPARATOR = "&";

    /**
     * Plug-In settings factory.
     */
    private final PluginSettingsFactory pluginSettingsFactory;

    /**
     * Constructor.
     */
    public CloneCompMgrImpl(
        PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public String[] getProjectKeys()
    {
        List<String> projectKeys = new ArrayList<String>();

        String val = getStringProperty(PROJ_KEYS);
        if (val != null && val.length() > 0)
        {
            StringTokenizer st = new StringTokenizer(val, VAL_SEPARATOR);
            while(st.hasMoreTokens())
            {
                projectKeys.add(st.nextToken());
            }
        }

        return projectKeys.toArray(new String[projectKeys.size()]);
    }

    private String getStringProperty(String key)
    {
        return (String) pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(key);
    }

    @Override
    public void setProjectKeys(String[] projectKeys)
    {
        StringBuilder sb = new StringBuilder();
        if (projectKeys != null)
        {
            for (String projectKey : projectKeys)
            {
                sb.append(projectKey).append(VAL_SEPARATOR);
            }
        }

        setStringProperty(PROJ_KEYS, sb.toString());
    }

    private void setStringProperty(String key, String value)
    {
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(key, value);
    }
}
