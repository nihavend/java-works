package com.tabii.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisRestTestApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(RedisRestTestApplication.class);
        // This line fully disables JMX before context initialization
        app.setRegisterShutdownHook(false);
        System.setProperty("spring.jmx.enabled", "false");
        app.run(args);
    }
}
