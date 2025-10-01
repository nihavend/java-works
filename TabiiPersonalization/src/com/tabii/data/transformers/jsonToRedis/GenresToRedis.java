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

public class GenresToRedis {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java GenresToRedis <path-to-json-file>");
			System.exit(1);
		}

		String filePath = args[0];
		ObjectMapper mapper = new ObjectMapper();

		try (Jedis jedis = new Jedis("localhost", 6379)) { // adjust host/port if needed
			JsonNode root = mapper.readTree(new File(filePath));
			storeGenresNodes(root, mapper, jedis);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void storeGenresNodes(JsonNode node, ObjectMapper mapper, Jedis jedis) {
		if (node.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				String key = entry.getKey();
				JsonNode value = entry.getValue();

				if ("genres".equalsIgnoreCase(key)) {
					if (value.isArray()) {
						for (JsonNode genreNode : value) {
							processAndStoreGenre(genreNode, mapper, jedis);
						}
					} else if (value.isObject()) {
						processAndStoreGenre(value, mapper, jedis);
					}
				}

				// recursively search in child nodes
				storeGenresNodes(value, mapper, jedis);
			}
		} else if (node.isArray()) {
			for (JsonNode item : node) {
				storeGenresNodes(item, mapper, jedis);
			}
		}
	}

	private static void processAndStoreGenre(JsonNode genreNode, ObjectMapper mapper, Jedis jedis) {
		if (!genreNode.has("id")) {
			System.out.println("Skipping genre node without id: " + genreNode);
			return;
		}

		ObjectNode genreCopy = genreNode.deepCopy();

		// Replace images array with image IDs
		if (genreCopy.has("images") && genreCopy.get("images").isArray()) {
			ArrayNode imagesArray = (ArrayNode) genreCopy.get("images");
			ArrayNode idArray = mapper.createArrayNode();
			for (JsonNode imageNode : imagesArray) {
				if (imageNode.has("id")) {
					idArray.add(imageNode.get("id").asText());
				}
			}
			genreCopy.set("images", idArray);
		}

		try {
			String genreId = genreCopy.get("id").asText();
			String jsonValue = mapper.writeValueAsString(genreCopy);
			jedis.set("genre:" + genreId, jsonValue);
			System.out.println("Stored genre with key: genre:" + genreId);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
