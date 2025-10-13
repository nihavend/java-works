package com.tabii.utils;

public class RedisProperties {

	String url;

	int port;
	String host;

	public RedisProperties(String url) {
		super();
		this.url = url;
	}

	/**
	 * use RedisProperties(String url) instead
	 * e.g. Jedis jedis = new Jedis("redis://localhost:6379/14");
	 */
	@Deprecated  
	public RedisProperties(int port, String host) {
		super();
		this.port = port;
		this.host = host;
	}
	
	/**
	 * use getUrl
	 */
	@Deprecated
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * use getUrl
	 */
	@Deprecated
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
