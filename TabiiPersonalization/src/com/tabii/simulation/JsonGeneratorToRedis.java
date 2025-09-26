package com.tabii.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import redis.clients.jedis.Jedis;

public class JsonGeneratorToRedis {

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Random random = new Random();

	// keep all row IDs across wrappers for queue maps
	private static final List<Integer> allRowIds = new ArrayList<>();

	public static void main(String[] args) throws Exception {
		int wrapperCount = 5; // how many wrapper JSONs
		int queueCount = 3; // how many queue maps

		try (Jedis jedis = new Jedis("localhost", 6379)) {
			// --- Generate wrappers + rows ---
			for (int i = 0; i < wrapperCount; i++) {
				ObjectNode wrapper = generateRandomWrapper();
				String wrapperKey = "json:" + UUID.randomUUID();
				String wrapperJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(wrapper);

				jedis.set(wrapperKey, wrapperJson);
				System.out.println("Stored wrapper -> " + wrapperKey);

				// store rows from wrapper
				ArrayNode contents = (ArrayNode) wrapper.withArray("data").get(0).get("contents");
				for (int j = 0; j < contents.size(); j++) {
					ObjectNode row = (ObjectNode) contents.get(j);
					int rowId = row.get("id").asInt();
					allRowIds.add(rowId);

					String rowKey = "row:" + rowId;
					String rowJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(row);

					jedis.set(rowKey, rowJson);
					System.out.println("   -> Stored row: " + rowKey);
				}
			}

			// --- Generate queue maps ---
			for (int i = 0; i < queueCount; i++) {
				ObjectNode queueMap = generateQueueMap();
				int queueId = queueMap.get("id").asInt();

				String queueKey = "queue:" + queueId;
				jedis.set(queueKey, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(queueMap));
				System.out.println("Stored queue -> " + queueKey);
			}
		}
	}

	private static ObjectNode generateRandomWrapper() {
		ObjectNode root = mapper.createObjectNode();
		ArrayNode dataArray = mapper.createArrayNode();
		root.set("data", dataArray);

		ObjectNode dataObj = mapper.createObjectNode();
		dataArray.add(dataObj);

		ArrayNode contentsArray = mapper.createArrayNode();
		dataObj.set("contents", contentsArray);

		// random number of rows between 1 and 5
		int rows = 1 + random.nextInt(5);
		for (int i = 0; i < rows; i++) {
			contentsArray.add(generateRandomRow());
		}

		return root;
	}

	private static ObjectNode generateRandomRow() {
		ObjectNode row = mapper.createObjectNode();

		String rowType = randomRowType();
		int id = randomId();

		// mandatory
		row.put("id", id);
		row.put("rowType", rowType);

		// type-specific
		switch (rowType) {
		case "banner":
			row.put("title", "Banner Row");
			break;

		case "show":
			row.put("title", randomTitle());
			ObjectNode metaDataShow = mapper.createObjectNode();
			metaDataShow.put("viewType", "Portrait Medium");
			row.set("metaData", metaDataShow);
			break;

		case "continueWatching":
			row.put("title", "Continue Watching");
			break;

		case "livestream":
			row.put("title", "Live TV");
			row.put("description", "All TRT Channels");
			row.put("targetId", String.valueOf(randomId()));
			break;

		case "special":
			row.put("title", "Kids' Table");
			row.put("targetId", String.valueOf(randomId()));
			break;

		case "genre":
			row.put("title", "Genres");
			row.set("metaData", mapper.createObjectNode());
			break;
		}

		// contents array inside row
		ArrayNode contents = mapper.createArrayNode();
		int contentCount = 1 + random.nextInt(5);
		for (int i = 0; i < contentCount; i++) {
			ObjectNode content = mapper.createObjectNode();
			content.put("contentId", UUID.randomUUID().toString());
			content.put("id", randomId());
			contents.add(content);
		}
		row.set("contents", contents);

		return row;
	}

	private static ObjectNode generateQueueMap() {
		ObjectNode queueMap = mapper.createObjectNode();
		int queueId = randomId();
		queueMap.put("id", queueId);

		ArrayNode rowsArray = mapper.createArrayNode();
		queueMap.set("rows", rowsArray);

		// between 10–15 unique row IDs
		int count = 10 + random.nextInt(6);

		List<Integer> shuffled = new ArrayList<>(allRowIds);
		Collections.shuffle(shuffled);

		for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
			rowsArray.add(shuffled.get(i));
		}

		return queueMap;
	}

	private static int randomId() {
		return 100000 + random.nextInt(900000);
	}

	private static String randomRowType() {
		String[] rowTypes = { "banner", "show", "continueWatching", "livestream", "special", "genre" };
		return rowTypes[random.nextInt(rowTypes.length)];
	}

	private static String randomTitle() {
		String[] titles = { "Weekly tabii 10", "Only on tabii!", "Top Picks", "Trending Now", "Recommended" };
		return titles[random.nextInt(titles.length)];
	}
}
