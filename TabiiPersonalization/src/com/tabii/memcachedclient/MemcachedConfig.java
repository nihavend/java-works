package com.tabii.memcachedclient;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.MemcachedProperties;

import net.spy.memcached.MemcachedClient;

@Configuration
public class MemcachedConfig {


    @Bean
    public MemcachedClient memcachedClient() throws Exception {
    	
    	MemcachedProperties memcachedProperties = CommonUtils.getMemcachedConnectionProps();
    	
        List<InetSocketAddress> serverList = new ArrayList<>();
        for (String server : memcachedProperties.getServers().split(",")) {
            String[] parts = server.split(":");
            serverList.add(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])));
        }
        return new MemcachedClient(serverList);
    }
}
