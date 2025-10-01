package com.tabii.data.transformers.jsonToRedis;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class BadgesToRedis {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java BadgesToRedis <path-to-json-file>");
            System.exit(1);
        }

        String filePath = args[0];
        ObjectMapper mapper = new ObjectMapper();

        try (Jedis jedis = new Jedis("localhost", 6379)) { // adjust host/port if needed
            JsonNode root = mapper.readTree(new File(filePath));
            storeBadgesNodes(root, mapper, jedis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void storeBadgesNodes(JsonNode node, ObjectMapper mapper, Jedis jedis) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if ("badges".equalsIgnoreCase(key)) {
                    if (value.isArray()) {
                        for (JsonNode badgeNode : value) {
                            processAndStoreBadge(badgeNode, mapper, jedis);
                        }
                    } else if (value.isObject()) {
                        processAndStoreBadge(value, mapper, jedis);
                    }
                }

                // recursively search in child nodes
                storeBadgesNodes(value, mapper, jedis);
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                storeBadgesNodes(item, mapper, jedis);
            }
        }
    }

    private static void processAndStoreBadge(JsonNode badgeNode, ObjectMapper mapper, Jedis jedis) {
        if (!badgeNode.has("id")) {
            System.out.println("Skipping badge node without id: " + badgeNode);
            return;
        }
    	String badgeId = badgeNode.get("id").asText();
    	if (doesExist("badge:" + badgeId)) {
    		System.out.println("Skipping badge node exists id: " + badgeId);
    		return;
    	}
        
        ObjectNode badgeCopy = badgeNode.deepCopy();

        // Replace images array with image IDs
        if (badgeCopy.has("images") && badgeCopy.get("images").isArray()) {
            ArrayNode imagesArray = (ArrayNode) badgeCopy.get("images");
            ArrayNode idArray = mapper.createArrayNode();
            for (JsonNode imageNode : imagesArray) {
                if (imageNode.has("id")) {
                    idArray.add(imageNode.get("id").asText());
                }
            }
            badgeCopy.set("images", idArray);
        }

        try {
            badgeId = badgeCopy.get("id").asText();
            String jsonValue = mapper.writeValueAsString(badgeCopy);
            jedis.set("badge:" + badgeId, jsonValue);
            System.out.println("Stored badge with key: badge:" + badgeId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static boolean doesExist(String key) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            String storedJson = jedis.get(key);
            if (storedJson != null) {
                return true;
            }
        }
        return false;
    }
}
