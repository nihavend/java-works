package com.tabii;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabii.rest.memcached.MemcachedService;

public class MemcQueueToJson {

	// For memcached improved performance with batch gets
	public static ObjectNode parseQueueJson(MemcachedService memcachedService, ObjectMapper mapper, String queueJson)
			throws JsonProcessingException {

		ObjectNode resultJson = mapper.createObjectNode();

		// --- Parse queue JSON
		ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
		ArrayNode rowIds = (ArrayNode) queueNode.get("rows");

		if (rowIds == null || rowIds.isEmpty()) {
			resultJson.putArray("data");
			return resultJson;
		}

		// --- Bulk fetch all rows at once
		List<String> rowKeys = new ArrayList<>();
		for (JsonNode idNode : rowIds)
			rowKeys.add("row:" + idNode.asText());
		Map<String, Object> rowDataMap = memcachedService.getBulkValues(rowKeys);

		ArrayNode dataArray = mapper.createArrayNode();

		for (JsonNode rowIdNode : rowIds) {
			String rowId = rowIdNode.asText();
			Object rowValue = rowDataMap.get("row:" + rowId);
			if (rowValue == null)
				continue;

			ObjectNode rowNode = (ObjectNode) mapper.readTree(rowValue.toString());
			ObjectNode rowData = mapper.createObjectNode();
			rowData.put("id", Integer.parseInt(rowId));
			rowData.put("rowType", rowNode.path("rowType").asText());

			ArrayNode showsArray = (ArrayNode) rowNode.get("shows");
			if (showsArray == null || showsArray.isEmpty())
				continue;

			// --- Bulk fetch all shows for this row
			List<String> showKeys = new ArrayList<>();
			for (JsonNode showId : showsArray)
				showKeys.add("show:" + showId.asText());
			Map<String, Object> showMap = memcachedService.getBulkValues(showKeys);

			ArrayNode contentsArray = mapper.createArrayNode();

			for (JsonNode showIdNode : showsArray) {
				String showId = showIdNode.asText();
				Object showValue = showMap.get("show:" + showId);
				if (showValue == null)
					continue;

				ObjectNode showNode = (ObjectNode) mapper.readTree(showValue.toString());
				ObjectNode showData = mapper.createObjectNode();
				showData.put("id", Integer.parseInt(showId));
				showData.put("contentType", showNode.path("contentType").asText());
				showData.put("description", showNode.path("description").asText());
				showData.put("favorite", showNode.path("favorite").asBoolean());
				showData.put("madeYear", showNode.path("madeYear").asInt());
				showData.put("spot", showNode.path("spot").asText());
				showData.put("title", showNode.path("title").asText());

				// --- Bulk fetch all image, badge, genre keys in one go
				List<String> imageKeys = new ArrayList<>();
				collectPrefixedKeys(showNode, "images", "image:", imageKeys);
				collectPrefixedKeys(showNode, "badges", "badges:", imageKeys);
				collectPrefixedKeys(showNode, "genres", "genre:", imageKeys);

				Map<String, Object> subObjects = imageKeys.isEmpty() ? Collections.emptyMap()
						: memcachedService.getBulkValues(imageKeys);

				// --- Process images
				showData.set("images", buildNodeArray(mapper, subObjects, showNode.get("images"), "image:"));

				// --- Process badges
				ArrayNode badgesArray = (ArrayNode) showNode.get("badges");
				ArrayNode badgesData = mapper.createArrayNode();
				if (badgesArray != null) {
					for (JsonNode badgeIdNode : badgesArray) {
						String badgeId = badgeIdNode.asText();
						Object badgeJson = subObjects.get("badges:" + badgeId);
						if (badgeJson == null)
							continue;
						ObjectNode badgeNode = (ObjectNode) mapper.readTree(badgeJson.toString());

						// Process badge images in one batch
						ArrayNode badgeImages = (ArrayNode) badgeNode.get("images");
						ArrayNode badgeImageNodes = buildNodeArray(mapper, subObjects, badgeImages, "image:");
						badgeNode.set("images", badgeImageNodes);

						badgesData.add(badgeNode);
					}
				}
				showData.set("badges", badgesData);

				// --- Process genres
				ArrayNode genresArray = (ArrayNode) showNode.get("genres");
				ArrayNode genresData = mapper.createArrayNode();
				if (genresArray != null) {
					for (JsonNode genreIdNode : genresArray) {
						String genreId = genreIdNode.asText();
						Object genreJson = subObjects.get("genre:" + genreId);
						if (genreJson == null)
							continue;
						ObjectNode genreNode = (ObjectNode) mapper.readTree(genreJson.toString());

						ArrayNode genreImages = (ArrayNode) genreNode.get("images");
						ArrayNode genreImageNodes = buildNodeArray(mapper, subObjects, genreImages, "image:");
						genreNode.set("images", genreImageNodes);

						genresData.add(genreNode);
					}
				}
				showData.set("genres", genresData);

				// --- Exclusive badges
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

	/** Collects keys with given prefix from a JSON node array */
	private static void collectPrefixedKeys(ObjectNode parent, String field, String prefix, List<String> out) {
		JsonNode arr = parent.get(field);
		if (arr != null && arr.isArray()) {
			for (JsonNode n : arr)
				out.add(prefix + n.asText());
		}
	}

	/** Builds array of parsed nodes for fetched keys */
	private static ArrayNode buildNodeArray(ObjectMapper mapper, Map<String, Object> bulkData, JsonNode idArray,
			String prefix) throws JsonProcessingException {
		ArrayNode result = mapper.createArrayNode();
		if (idArray == null || !idArray.isArray())
			return result;
		for (JsonNode idNode : idArray) {
			Object json = bulkData.get(prefix + idNode.asText());
			if (json != null)
				result.add(mapper.readTree(json.toString()));
		}
		return result;
	}

	// For memcached bad performance
	public static ObjectNode parseQueueJson1(MemcachedService memcachedService, ObjectMapper mapper, String queueJson)
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
			String rowJson = memcachedService.getValue(rowKey).toString();

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
				String showJson = memcachedService.getValue(showKey).toString();
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
					String imageJson = memcachedService.getValue(imageKey).toString();
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
					String badgeJson = memcachedService.getValue(badgeKey).toString();
					if (badgeJson != null) {
						ObjectNode badgeNode = (ObjectNode) mapper.readTree(badgeJson);
						// Process badge images
						ArrayNode badgeImagesArray = badgeNode.has("images") ? (ArrayNode) badgeNode.get("images")
								: mapper.createArrayNode();
						ArrayNode badgeImagesData = mapper.createArrayNode();
						for (int m = 0; m < badgeImagesArray.size(); m++) {
							String badgeImageId = badgeImagesArray.get(m).asText();
							String badgeImageKey = "image:" + badgeImageId;
							String badgeImageJson = memcachedService.getValue(badgeImageKey).toString();
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
					String genreJson = memcachedService.getValue(genreKey).toString();
					if (genreJson != null) {
						ObjectNode genreNode = (ObjectNode) mapper.readTree(genreJson);
						// Process badge images
						ArrayNode genreImagesArray = genreNode.has("images") ? (ArrayNode) genreNode.get("images")
								: mapper.createArrayNode();
						ArrayNode genreImagesData = mapper.createArrayNode();
						for (int m = 0; m < genreImagesArray.size(); m++) {
							String genreImageId = genreImagesArray.get(m).asText();
							String genreImageKey = "image:" + genreImageId;
							String genreImageJson = memcachedService.getValue(genreImageKey).toString();
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
