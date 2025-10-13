package com.tabii.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CommonUtils {
	
	private static Properties loadDbProperties() {
		
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
	
	public static PgProperties getPgConnectionProps() {
		
	    Properties dbProps = CommonUtils.loadDbProperties();
	    
	    String dbUrl = dbProps.getProperty("pg.db.url");
	    String dbUser = dbProps.getProperty("pg.db.user");
	    String dbPassword = dbProps.getProperty("pg.db.password");
	    String datafilepath = dbProps.getProperty("pg.db.datafilepath");
	    
	    if(datafilepath != null) {
	    	return new PgProperties(dbUrl, dbUser, dbPassword, datafilepath);
	    }
	    
	    return new PgProperties(dbUrl, dbUser, dbPassword);
	}
	
	
	public static MongoProperties getMongoConnectionProps() {
		
	    Properties dbProps = CommonUtils.loadDbProperties();
	    
	    String pgUser = dbProps.getProperty("mongo.db.url");
		String pgPass = dbProps.getProperty("mongo.db.name");
	    
	    return new MongoProperties(pgUser, pgPass);
	}
	
	public static RedisProperties getRedisConnectionProps() {
		
	    Properties dbProps = CommonUtils.loadDbProperties();
	    
		String url = dbProps.getProperty("redis.url");
	    
	    return new RedisProperties(url);
	}
	
}
