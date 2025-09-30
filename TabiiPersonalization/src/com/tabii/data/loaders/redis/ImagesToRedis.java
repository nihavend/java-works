package com.tabii.data.loaders.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class ImagesToRedis {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java ImagesToRedis <path-to-json-file>");
			System.exit(1);
		}

		String filePath = args[0];
		ObjectMapper mapper = new ObjectMapper();

		try (Jedis jedis = new Jedis("localhost", 6379)) { // adjust host/port if needed
			JsonNode root = mapper.readTree(new File(filePath));
			storeImagesNodes(root, mapper, jedis);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void storeImagesNodes(JsonNode node, ObjectMapper mapper, Jedis jedis) {
		if (node.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				String key = entry.getKey();
				JsonNode value = entry.getValue();

				if ("images".equalsIgnoreCase(key) && value.isArray()) {
					for (JsonNode imageNode : value) {
						if (imageNode.has("id")) {
							String imageId = imageNode.get("id").asText();
							try {
								// store the full image object as value
								String jsonValue = mapper.writeValueAsString(imageNode);
								jedis.set("image:" + imageId, jsonValue);
								System.out.println("Stored image with key: image:" + imageId);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							System.out.println("Skipping image node without id: " + imageNode);
						}
					}
				}

				// recursively search in child nodes
				storeImagesNodes(value, mapper, jedis);
			}
		} else if (node.isArray()) {
			for (JsonNode item : node) {
				storeImagesNodes(item, mapper, jedis);
			}
		}
	}
}
