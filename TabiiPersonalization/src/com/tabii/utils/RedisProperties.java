package com.tabii.utils;

public class RedisProperties {

	int port;
	String host;

	public RedisProperties(int port, String host) {
		super();
		this.port = port;
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
