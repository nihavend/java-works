package com.tabii;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.utils.CommonUtils;
import com.tabii.utils.RedisProperties;
import redis.clients.jedis.Jedis;
import java.io.File;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class QueueToJson {
	
	public static void main(String[] args) {
	
		RedisProperties redisProperties = CommonUtils.getRedisConnectionProps();
		
		try (Jedis jedis = new Jedis(redisProperties.getHost(), redisProperties.getPort())) {
		
			// Create ObjectMapper for JSON handling
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode resultJson = mapper.createObjectNode();
			ArrayNode dataArray = mapper.createArrayNode();

			// Get queue data (e.g., queue:1)
			String queueKey = "queue:1";
			String queueJson = jedis.get(queueKey);
			if (queueJson == null) {
				System.out.println("Queue not found: " + queueKey);
				return;
			}

			// Parse queue JSON to get row IDs
			ObjectNode queueNode = (ObjectNode) mapper.readTree(queueJson);
			ArrayNode rowIds = (ArrayNode) queueNode.get("rows");

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

			// Write to file
			mapper.writerWithDefaultPrettyPrinter().writeValue(new File("queue_1.json"), resultJson);
			System.out.println("JSON written to queue_1.json");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}