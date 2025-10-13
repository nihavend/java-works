package com.tabii.test.redis;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.Set;

public class RedisKeyValueFetcher {

    public static void main(String[] args) {
        // Define the list of base keys
        List<String> baseKeys = List.of("genre", "badges", "image");

        // Connect to Redis (default localhost:6379)
        try (Jedis jedis = new Jedis("localhost", 6379)) {

            for (String baseKey : baseKeys) {
                // Use pattern to find matching keys
                String pattern = baseKey + ":*";
                Set<String> matchingKeys = jedis.keys(pattern);

                System.out.println("== Keys matching pattern: " + pattern + " ==");

                // Retrieve and print values for each key
                for (String fullKey : matchingKeys) {
                    String value = jedis.get(fullKey); // Assumes values are simple Strings
                    System.out.println(fullKey + " -> " + value);
                    // break;
                }

                System.out.println();
            }

        } catch (Exception e) {
            System.err.println("Redis error: " + e.getMessage());
        }
    }
}
