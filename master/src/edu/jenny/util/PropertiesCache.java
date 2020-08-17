package edu.utas.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesCache {
    private final Properties configProp = new Properties();

    private PropertiesCache()
    {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("config.properties");
        try {
            configProp.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class LazyHolder
    {
        private static final PropertiesCache INSTANCE = new PropertiesCache();
    }

    public static PropertiesCache getInstance()
    {
        return LazyHolder.INSTANCE;
    }

    public String getProperty(String key){
        return configProp.getProperty(key);
    }
}
