package com.tabii;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import redis.clients.jedis.Jedis;

public class QueueToJson {
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		// Scanner scanner = new Scanner(System.in);
		// System.out.print("Enter queue ID: ");
		String id = "1";// scanner.nextLine().trim();
		
		String queueKey = "queue:" + id; // Construct queue key as queue:id

		Jedis jedis = new Jedis("localhost", 6379); // Redis connection with port 6379
		String queueJsonStr = jedis.get(queueKey);
		if (queueJsonStr == null) {
			System.out.println("Queue data not found in Redis for key: " + queueKey);
			return;
		}

		JsonObject queueJson = JsonParser.parseString(queueJsonStr).getAsJsonObject();

		// Remove queue ID from queue JSON if it exists (e.g., "id" field)
		queueJson.remove("id");

		// Get "rows" array and rename to "data"
		JsonArray rowsIds = queueJson.getAsJsonArray("rows");
		if (rowsIds == null) {
			System.out.println("No 'rows' array found in queue data.");
			return;
		}

		JsonArray fullData = new JsonArray();
		for (JsonElement rowIdElem : rowsIds) {
			BigInteger rowId = rowIdElem.getAsBigInteger();
			String rowKey = "row:" + rowId; // Construct row key as row:<row_id>
			String rowJsonStr = jedis.get(rowKey);
			if (rowJsonStr == null) {
				System.out.println("Row data not found for key: " + rowKey);
				continue;
			}

			JsonObject rowJson = JsonParser.parseString(rowJsonStr).getAsJsonObject();
			rowJson.addProperty("id", rowId); // Add row ID as "id" field

			// Get "shows" array and remove the key, replacing with "content"
			JsonArray showsIds = rowJson.getAsJsonArray("shows");
			if (showsIds != null) {
				JsonArray fullShows = new JsonArray();
				for (JsonElement showIdElem : showsIds) {
					BigInteger showId = showIdElem.getAsBigInteger();
					String showKey = "show:" + showId; // Construct show key as show:<show_id>
					String showJsonStr = jedis.get(showKey);
					if (showJsonStr == null) {
						System.out.println("Show data not found for key: " + showKey);
						continue;
					}

					JsonObject showJson = JsonParser.parseString(showJsonStr).getAsJsonObject();
					showJson.addProperty("id", showId); // Add show ID as "id" field

					// Process badge, genre, and image arrays
					String[] objectArrayFields = { "badges", "genres", "images" };
					for (String field : objectArrayFields) {
						JsonArray objIds = showJson.getAsJsonArray(field);
						if (objIds != null) {
							JsonArray fullObjs = new JsonArray();
							for (JsonElement objIdElem : objIds) {
								BigInteger objId = objIdElem.getAsBigInteger();
								// String objKey = field.substring(0, field.length()) + ":" + objId;
								String objKey = field.substring(0, field.length() - 1) + ":" + objId; // e.g.,
								if(field.substring(0, field.length() - 1).equals("badge")) {
									objKey = field.substring(0, field.length()) + ":" + objId;
								}
								// e.g.,
								// badge:<id>, genre:<id>, image:<id>
								String objJsonStr = jedis.get(objKey);
								if (objJsonStr == null) {
									System.out.println(field.substring(0, field.length() - 1)
											+ " data not found for key: " + objKey);
									continue;
								}

								JsonObject objJson = JsonParser.parseString(objJsonStr).getAsJsonObject();
								objJson.addProperty("id", objId); // Add object ID as "id" field

								// Recursively process nested badge, genre, image arrays within this object
								for (String nestedField : objectArrayFields) {
									JsonArray nestedObjIds = objJson.getAsJsonArray(nestedField);
									if (nestedObjIds != null) {
										JsonArray fullNestedObjs = new JsonArray();
										for (JsonElement nestedObjIdElem : nestedObjIds) {
											BigInteger nestedObjId = nestedObjIdElem.getAsBigInteger();
											String nestedObjKey = nestedField.substring(0, nestedField.length() - 1)
													+ ":" + nestedObjId;
											String nestedObjJsonStr = jedis.get(nestedObjKey);
											if (nestedObjJsonStr == null) {
												System.out.println(nestedField.substring(0, nestedField.length() - 1)
														+ " data not found for key: " + nestedObjKey);
												continue;
											}

											JsonObject nestedObjJson = JsonParser.parseString(nestedObjJsonStr).getAsJsonObject();
											nestedObjJson.addProperty("id", nestedObjId); // Add nested object ID as
																							// "id" field
											fullNestedObjs.add(nestedObjJson);
										}
										objJson.remove(nestedField);
										objJson.add(nestedField, fullNestedObjs);
									}
								}

								fullObjs.add(objJson);
							}
							showJson.remove(field);
							showJson.add(field, fullObjs);
						}
					}

					fullShows.add(showJson);
				}
				rowJson.remove("shows");
				rowJson.add("contents", fullShows); // Replace "shows" with "content"
			}

			fullData.add(rowJson);
		}

		queueJson.remove("rows");
		queueJson.add("data", fullData); // Replace "rows" with "data"

		// Pretty print the JSON and write to file
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String finalJson = gson.toJson(queueJson);

		String fileName = "queue_" + id + ".json"; // Use only the ID part in the file name
		try (FileWriter writer = new FileWriter(fileName)) {
			writer.write(finalJson);
			System.out.println("JSON written to file: " + fileName);
		} catch (IOException e) {
			System.out.println("Error writing to file: " + e.getMessage());
		}

		jedis.close();
		// scanner.close();
	}
}