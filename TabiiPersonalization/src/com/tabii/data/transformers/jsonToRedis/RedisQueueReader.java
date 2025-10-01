package com.tabii.data.transformers.jsonToRedis;
import redis.clients.jedis.Jedis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RedisQueueReader {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java RedisQueueReader <queue-id>");
            System.exit(1);
        }

        String id = args[0];
        String key = "queue:" + id;

        ObjectMapper mapper = new ObjectMapper();

        try (Jedis jedis = new Jedis("localhost", 6379)) {
            String storedJson = jedis.get(key);

            if (storedJson == null) {
                System.out.println("⚠️ Key not found: " + key);
                return;
            }

            JsonNode parsed = mapper.readTree(storedJson);
            System.out.println("📌 Retrieved JSON for key: " + key);
            System.out.println(parsed.toPrettyString());
        }
    }
}
