package com.tabii;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabii.rest.hazelcast.HazelcastService;
import com.tabii.utils.HcMaps;

public class HzcQueueToJson {

	// For hazelcast improved performance with batch gets
	public static ObjectNode parseQueueJson(HazelcastService hazelcastService, ObjectMapper mapper, String queueJson)
	        throws JsonProcessingException {

	    ObjectNode resultJson = mapper.createObjectNode();
	    ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
	    ArrayNode rowIds = (ArrayNode) queueNode.get("rows");
	    ArrayNode dataArray = mapper.createArrayNode();

	    for (JsonNode rowIdNode : rowIds) {
	        String rowId = rowIdNode.asText();
	        String rowJson = hazelcastService.getValue(HcMaps.ROWS.getMapName(), "row:" + rowId);
	        if (rowJson == null) continue;

	        ObjectNode rowNode = (ObjectNode) mapper.readTree(rowJson);
	        ObjectNode rowData = mapper.createObjectNode();
	        rowData.put("id", Integer.parseInt(rowId));
	        rowData.put("rowType", rowNode.path("rowType").asText());

	        ArrayNode contentsArray = mapper.createArrayNode();
	        ArrayNode showsArray = (ArrayNode) rowNode.path("shows");

	        for (JsonNode showIdNode : showsArray) {
	            String showId = showIdNode.asText();
	            String showJson = hazelcastService.getValue(HcMaps.SHOWS.getMapName(), "show:" + showId);
	            if (showJson == null) continue;

	            ObjectNode showNode = (ObjectNode) mapper.readTree(showJson);
	            ObjectNode showData = mapper.createObjectNode();

	            showData.put("id", Integer.parseInt(showId));
	            showData.put("contentType", showNode.path("contentType").asText());
	            showData.put("description", showNode.path("description").asText());
	            showData.put("favorite", showNode.path("favorite").asBoolean());
	            showData.put("madeYear", showNode.path("madeYear").asInt());
	            showData.put("spot", showNode.path("spot").asText());
	            showData.put("title", showNode.path("title").asText());

	            // Add nested data
	            showData.set("images", fetchNodes(mapper, hazelcastService, HcMaps.IMAGES, showNode.path("images")));
	            showData.set("badges", fetchNodesWithImages(mapper, hazelcastService, HcMaps.BADEGES, showNode.path("badges")));
	            showData.set("genres", fetchNodesWithImages(mapper, hazelcastService, HcMaps.GENRE, showNode.path("genres")));

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
	private static ArrayNode fetchNodes(ObjectMapper mapper, HazelcastService hazelcastService, HcMaps mapType, JsonNode idArray)
	        throws JsonProcessingException {
	    ArrayNode resultArray = mapper.createArrayNode();
	    if (idArray == null || !idArray.isArray()) return resultArray;

	    for (JsonNode idNode : idArray) {
	        String key = getKeyPrefix(mapType) + idNode.asText();
	        String json = hazelcastService.getValue(mapType.getMapName(), key);
	        if (json != null) {
	            resultArray.add(mapper.readTree(json));
	        }
	    }
	    return resultArray;
	}

	/**
	 * Fetches nodes (like badges/genres) that also contain nested image IDs.
	 */
	private static ArrayNode fetchNodesWithImages(ObjectMapper mapper, HazelcastService hazelcastService,
	                                              HcMaps mapType, JsonNode idArray)
	        throws JsonProcessingException {
	    ArrayNode resultArray = mapper.createArrayNode();
	    if (idArray == null || !idArray.isArray()) return resultArray;

	    for (JsonNode idNode : idArray) {
	        String key = getKeyPrefix(mapType) + idNode.asText();
	        String json = hazelcastService.getValue(mapType.getMapName(), key);
	        if (json == null) continue;

	        ObjectNode node = (ObjectNode) mapper.readTree(json);
	        node.set("images", fetchNodes(mapper, hazelcastService, HcMaps.IMAGES, node.path("images")));
	        resultArray.add(node);
	    }
	    return resultArray;
	}

	/**
	 * Generates consistent key prefixes based on map type.
	 */
	private static String getKeyPrefix(HcMaps mapType) {
	    switch (mapType) {
	        case ROWS: return "row:";
	        case SHOWS: return "show:";
	        case IMAGES: return "image:";
	        case BADEGES: return "badges:";
	        case GENRE: return "genre:";
	        case EXCBADGES: return "exclusiveBadge:";
	        default: return "";
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
