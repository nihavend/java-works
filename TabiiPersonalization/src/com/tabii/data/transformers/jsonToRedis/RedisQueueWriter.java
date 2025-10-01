package com.tabii.data.transformers.jsonToRedis;
import redis.clients.jedis.Jedis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class RedisQueueWriter {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java RedisQueueWriter <json-file>");
            System.exit(1);
        }

        File jsonFile = new File(args[0]);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonFile);

        String id = root.get("id").asText();
        String key = "queue:" + id; // prefix to differentiate
        String value = mapper.writeValueAsString(root);

        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.set(key, value);
            System.out.println("✅ Stored JSON under key: " + key);
        }
    }
}
