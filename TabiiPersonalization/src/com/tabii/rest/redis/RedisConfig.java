package com.tabii.rest.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tabii.utils.CommonUtils;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
public class RedisConfig {

    // @Value("${redis.host}")
    // private String redisHost;

    // @Value("${redis.port}")
    // private int redisPort;

    @Bean
    public JedisPool jedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();
        
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(2);

		// poolConfig.setTestOnBorrow(true); // Bağlantı alınmadan önce test et
        // poolConfig.setTestWhileIdle(true);// Boşta test et
		// poolConfig.setBlockWhenExhausted(true); // Pool boşsa bekle
		// poolConfig.setMaxWait(Duration.ofMillis(2000)); // Bağlantı için maksimum bekleme süresi

        return new JedisPool(poolConfig, redisProperties.getUrl()); 
        
    }
}
