package com.tabii.rest.redis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.RedisProperties;

import redis.clients.jedis.Jedis;

public class RedisQueueToJson {

	public static void main(String[] args) {

		RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();

		try (Jedis jedis = new Jedis(redisProperties.getUrl())) {

			// Create ObjectMapper for JSON handling
			ObjectMapper mapper = new ObjectMapper();

			// Get queue data (e.g., queue:1)
			String queueKey = "queue:1";
			String queueJson = jedis.get(queueKey);
			if (queueJson == null) {
				System.out.println("Queue not found: " + queueKey);
				return;
			}

			ObjectNode resultJson = parseQueueJson(jedis, mapper, queueJson);

			// Write to file
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File("queue_1.json"), resultJson);
			System.out.println("JSON written to queue_1.json");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ObjectNode parseQueueJson(Jedis jedis, ObjectMapper mapper, String queueJson)
			throws JsonMappingException, JsonProcessingException {

		ObjectNode resultJson = mapper.createObjectNode();

		// Parse queue JSON to get row IDs
		ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
		ArrayNode rowIds = (ArrayNode) queueNode.get("rows");

		ArrayNode dataArray = mapper.createArrayNode();

		// Process each row
		for (int i = 0; i < rowIds.size(); i++) {

			String rowId = rowIds.get(i).asText();
			String rowKey = "row:" + rowId;
			String rowJson = jedis.get(rowKey);

			if (rowJson == null)
				continue;

			ObjectNode rowNode = (ObjectNode) mapper.readTree(rowJson);
			ObjectNode rowData = mapper.createObjectNode();

			rowData.put("id", Integer.parseInt(rowId));
			rowData.put("rowType", rowNode.get("rowType").asText());

			// Get shows for the row
			ArrayNode showsArray = (ArrayNode) rowNode.get("shows");
			ArrayNode contentsArray = mapper.createArrayNode();

			// Process each show
			for (int j = 0; j < showsArray.size(); j++) {
				String showId = showsArray.get(j).asText();
				String showKey = "show:" + showId;
				String showJson = jedis.get(showKey);
				if (showJson == null)
					continue;

				ObjectNode showNode = (ObjectNode) mapper.readTree(showJson);
				ObjectNode showData = mapper.createObjectNode();
				showData.put("id", Integer.parseInt(showId)); // Ensure show ID is a number
				showData.put("contentType", showNode.get("contentType").asText());
				showData.put("description", showNode.get("description").asText());
				showData.put("favorite", showNode.get("favorite").asBoolean());
				showData.put("madeYear", showNode.get("madeYear").asInt());
				showData.put("spot", showNode.get("spot").asText());
				showData.put("title", showNode.get("title").asText());

				// Process images
				ArrayNode imagesArray = (ArrayNode) showNode.get("images");
				ArrayNode imagesData = mapper.createArrayNode();
				for (int k = 0; k < imagesArray.size(); k++) {
					String imageId = imagesArray.get(k).asText();
					String imageKey = "image:" + imageId;
					String imageJson = jedis.get(imageKey);
					if (imageJson != null) {
						ObjectNode imageNode = (ObjectNode) mapper.readTree(imageJson);
						imagesData.add(imageNode);
					}
				}
				showData.set("images", imagesData);

				// Process badges
				ArrayNode badgesArray = (ArrayNode) showNode.get("badges");
				ArrayNode badgesData = mapper.createArrayNode();
				for (int k = 0; k < badgesArray.size(); k++) {
					String badgeId = badgesArray.get(k).asText();
					String badgeKey = "badges:" + badgeId;
					String badgeJson = jedis.get(badgeKey);
					if (badgeJson != null) {
						ObjectNode badgeNode = (ObjectNode) mapper.readTree(badgeJson);
						// Process badge images
						ArrayNode badgeImagesArray = badgeNode.has("images") ? (ArrayNode) badgeNode.get("images")
								: mapper.createArrayNode();
						ArrayNode badgeImagesData = mapper.createArrayNode();
						for (int m = 0; m < badgeImagesArray.size(); m++) {
							String badgeImageId = badgeImagesArray.get(m).asText();
							String badgeImageKey = "image:" + badgeImageId;
							String badgeImageJson = jedis.get(badgeImageKey);
							if (badgeImageJson != null) {
								ObjectNode badgeImageNode = (ObjectNode) mapper.readTree(badgeImageJson);
								badgeImagesData.add(badgeImageNode);
							}
						}
						badgeNode.set("images", badgeImagesData);
						badgesData.add(badgeNode); // Directly add badge object
					}
				}
				showData.set("badges", badgesData);

				// Process genres
				ArrayNode genresArray = (ArrayNode) showNode.get("genres");
				ArrayNode genresData = mapper.createArrayNode();
				for (int k = 0; k < genresArray.size(); k++) {
					String genreId = genresArray.get(k).asText();
					String genreKey = "genre:" + genreId;
					String genreJson = jedis.get(genreKey);
					if (genreJson != null) {
						ObjectNode genreNode = (ObjectNode) mapper.readTree(genreJson);
						// Process badge images
						ArrayNode genreImagesArray = genreNode.has("images") ? (ArrayNode) genreNode.get("images")
								: mapper.createArrayNode();
						ArrayNode genreImagesData = mapper.createArrayNode();
						for (int m = 0; m < genreImagesArray.size(); m++) {
							String genreImageId = genreImagesArray.get(m).asText();
							String genreImageKey = "image:" + genreImageId;
							String genreImageJson = jedis.get(genreImageKey);
							if (genreImageJson != null) {
								ObjectNode badgeImageNode = (ObjectNode) mapper.readTree(genreImageJson);
								genreImagesData.add(badgeImageNode);
							}
						}
						genreNode.set("images", genreImagesData);
						genresData.add(genreNode); // Directly add badge object
					}
				}
				showData.set("genres", genresData);

				// Process exclusiveBadges if present
				if (showNode.has("exclusiveBadges")) {
					ArrayNode exclusiveBadgesArray = (ArrayNode) showNode.get("exclusiveBadges");
					showData.set("exclusiveBadges", exclusiveBadgesArray);
				}

				contentsArray.add(showData);
			}
			rowData.set("contents", contentsArray);
			dataArray.add(rowData);
		}

		resultJson.set("data", dataArray);

		return resultJson;

	}

	// For redis bad performance
	public static ObjectNode parseQueueJson1(RedisService redisService, ObjectMapper mapper, String queueJson)
			throws JsonMappingException, JsonProcessingException {

		ObjectNode resultJson = mapper.createObjectNode();

		// Parse queue JSON to get row IDs
		ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
		ArrayNode rowIds = (ArrayNode) queueNode.get("rows");

		ArrayNode dataArray = mapper.createArrayNode();

		// Process each row
		for (int i = 0; i < rowIds.size(); i++) {

			String rowId = rowIds.get(i).asText();
			String rowKey = "row:" + rowId;
			String rowJson = redisService.getValue(rowKey);

			if (rowJson == null)
				continue;

			ObjectNode rowNode = (ObjectNode) mapper.readTree(rowJson);
			ObjectNode rowData = mapper.createObjectNode();

			rowData.put("id", Integer.parseInt(rowId));
			rowData.put("rowType", rowNode.get("rowType").asText());

			// Get shows for the row
			ArrayNode showsArray = (ArrayNode) rowNode.get("shows");
			ArrayNode contentsArray = mapper.createArrayNode();

			// Process each show
			for (int j = 0; j < showsArray.size(); j++) {
				String showId = showsArray.get(j).asText();
				String showKey = "show:" + showId;
				String showJson = redisService.getValue(showKey);
				if (showJson == null)
					continue;

				ObjectNode showNode = (ObjectNode) mapper.readTree(showJson);
				ObjectNode showData = mapper.createObjectNode();
				showData.put("id", Integer.parseInt(showId)); // Ensure show ID is a number
				showData.put("contentType", showNode.get("contentType").asText());
				showData.put("description", showNode.get("description").asText());
				showData.put("favorite", showNode.get("favorite").asBoolean());
				showData.put("madeYear", showNode.get("madeYear").asInt());
				showData.put("spot", showNode.get("spot").asText());
				showData.put("title", showNode.get("title").asText());

				// Process images
				ArrayNode imagesArray = (ArrayNode) showNode.get("images");
				ArrayNode imagesData = mapper.createArrayNode();
				for (int k = 0; k < imagesArray.size(); k++) {
					String imageId = imagesArray.get(k).asText();
					String imageKey = "image:" + imageId;
					String imageJson = redisService.getValue(imageKey);
					if (imageJson != null) {
						ObjectNode imageNode = (ObjectNode) mapper.readTree(imageJson);
						imagesData.add(imageNode);
					}
				}
				showData.set("images", imagesData);

				// Process badges
				ArrayNode badgesArray = (ArrayNode) showNode.get("badges");
				ArrayNode badgesData = mapper.createArrayNode();
				for (int k = 0; k < badgesArray.size(); k++) {
					String badgeId = badgesArray.get(k).asText();
					String badgeKey = "badges:" + badgeId;
					String badgeJson = redisService.getValue(badgeKey);
					if (badgeJson != null) {
						ObjectNode badgeNode = (ObjectNode) mapper.readTree(badgeJson);
						// Process badge images
						ArrayNode badgeImagesArray = badgeNode.has("images") ? (ArrayNode) badgeNode.get("images")
								: mapper.createArrayNode();
						ArrayNode badgeImagesData = mapper.createArrayNode();
						for (int m = 0; m < badgeImagesArray.size(); m++) {
							String badgeImageId = badgeImagesArray.get(m).asText();
							String badgeImageKey = "image:" + badgeImageId;
							String badgeImageJson = redisService.getValue(badgeImageKey);
							if (badgeImageJson != null) {
								ObjectNode badgeImageNode = (ObjectNode) mapper.readTree(badgeImageJson);
								badgeImagesData.add(badgeImageNode);
							}
						}
						badgeNode.set("images", badgeImagesData);
						badgesData.add(badgeNode); // Directly add badge object
					}
				}
				showData.set("badges", badgesData);

				// Process genres
				ArrayNode genresArray = (ArrayNode) showNode.get("genres");
				ArrayNode genresData = mapper.createArrayNode();
				for (int k = 0; k < genresArray.size(); k++) {
					String genreId = genresArray.get(k).asText();
					String genreKey = "genre:" + genreId;
					String genreJson = redisService.getValue(genreKey);
					if (genreJson != null) {
						ObjectNode genreNode = (ObjectNode) mapper.readTree(genreJson);
						// Process badge images
						ArrayNode genreImagesArray = genreNode.has("images") ? (ArrayNode) genreNode.get("images")
								: mapper.createArrayNode();
						ArrayNode genreImagesData = mapper.createArrayNode();
						for (int m = 0; m < genreImagesArray.size(); m++) {
							String genreImageId = genreImagesArray.get(m).asText();
							String genreImageKey = "image:" + genreImageId;
							String genreImageJson = redisService.getValue(genreImageKey);
							if (genreImageJson != null) {
								ObjectNode badgeImageNode = (ObjectNode) mapper.readTree(genreImageJson);
								genreImagesData.add(badgeImageNode);
							}
						}
						genreNode.set("images", genreImagesData);
						genresData.add(genreNode); // Directly add badge object
					}
				}
				showData.set("genres", genresData);

				// Process exclusiveBadges if present
				if (showNode.has("exclusiveBadges")) {
					ArrayNode exclusiveBadgesArray = (ArrayNode) showNode.get("exclusiveBadges");
					showData.set("exclusiveBadges", exclusiveBadgesArray);
				}

				contentsArray.add(showData);
			}
			rowData.set("contents", contentsArray);
			dataArray.add(rowData);
		}

		resultJson.set("data", dataArray);

		return resultJson;

	}

	// For redis improved performance with batch gets
	public static ObjectNode parseQueueJson(RedisService redisService, ObjectMapper mapper, String queueJson)
			throws JsonProcessingException {

		ObjectNode resultJson = mapper.createObjectNode();
		ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
		ArrayNode rowIds = (ArrayNode) queueNode.get("rows");
		ArrayNode dataArray = mapper.createArrayNode();

		// Local cache to avoid repeated Redis hits
		Map<String, String> redisCache = new HashMap<>();

		for (JsonNode rowIdNode : rowIds) {
			String rowId = rowIdNode.asText();
			String rowKey = "row:" + rowId;
			String rowJson = redisCache.computeIfAbsent(rowKey, redisService::getValue);
			if (rowJson == null)
				continue;

			ObjectNode rowNode = (ObjectNode) mapper.readTree(rowJson);
			ObjectNode rowData = mapper.createObjectNode();
			rowData.put("id", Integer.parseInt(rowId));
			rowData.put("rowType", rowNode.path("rowType").asText());

			ArrayNode showsArray = (ArrayNode) rowNode.path("shows");
			if (showsArray == null || showsArray.isEmpty())
				continue;

			// --- Collect all show keys for batch retrieval ---
			List<String> showKeys = new ArrayList<>();
			for (JsonNode showIdNode : showsArray) {
				showKeys.add("show:" + showIdNode.asText());
			}

			Map<String, String> showMap = redisService.mget(showKeys); // requires RedisService batch support
			ArrayNode contentsArray = mapper.createArrayNode();

			for (JsonNode showIdNode : showsArray) {
				String showId = showIdNode.asText();
				String showKey = "show:" + showId;
				String showJson = showMap.get(showKey);
				if (showJson == null)
					continue;

				ObjectNode showNode = (ObjectNode) mapper.readTree(showJson);
				ObjectNode showData = mapper.createObjectNode();

				showData.put("id", Integer.parseInt(showId));
				showData.put("contentType", showNode.path("contentType").asText());
				showData.put("description", showNode.path("description").asText(""));
				showData.put("favorite", showNode.path("favorite").asBoolean(false));
				showData.put("madeYear", showNode.path("madeYear").asInt(0));
				showData.put("spot", showNode.path("spot").asText(""));
				showData.put("title", showNode.path("title").asText(""));

				// --- IMAGES ---
				ArrayNode imageIds = (ArrayNode) showNode.path("images");
				ArrayNode imageDataArray = mapper.createArrayNode();
				if (imageIds != null && !imageIds.isEmpty()) {
					List<String> imageKeys = new ArrayList<>();
					for (JsonNode idNode : imageIds)
						imageKeys.add("image:" + idNode.asText());

					Map<String, String> imageMap = redisService.mget(imageKeys);
					for (String json : imageMap.values()) {
						if (json != null)
							imageDataArray.add(mapper.readTree(json));
					}
				}
				showData.set("images", imageDataArray);

				// --- BADGES ---
				ArrayNode badgeIds = (ArrayNode) showNode.path("badges");
				ArrayNode badgeDataArray = mapper.createArrayNode();
				if (badgeIds != null && !badgeIds.isEmpty()) {
					List<String> badgeKeys = new ArrayList<>();
					for (JsonNode idNode : badgeIds)
						badgeKeys.add("badges:" + idNode.asText());

					Map<String, String> badgeMap = redisService.mget(badgeKeys);
					for (String badgeJson : badgeMap.values()) {
						if (badgeJson == null)
							continue;

						ObjectNode badgeNode = (ObjectNode) mapper.readTree(badgeJson);
						ArrayNode badgeImageIds = (ArrayNode) badgeNode.path("images");
						ArrayNode badgeImagesData = mapper.createArrayNode();

						if (badgeImageIds != null && !badgeImageIds.isEmpty()) {
							List<String> badgeImageKeys = new ArrayList<>();
							for (JsonNode idNode : badgeImageIds)
								badgeImageKeys.add("image:" + idNode.asText());
							Map<String, String> badgeImageMap = redisService.mget(badgeImageKeys);
							for (String imgJson : badgeImageMap.values()) {
								if (imgJson != null)
									badgeImagesData.add(mapper.readTree(imgJson));
							}
						}

						badgeNode.set("images", badgeImagesData);
						badgeDataArray.add(badgeNode);
					}
				}
				showData.set("badges", badgeDataArray);

				// --- GENRES ---
				ArrayNode genreIds = (ArrayNode) showNode.path("genres");
				ArrayNode genreDataArray = mapper.createArrayNode();
				if (genreIds != null && !genreIds.isEmpty()) {
					List<String> genreKeys = new ArrayList<>();
					for (JsonNode idNode : genreIds)
						genreKeys.add("genre:" + idNode.asText());

					Map<String, String> genreMap = redisService.mget(genreKeys);
					for (String genreJson : genreMap.values()) {
						if (genreJson == null)
							continue;
						ObjectNode genreNode = (ObjectNode) mapper.readTree(genreJson);

						ArrayNode genreImageIds = (ArrayNode) genreNode.path("images");
						ArrayNode genreImagesData = mapper.createArrayNode();
						if (genreImageIds != null && !genreImageIds.isEmpty()) {
							List<String> genreImageKeys = new ArrayList<>();
							for (JsonNode idNode : genreImageIds)
								genreImageKeys.add("image:" + idNode.asText());
							Map<String, String> genreImageMap = redisService.mget(genreImageKeys);
							for (String imgJson : genreImageMap.values()) {
								if (imgJson != null)
									genreImagesData.add(mapper.readTree(imgJson));
							}
						}

						genreNode.set("images", genreImagesData);
						genreDataArray.add(genreNode);
					}
				}
				showData.set("genres", genreDataArray);

				// --- EXCLUSIVE BADGES ---
				if (showNode.has("exclusiveBadges")) {
					showData.set("exclusiveBadges", showNode.get("exclusiveBadges"));
				}

				contentsArray.add(showData);
			}

			rowData.set("contents", contentsArray);
			dataArray.add(rowData);
		}

		resultJson.set("data", dataArray);
		return resultJson;
	}


}