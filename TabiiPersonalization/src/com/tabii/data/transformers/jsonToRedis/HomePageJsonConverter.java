package com.tabii.data.transformers.jsonToRedis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class HomePageJsonConverter {

	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Usage: java JsonConverter <input-json-file>");
			return;
		}

		// Input file
		File inputFile = new File(args[0]);
		if (!inputFile.exists()) {
			System.out.println("File not found: " + args[0]);
			return;
		}

		// Output file: same folder with "_converted" appended
		String inputName = inputFile.getName();
		int dotIndex = inputName.lastIndexOf(".");
		String baseName = (dotIndex > 0) ? inputName.substring(0, dotIndex) : inputName;
		String extension = (dotIndex > 0) ? inputName.substring(dotIndex) : ".json";
		File outputFile = new File(inputFile.getParent(), baseName + "_converted" + extension);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(inputFile);

		ObjectNode result = mapper.createObjectNode();

		// Traverse "data" array
		if (root.has("data") && root.get("data").isArray()) {
			ArrayNode dataArray = (ArrayNode) root.get("data");

			for (JsonNode row : dataArray) {
				if (!row.has("id"))
					continue;
				String rowId = "row:" + row.get("id").asText();
				ObjectNode rowNode = mapper.createObjectNode();

				Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
				while (fields.hasNext()) {
					Map.Entry<String, JsonNode> entry = fields.next();
					String key = entry.getKey();
					JsonNode value = entry.getValue();

					if (key.equals("id"))
						continue; // used as row key

					// Special case: contents array → shows
					if (key.equals("contents") && value.isArray()) {
						ObjectNode showsNode = mapper.createObjectNode();
						for (JsonNode show : value) {
							if (show.has("id")) {
								String showId = "show:" + show.get("id").asText();
								showsNode.set(showId, transformRow(show, mapper));
							}
						}
						rowNode.set("shows", showsNode);
					}
					// Convert arrays of objects to arrays of IDs
					else if (value.isArray() && value.size() > 0 && value.get(0).isObject()) {
						ArrayNode idArray = mapper.createArrayNode();
						value.forEach(item -> {
							if (item.has("badgeid"))
								idArray.add(item.get("badgeid").asText());
							else if (item.has("id"))
								idArray.add(item.get("id").asText());
						});
						rowNode.set(key, idArray);
					} else {
						rowNode.set(key, value);
					}
				}

				result.set(rowId, rowNode);
			}
		}

		// Write converted JSON
		mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, result);
		System.out.println("Converted JSON written to: " + outputFile.getAbsolutePath());
	}

	// Helper to transform a single row or show
	private static ObjectNode transformRow(JsonNode row, ObjectMapper mapper) {
		ObjectNode rowNode = mapper.createObjectNode();
		Iterator<Map.Entry<String, JsonNode>> fields = row.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			String key = entry.getKey();
			JsonNode value = entry.getValue();

			if (key.equals("id"))
				continue; // already used as key

			if (value.isArray() && value.size() > 0 && value.get(0).isObject()) {
				ArrayNode idArray = mapper.createArrayNode();
				value.forEach(item -> {
					if (item.has("badgeid"))
						idArray.add(item.get("badgeid").asText());
					else if (item.has("id"))
						idArray.add(item.get("id").asText());
				});
				rowNode.set(key, idArray);
			} else {
				rowNode.set(key, value);
			}
		}
		return rowNode;
	}
}
