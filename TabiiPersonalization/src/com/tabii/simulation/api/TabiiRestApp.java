package com.tabii.simulation.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.tabii.redisclient", "com.tabii.memcachedclient"})
public class TabiiRestApp {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TabiiRestApp.class);
        // This line fully disables JMX before context initialization
        app.setRegisterShutdownHook(false);
        System.setProperty("spring.jmx.enabled", "false");
        app.run(args);
    }
}