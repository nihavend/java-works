package com.tabii.rest.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastClientConfig {

    @Bean
    HazelcastInstance hazelcastClientInstance() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName("dev");

        ClientNetworkConfig networkConfig = clientConfig.getNetworkConfig();
        // networkConfig.addAddress("192.168.1.10:5701", "192.168.1.11:5702");
        networkConfig.addAddress("127.0.0.1:5701", "127.0.0.1:5702");

        return HazelcastClient.newHazelcastClient(clientConfig);
    }
}
