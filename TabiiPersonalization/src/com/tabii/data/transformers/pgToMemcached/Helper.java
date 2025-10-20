package com.tabii.data.transformers.pgToMemcached;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Helper {
	public static List<InetSocketAddress> getServers(String serversStr) {
		List<InetSocketAddress> serverList = new ArrayList<>();
		for (String server : serversStr.split(",")) {
			String[] parts = server.split(":");
			serverList.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
		}

		return serverList;
	}

}