package com.tabii;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication(scanBasePackages = {"com.tabii.rest.redis", "com.tabii.rest.memcached", "com.tabii.rest.hazelcast", "com.tabii.rest.dynamodb})
// @SpringBootApplication(scanBasePackages = {"com.tabii.rest.memcached"})
@SpringBootApplication(scanBasePackages = {"com.tabii.rest.dynamodb"})
public class TabiiRestApp {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TabiiRestApp.class);
        // This line fully disables JMX before context initialization
        app.setRegisterShutdownHook(false);
        System.setProperty("spring.jmx.enabled", "false");
        app.run(args);
    }
}