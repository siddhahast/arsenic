package com.util;

import java.util.Map;
import java.util.Properties;

public class BasicSystemPropetiesLoader implements SystemPropertiesLoader
{

    private static final String SYSTEM_PROPERTIES_FILE = "system.properties.file";
    private static final String SYSTEM_PROPERTIES_ENV = "env";

    private String[] systemFiles;

    public BasicSystemPropetiesLoader()
    {
        this.systemFiles = System.getProperty(SYSTEM_PROPERTIES_FILE, SYSTEM_PROPERTIES_ENV).split("//s*");
        loadSystemProperties();
    }


    public void loadSystemProperties()
    {
        for (String file : systemFiles)
        {
            Properties systemProps = ResourceFactory.BASIC.getResource(file, Properties.class);

            if(systemProps!=null)
            {
                for(Map.Entry<Object, Object> entry: systemProps.entrySet())
                {
                    if(System.getProperty(entry.getKey().toString())==null)
                    {
                        System.setProperty(entry.getKey().toString(), entry.getValue().toString());
                    }
                    else
                    {
                        // already set the system properties
                    }
                }
            }
        }
    }
}
