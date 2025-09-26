package com.tabii.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CommonUtils {
	
	public static Properties loadDbProperties() {
		Properties props = new Properties();
		try (InputStream input = new FileInputStream("db.properties")) {
			props.load(input);
		} catch (IOException e) {
			System.err.println("❌ Failed to load db.properties file");
			e.printStackTrace();
			System.exit(1);
		}
		return props;
	}

}
