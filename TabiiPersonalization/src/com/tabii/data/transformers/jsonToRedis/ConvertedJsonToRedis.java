package com.tabii.data.transformers.jsonToRedis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConvertedJsonToRedis {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java JsonToRedis <path-to-json-file>");
			System.exit(1);
		}

		String jsonFilePath = args[0];
		ObjectMapper mapper = new ObjectMapper();

		try (Jedis jedis = new Jedis("localhost", 6379)) { // Change host/port if needed
			JsonNode rootNode = mapper.readTree(new File(jsonFilePath));

			Iterator<Map.Entry<String, JsonNode>> rows = rootNode.fields();
			while (rows.hasNext()) {
				Map.Entry<String, JsonNode> rowEntry = rows.next();
				String rowKey = rowEntry.getKey(); // e.g., "row:149015"
				JsonNode rowNode = rowEntry.getValue();

				JsonNode showsNode = rowNode.get("shows");
				if (showsNode != null && showsNode.isObject()) {
					List<String> showIds = new ArrayList<>();

					Iterator<Map.Entry<String, JsonNode>> shows = showsNode.fields();
					while (shows.hasNext()) {
						Map.Entry<String, JsonNode> showEntry = shows.next();
						String showKey = showEntry.getKey(); // e.g., "show:191180"
						JsonNode showValue = showEntry.getValue();

						showIds.add(showKey.split(":")[1]); // Extract numeric ID
						jedis.set(showKey, mapper.writeValueAsString(showValue)); // Store show JSON
					}

					// Store row with list of show IDs
					JsonNode rowType = rowNode.get("rowType");
					if (rowType != null) {
						// Optional: include rowType if needed
						jedis.set(rowKey,
								mapper.writeValueAsString(Map.of("rowType", rowType.asText(), "shows", showIds)));
					} else {
						jedis.set(rowKey, mapper.writeValueAsString(Map.of("shows", showIds)));
					}

					System.out.println("Saved row " + rowKey + " with shows " + showIds);
				}
			}

			System.out.println("All rows and shows saved to Redis successfully.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
