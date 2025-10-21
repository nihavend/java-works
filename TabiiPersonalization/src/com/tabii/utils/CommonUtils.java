package com.tabii.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class CommonUtils {
	
	public static final List<String> hcMaps = Arrays.asList("imagesMap", "exclusiveBadgesMap", "badgesMap", "genreMap", "showsMap");
	
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

	public static MemcachedProperties getMemcachedConnectionProps() {
		
	    Properties dbProps = CommonUtils.loadDbProperties();
	    
		String url = dbProps.getProperty("memcached.servers");
		int expireSeconds = Integer.parseInt(dbProps.getProperty("memcached.expire.seconds"));
		
	    return new MemcachedProperties(url, expireSeconds);
	}
	
	public static HazelcastProperties getHazelcastConnectionProps() {
		
	    Properties dbProps = CommonUtils.loadDbProperties();
	    
		String url = dbProps.getProperty("hazelcast.servers");
		String clusterName = dbProps.getProperty("cluster.name");
		
	    return new HazelcastProperties(url, clusterName);
	}
	
	public static List<InetSocketAddress> getServers(String serversStr) {
		List<InetSocketAddress> serverList = new ArrayList<>();
		for (String server : serversStr.split(",")) {
			String[] parts = server.split(":");
			serverList.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
		}

		return serverList;
	}
	
	public static List<String> serversSplitter(String servers) {
		List<String> addresses = List.of(servers.split(","));
		
		return addresses;
	}
	
	public static String mapLocation(String key) {
		switch (key) {
		case "upper_right_corner":
			return "rightTop";
		case "upper_left_corner":
			return "leftTop";
		case "lower_left_corner":
			return "leftBottom";
		case "on_top_of_the_logo":
			return "upLogo";
		case "under_the_logo":
			return "bottomLogo";
		case "do_not_show":
		default:
			return "invisible";
		}
	}
}
