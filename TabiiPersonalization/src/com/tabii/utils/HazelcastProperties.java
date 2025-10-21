package com.tabii.utils;

public class HazelcastProperties {

	private String servers;
	private String clusterName;

	public HazelcastProperties(String servers, String clusterName) {
		super();
		this.servers = servers;
		this.clusterName = clusterName;
	}

	public String getServers() {
		return servers;
	}

	public String getClusterName() {
		return clusterName;
	}

}
