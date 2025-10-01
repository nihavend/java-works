package com.tabii.data.transformers.jsonToRedis;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import redis.clients.jedis.Jedis;

public class GenerateJsonFromRedis {

	private static Jedis jedis;
	private static ObjectMapper objectMapper = new ObjectMapper();

	public static void main(String[] args) throws IOException {
		try (Scanner scanner = new Scanner(System.in)) {
			System.out.print("Enter queue id: ");
			String queueId = scanner.nextLine();

			// Connect to Redis
			jedis = new Jedis("localhost", 6379);

			// Read queue data from Redis
			String queueKey = "queue:" + queueId;
			String queueJson = jedis.get(queueKey);

			if (queueJson == null) {
				System.out.println("Queue not found in Redis: " + queueKey);
				return;
			}

			JsonNode queueNode = objectMapper.readTree(queueJson);

			// Resolve rows recursively
			JsonNode resolvedQueue = resolveQueue(queueNode);

			// Write output to file
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("resolved_queue_" + queueId + ".json"),
					resolvedQueue);

			
			System.out.println("Resolved queue JSON written to file: resolved_queue_" + queueId + ".json");
		}
	}

	private static JsonNode resolveQueue(JsonNode queueNode) throws JsonMappingException, JsonProcessingException {
		if (queueNode.isObject()) {
			ObjectNode objNode = (ObjectNode) queueNode;
			Iterator<String> fieldNames = objNode.fieldNames();
			while (fieldNames.hasNext()) {
				String field = fieldNames.next();
				JsonNode child = objNode.get(field);
				if (field.equalsIgnoreCase("rows") && child.isArray()) {
					ArrayNode newRows = objectMapper.createArrayNode();
					for (JsonNode rowIdNode : child) {
						String rowId = rowIdNode.asText();
						String rowJson = jedis.get("row:" + rowId);
						if (rowJson != null) {
							JsonNode rowNode = objectMapper.readTree(rowJson);
							newRows.add(resolveRow(rowNode));
						} else {
							System.out.println("Row not found: " + rowId);
						}
					}
					objNode.set("rows", newRows);
				} else {
					objNode.set(field, resolveQueue(child));
				}
			}
			return objNode;
		} else if (queueNode.isArray()) {
			ArrayNode arrayNode = (ArrayNode) queueNode;
			ArrayNode newArray = objectMapper.createArrayNode();
			for (JsonNode item : arrayNode) {
				newArray.add(resolveQueue(item));
			}
			return newArray;
		}
		return queueNode;
	}

	private static JsonNode resolveRow(JsonNode rowNode) {
		if (rowNode.isObject() && rowNode.has("shows")) {
			ObjectNode showsNode = (ObjectNode) rowNode.get("shows");
			Iterator<String> showIds = showsNode.fieldNames();
			while (showIds.hasNext()) {
				String showId = showIds.next();
				String showJson = jedis.get("show:" + showId);
				if (showJson != null) {
					try {
						JsonNode showNode = objectMapper.readTree(showJson);
						showsNode.set(showId, resolveShow(showNode));
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					System.out.println("Show not found: " + showId);
				}
			}
		}
		return rowNode;
	}

	private static JsonNode resolveShow(JsonNode showNode) {
		if (showNode.isObject()) {
			ObjectNode objNode = (ObjectNode) showNode;
			Iterator<String> fields = objNode.fieldNames();
			while (fields.hasNext()) {
				String field = fields.next();
				JsonNode child = objNode.get(field);
				if (child.isArray()) {
					ArrayNode arrayNode = (ArrayNode) child;
					ArrayNode resolvedArray = objectMapper.createArrayNode();
					for (JsonNode item : arrayNode) {
						if (item.isTextual()) { // maybe an ID pointing to another Redis object
							String itemJson = jedis.get(field + ":" + item.asText());
							if (itemJson != null) {
								try {
									resolvedArray.add(resolveShow(objectMapper.readTree(itemJson)));
								} catch (IOException e) {
									e.printStackTrace();
								}
							} else {
								resolvedArray.add(item);
							}
						} else {
							resolvedArray.add(resolveShow(item));
						}
					}
					objNode.set(field, resolvedArray);
				} else if (child.isObject()) {
					objNode.set(field, resolveShow(child));
				}
			}
		}
		return showNode;
	}
}
