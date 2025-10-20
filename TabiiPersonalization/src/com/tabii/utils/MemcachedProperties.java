package com.tabii.utils;

public class MemcachedProperties {

	private String servers;
	private int expireSeconds;

	public MemcachedProperties(String servers, int expireSeconds) {
		super();
		this.servers = servers;
		this.expireSeconds = expireSeconds;
	}

	public String getServers() {
		return servers;
	}

	public int getExpireSeconds() {
		return expireSeconds;
	}

}
