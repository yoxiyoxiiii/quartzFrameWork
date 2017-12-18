package com.kerui.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtils {

    public static Properties getProperties(String path) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = new FileInputStream(new File(path));
        properties.load(inputStream);
        return properties;
    }
}
