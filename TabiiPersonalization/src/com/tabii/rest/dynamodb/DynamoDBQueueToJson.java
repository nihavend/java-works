package com.tabii.rest.dynamodb;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class DynamoDBQueueToJson {

	// For dynamodb bad performance
	public static ObjectNode parseQueueJson1(DynamoDBService dynamoDBService, ObjectMapper mapper, String queueJson)
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
			Item rowJson = dynamoDBService.getById("rows", rowKey);

			if (rowJson == null)
				continue;

			ObjectNode rowNode = (ObjectNode) mapper.readTree(rowJson.getDescription());
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
				Item showJson = dynamoDBService.getById("shows", showKey);
				if (showJson == null)
					continue;

				ObjectNode showNode = (ObjectNode) mapper.readTree(showJson.getDescription());
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
					Item imageJson = dynamoDBService.getById("images", imageKey);
					if (imageJson != null) {
						ObjectNode imageNode = (ObjectNode) mapper.readTree(imageJson.getDescription());
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
					Item badgeJson = dynamoDBService.getById("badges", badgeKey);
					if (badgeJson != null) {
						ObjectNode badgeNode = (ObjectNode) mapper.readTree(badgeJson.getDescription());
						// Process badge images
						ArrayNode badgeImagesArray = badgeNode.has("images") ? (ArrayNode) badgeNode.get("images")
								: mapper.createArrayNode();
						ArrayNode badgeImagesData = mapper.createArrayNode();
						for (int m = 0; m < badgeImagesArray.size(); m++) {
							String badgeImageId = badgeImagesArray.get(m).asText();
							String badgeImageKey = "image:" + badgeImageId;
							Item badgeImageJson = dynamoDBService.getById("images", badgeImageKey);
							if (badgeImageJson != null) {
								ObjectNode badgeImageNode = (ObjectNode) mapper.readTree(badgeImageJson.getDescription());
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
					Item genreJson = dynamoDBService.getById("genres", genreKey);
					if (genreJson != null) {
						ObjectNode genreNode = (ObjectNode) mapper.readTree(genreJson.getDescription());
						// Process badge images
						ArrayNode genreImagesArray = genreNode.has("images") ? (ArrayNode) genreNode.get("images")
								: mapper.createArrayNode();
						ArrayNode genreImagesData = mapper.createArrayNode();
						for (int m = 0; m < genreImagesArray.size(); m++) {
							String genreImageId = genreImagesArray.get(m).asText();
							String genreImageKey = "image:" + genreImageId;
							Item genreImageJson = dynamoDBService.getById("images", genreImageKey);
							if (genreImageJson != null) {
								ObjectNode badgeImageNode = (ObjectNode) mapper.readTree(genreImageJson.getDescription());
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

	// For dynamodb improved performance
	public static ObjectNode parseQueueJson(
	        DynamoDBService dynamoDBService,
	        ObjectMapper mapper,
	        String queueJson) throws JsonProcessingException {

	    ObjectNode resultJson = mapper.createObjectNode();
	    ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
	    ArrayNode rowIds = (ArrayNode) queueNode.get("rows");

	    ArrayNode dataArray = mapper.createArrayNode();

	    // --- Simple in-memory cache for reused entities ---
	    Map<String, ObjectNode> cache = new HashMap<>();

	    for (JsonNode rowIdNode : rowIds) {
	        String rowId = rowIdNode.asText();
	        String rowKey = "row:" + rowId;

	        Item rowItem = dynamoDBService.getById("rows", rowKey);
	        if (rowItem == null) continue;

	        ObjectNode rowNode = (ObjectNode) mapper.readTree(rowItem.getDescription());
	        ObjectNode rowData = mapper.createObjectNode();
	        rowData.put("id", Integer.parseInt(rowId));
	        rowData.put("rowType", rowNode.path("rowType").asText());

	        ArrayNode showsArray = (ArrayNode) rowNode.path("shows");
	        ArrayNode contentsArray = mapper.createArrayNode();

	        for (JsonNode showIdNode : showsArray) {
	            String showId = showIdNode.asText();
	            String showKey = "show:" + showId;

	            Item showItem = dynamoDBService.getById("shows", showKey);
	            if (showItem == null) continue;

	            ObjectNode showNode = (ObjectNode) mapper.readTree(showItem.getDescription());
	            ObjectNode showData = mapper.createObjectNode();

	            showData.put("id", Integer.parseInt(showId));
	            showData.put("contentType", showNode.path("contentType").asText());
	            showData.put("description", showNode.path("description").asText());
	            showData.put("favorite", showNode.path("favorite").asBoolean());
	            showData.put("madeYear", showNode.path("madeYear").asInt());
	            showData.put("spot", showNode.path("spot").asText());
	            showData.put("title", showNode.path("title").asText());

	            // ---- Images (with cache) ----
	            ArrayNode imagesData = mapper.createArrayNode();
	            for (JsonNode imageIdNode : showNode.path("images")) {
	                String imageId = imageIdNode.asText();
	                String imageKey = "image:" + imageId;

	                ObjectNode imageNode = cache.computeIfAbsent(imageKey, key -> {
	                    Item item = dynamoDBService.getById("images", key);
	                    if (item == null) return null;
	                    try {
	                        return (ObjectNode) mapper.readTree(item.getDescription());
	                    } catch (JsonProcessingException e) {
	                        return null;
	                    }
	                });
	                if (imageNode != null) imagesData.add(imageNode);
	            }
	            showData.set("images", imagesData);

	            // ---- Badges ----
	            ArrayNode badgesData = mapper.createArrayNode();
	            for (JsonNode badgeIdNode : showNode.path("badges")) {
	                String badgeId = badgeIdNode.asText();
	                String badgeKey = "badges:" + badgeId;

	                ObjectNode badgeNode = cache.computeIfAbsent(badgeKey, key -> {
	                    Item item = dynamoDBService.getById("badges", key);
	                    if (item == null) return null;
	                    try {
	                        return (ObjectNode) mapper.readTree(item.getDescription());
	                    } catch (JsonProcessingException e) {
	                        return null;
	                    }
	                });
	                if (badgeNode == null) continue;

	                // Badge images (cached)
	                ArrayNode badgeImagesData = mapper.createArrayNode();
	                for (JsonNode badgeImageIdNode : badgeNode.path("images")) {
	                    String badgeImageId = badgeImageIdNode.asText();
	                    String badgeImageKey = "image:" + badgeImageId;

	                    ObjectNode badgeImageNode = cache.computeIfAbsent(badgeImageKey, key -> {
	                        Item item = dynamoDBService.getById("images", key);
	                        if (item == null) return null;
	                        try {
	                            return (ObjectNode) mapper.readTree(item.getDescription());
	                        } catch (JsonProcessingException e) {
	                            return null;
	                        }
	                    });
	                    if (badgeImageNode != null) badgeImagesData.add(badgeImageNode);
	                }
	                badgeNode.set("images", badgeImagesData);
	                badgesData.add(badgeNode);
	            }
	            showData.set("badges", badgesData);

	            // ---- Genres ----
	            ArrayNode genresData = mapper.createArrayNode();
	            for (JsonNode genreIdNode : showNode.path("genres")) {
	                String genreId = genreIdNode.asText();
	                String genreKey = "genre:" + genreId;

	                ObjectNode genreNode = cache.computeIfAbsent(genreKey, key -> {
	                    Item item = dynamoDBService.getById("genres", key);
	                    if (item == null) return null;
	                    try {
	                        return (ObjectNode) mapper.readTree(item.getDescription());
	                    } catch (JsonProcessingException e) {
	                        return null;
	                    }
	                });
	                if (genreNode == null) continue;

	                // Genre images
	                ArrayNode genreImagesData = mapper.createArrayNode();
	                for (JsonNode genreImageIdNode : genreNode.path("images")) {
	                    String genreImageId = genreImageIdNode.asText();
	                    String genreImageKey = "image:" + genreImageId;

	                    ObjectNode genreImageNode = cache.computeIfAbsent(genreImageKey, key -> {
	                        Item item = dynamoDBService.getById("images", key);
	                        if (item == null) return null;
	                        try {
	                            return (ObjectNode) mapper.readTree(item.getDescription());
	                        } catch (JsonProcessingException e) {
	                            return null;
	                        }
	                    });
	                    if (genreImageNode != null) genreImagesData.add(genreImageNode);
	                }
	                genreNode.set("images", genreImagesData);
	                genresData.add(genreNode);
	            }
	            showData.set("genres", genresData);

	            if (showNode.has("exclusiveBadges"))
	                showData.set("exclusiveBadges", showNode.get("exclusiveBadges"));

	            contentsArray.add(showData);
	        }
	        rowData.set("contents", contentsArray);
	        dataArray.add(rowData);
	    }

	    resultJson.set("data", dataArray);
	    return resultJson;
	}

}