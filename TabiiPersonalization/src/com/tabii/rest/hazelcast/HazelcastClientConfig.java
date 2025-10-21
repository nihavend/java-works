package com.tabii.rest.hazelcast;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.HazelcastProperties;

@Configuration
public class HazelcastClientConfig {

    @Bean
    HazelcastInstance hazelcastClientInstance() {
    	
    	HazelcastProperties hazelcastProperties = CommonUtils.getHazelcastConnectionProps();
    	
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName(hazelcastProperties.getClusterName());

        ClientNetworkConfig networkConfig = clientConfig.getNetworkConfig();
        // networkConfig.addAddress("192.168.1.10:5701", "192.168.1.11:5702");
        // networkConfig.addAddress("127.0.0.1:5701", "127.0.0.1:5702");
        networkConfig.setAddresses(CommonUtils.serversSplitter(hazelcastProperties.getServers()));
        
        return HazelcastClient.newHazelcastClient(clientConfig);
    }
}
