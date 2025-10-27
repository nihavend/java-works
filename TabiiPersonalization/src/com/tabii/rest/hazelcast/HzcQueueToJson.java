package com.tabii.rest.hazelcast;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabii.utils.HcMaps;

public class HzcQueueToJson {

	// For hazelcast improved performance with batch gets
	// === Main Parser ===
	public static ObjectNode parseQueueJson(HazelcastService hazelcastService, ObjectMapper mapper, String queueJson)
			throws JsonProcessingException {

		ObjectNode resultJson = mapper.createObjectNode();
		ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
		ArrayNode rowIds = (ArrayNode) queueNode.path("rows");

		if (rowIds == null || rowIds.isEmpty()) {
			resultJson.set("data", mapper.createArrayNode());
			return resultJson;
		}

		// ---- 1️⃣ Batch fetch all rows ----
		Set<String> rowKeys = new HashSet<>();
		for (JsonNode rowIdNode : rowIds) {
			rowKeys.add("row:" + rowIdNode.asText());
		}
		Map<String, Object> rowMap = hazelcastService.getAllValues(HcMaps.ROWS.getMapName(), rowKeys);

		ArrayNode dataArray = mapper.createArrayNode();

		// ---- 2️⃣ Iterate over rows ----
		for (Map.Entry<String, Object> rowEntry : rowMap.entrySet()) {
			String rowKey = rowEntry.getKey();
			String rowJson = (String) rowEntry.getValue();
			if (rowJson == null)
				continue;

			String rowId = rowKey.substring("row:".length());
			ObjectNode rowNode = (ObjectNode) mapper.readTree(rowJson);

			ObjectNode rowData = mapper.createObjectNode();
			rowData.put("id", rowId);
			rowData.put("rowType", rowNode.path("rowType").asText());

			ArrayNode showsArray = (ArrayNode) rowNode.path("shows");
			if (showsArray == null || showsArray.isEmpty())
				continue;

			// ---- 3️⃣ Batch fetch shows ----
			Set<String> showKeys = new HashSet<>();
			for (JsonNode showIdNode : showsArray) {
				showKeys.add("show:" + showIdNode.asText());
			}
			Map<String, Object> showMap = hazelcastService.getAllValues(HcMaps.SHOWS.getMapName(), showKeys);

			ArrayNode contentsArray = mapper.createArrayNode();

			for (Map.Entry<String, Object> showEntry : showMap.entrySet()) {
				String showKey = showEntry.getKey();
				String showJson = (String) showEntry.getValue();
				if (showJson == null)
					continue;

				String showId = showKey.substring("show:".length());
				ObjectNode showNode = (ObjectNode) mapper.readTree(showJson);
				ObjectNode showData = mapper.createObjectNode();

				showData.put("id", showId);
				showData.put("contentType", showNode.path("contentType").asText());
				showData.put("description", showNode.path("description").asText());
				showData.put("favorite", showNode.path("favorite").asBoolean());
				showData.put("madeYear", showNode.path("madeYear").asInt());
				showData.put("spot", showNode.path("spot").asText());
				showData.put("title", showNode.path("title").asText());

				// ---- 4️⃣ Nested data with batched helper methods ----
				showData.set("images", fetchNodes(mapper, hazelcastService, HcMaps.IMAGES, showNode.path("images")));
				showData.set("badges",
						fetchNodesWithImages(mapper, hazelcastService, HcMaps.BADGES, showNode.path("badges")));
				showData.set("genres",
						fetchNodesWithImages(mapper, hazelcastService, HcMaps.GENRE, showNode.path("genres")));

				if (showNode.has("exclusiveBadges")) {
					showData.set("exclusiveBadges", showNode.path("exclusiveBadges"));
				}

				contentsArray.add(showData);
			}

			rowData.set("contents", contentsArray);
			dataArray.add(rowData);
		}

		resultJson.set("data", dataArray);
		return resultJson;
	}

	/**
	 * Fetches simple object nodes (like images) by ID list.
	 */
	private static ArrayNode fetchNodes(ObjectMapper mapper, HazelcastService hazelcastService, HcMaps mapType,
			JsonNode idArray) throws JsonProcessingException {

		ArrayNode resultArray = mapper.createArrayNode();
		if (idArray == null || !idArray.isArray() || idArray.isEmpty())
			return resultArray;

		// Build all keys first
		Set<String> keys = new HashSet<>();
		for (JsonNode idNode : idArray) {
			keys.add("image:" + idNode.asText());
		}

		// Batch fetch
		Map<String, Object> dataMap = hazelcastService.getAllValues(mapType.getMapName(), keys);

		for (Object value : dataMap.values()) {
			if (value != null) {
				resultArray.add(mapper.readTree(value.toString()));
			}
		}

		return resultArray;
	}

	/**
	 * Fetches nodes (like badges/genres) that also contain nested image IDs.
	 */
	private static ArrayNode fetchNodesWithImages(ObjectMapper mapper, HazelcastService hazelcastService,
			HcMaps mapType, JsonNode idArray) throws JsonProcessingException {

		ArrayNode resultArray = mapper.createArrayNode();
		if (idArray == null || !idArray.isArray() || idArray.isEmpty())
			return resultArray;

// ---- 1️⃣ Batch fetch all parent objects (badges / genres) ----
		Set<String> keys = new HashSet<>();
		for (JsonNode idNode : idArray) {
			keys.add(mapType.name().toLowerCase() + ":" + idNode.asText());
		}
		Map<String, Object> parentMap = hazelcastService.getAllValues(mapType.getMapName(), keys);

// ---- 2️⃣ Collect all nested image IDs ----
		Set<String> nestedImageKeys = new HashSet<>();
		Map<String, ObjectNode> parentNodes = new HashMap<>();

		for (Map.Entry<String, Object> entry : parentMap.entrySet()) {
			if (entry.getValue() == null)
				continue;

			ObjectNode parentNode = (ObjectNode) mapper.readTree(entry.getValue().toString());
			parentNodes.put(entry.getKey(), parentNode);

			ArrayNode imageIds = (ArrayNode) parentNode.path("images");
			if (imageIds != null) {
				for (JsonNode imgId : imageIds) {
					nestedImageKeys.add("image:" + imgId.asText());
				}
			}
		}

		// ---- 3️⃣ Batch fetch all nested images ----
		Map<String, Object> imageMap = nestedImageKeys.isEmpty() ? Collections.emptyMap()
				: hazelcastService.getAllValues(HcMaps.IMAGES.getMapName(), nestedImageKeys);

		// ---- 4️⃣ Attach images back to each parent ----
		for (ObjectNode parentNode : parentNodes.values()) {
			ArrayNode imageIds = (ArrayNode) parentNode.path("images");
			ArrayNode imagesArray = mapper.createArrayNode();

			if (imageIds != null) {
				for (JsonNode imgId : imageIds) {
					String key = "image:" + imgId.asText();
					Object imgJson = imageMap.get(key);
					if (imgJson != null) {
						imagesArray.add(mapper.readTree(imgJson.toString()));
					}
				}
			}

			parentNode.set("images", imagesArray);
			resultArray.add(parentNode);
		}

		return resultArray;
	}

	/**
	 * Generates consistent key prefixes based on map type.
	 */
	private static String getKeyPrefix(HcMaps mapType) {
		switch (mapType) {
		case ROWS:
			return "row:";
		case SHOWS:
			return "show:";
		case IMAGES:
			return "image:";
		case BADGES:
			return "badges:";
		case GENRE:
			return "genre:";
		case EXCBADGES:
			return "exclusiveBadge:";
		default:
			return "";
		}
	}

	// For hazelcast bad performance
	public static ObjectNode parseQueueJson1(HazelcastService hazelcastService, ObjectMapper mapper, String queueJson)
			throws JsonProcessingException {

		ObjectNode resultJson = mapper.createObjectNode();

		// Parse queue JSON to get row IDs
		ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
		ArrayNode rowIds = (ArrayNode) queueNode.get("rows");

		ArrayNode dataArray = mapper.createArrayNode();

		// Process each row
		for (int i = 0; i < rowIds.size(); i++) {

			String rowId = rowIds.get(i).asText();
			String rowKey = "row:" + rowId;
			String rowJson = hazelcastService.getValue(HcMaps.ROWS.getMapName(), rowKey);

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
				String showJson = hazelcastService.getValue(HcMaps.SHOWS.getMapName(), showKey);
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
					String imageJson = hazelcastService.getValue(HcMaps.IMAGES.getMapName(), imageKey);
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
					String badgeJson = hazelcastService.getValue(badgeKey);
					if (badgeJson != null) {
						ObjectNode badgeNode = (ObjectNode) mapper.readTree(badgeJson);
						// Process badge images
						ArrayNode badgeImagesArray = badgeNode.has("images") ? (ArrayNode) badgeNode.get("images")
								: mapper.createArrayNode();
						ArrayNode badgeImagesData = mapper.createArrayNode();
						for (int m = 0; m < badgeImagesArray.size(); m++) {
							String badgeImageId = badgeImagesArray.get(m).asText();
							String badgeImageKey = "image:" + badgeImageId;
							String badgeImageJson = hazelcastService.getValue(badgeImageKey).toString();
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
					String genreJson = hazelcastService.getValue(genreKey);
					if (genreJson != null) {
						ObjectNode genreNode = (ObjectNode) mapper.readTree(genreJson);
						// Process badge images
						ArrayNode genreImagesArray = genreNode.has("images") ? (ArrayNode) genreNode.get("images")
								: mapper.createArrayNode();
						ArrayNode genreImagesData = mapper.createArrayNode();
						for (int m = 0; m < genreImagesArray.size(); m++) {
							String genreImageId = genreImagesArray.get(m).asText();
							String genreImageKey = "image:" + genreImageId;
							String genreImageJson = hazelcastService.getValue(genreImageKey).toString();
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
}
