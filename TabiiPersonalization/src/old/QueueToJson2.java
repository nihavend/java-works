package old;

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

public class QueueToJson2 {
    @SuppressWarnings("resource")
    public static void main(String[] args) {
        // Scanner scanner = new Scanner(System.in);
        // System.out.print("Enter queue ID: ");
        String id = "1"; // scanner.nextLine().trim();
        
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

            // Get "shows" array and replace with "contents"
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
                                String objKey = field.equals("badges") ? "badges:" + objId : field.substring(0, field.length() - 1) + ":" + objId; // badges:<id>, genre:<id>, image:<id>
                                String objJsonStr = jedis.get(objKey);
                                if (objJsonStr == null) {
                                    System.out.println(field.substring(0, field.length() - 1) + " data not found for key: " + objKey);
                                    continue;
                                }

                                JsonObject rawObjJson = JsonParser.parseString(objJsonStr).getAsJsonObject();
                                JsonObject objJson = new JsonObject();

                                // Add ID field
                                objJson.addProperty("id", objId);

                                // Handle fields based on object type
                                if (field.equals("genres")) {
                                    // Genres: include contentType, title, and images
                                    objJson.addProperty("contentType", "genre");
                                    if (rawObjJson.has("title")) {
                                        objJson.add("title", rawObjJson.get("title"));
                                    }
                                } else if (field.equals("badges")) {
                                    // Badges: include bannerLocation, showLocation, title, type, and images
                                    objJson.addProperty("contentType", "badge");
                                    if (rawObjJson.has("bannerLocation")) {
                                        objJson.add("bannerLocation", rawObjJson.get("bannerLocation"));
                                    }
                                    if (rawObjJson.has("showLocation")) {
                                        objJson.add("showLocation", rawObjJson.get("showLocation"));
                                    }
                                    if (rawObjJson.has("title")) {
                                        objJson.add("title", rawObjJson.get("title"));
                                    }
                                    if (rawObjJson.has("type")) {
                                        objJson.add("type", rawObjJson.get("type"));
                                    }
                                    if (rawObjJson.has("images")) {
                                    	JsonArray imageIds = rawObjJson.getAsJsonArray("images");
                                    	if (imageIds != null) {
                                            JsonArray fullImages = new JsonArray();
                                            for (JsonElement imageIdElem : imageIds) {
                                                BigInteger imageId = imageIdElem.getAsBigInteger();
                                                String imageKey = "image:" + imageId;
                                                String imageJsonStr = jedis.get(imageKey);
                                                if (imageJsonStr == null) {
                                                    System.out.println("Image data not found for key: " + imageKey);
                                                    continue;
                                                }

                                                JsonObject rawImageJson = JsonParser.parseString(imageJsonStr).getAsJsonObject();
                                                JsonObject imageJson = new JsonObject();
                                                imageJson.addProperty("contentType", "image");
                                                if (rawImageJson.has("imageType")) {
                                                    imageJson.add("imageType", rawImageJson.get("imageType"));
                                                }
                                                if (rawImageJson.has("name")) {
                                                    imageJson.add("name", rawImageJson.get("name"));
                                                }
                                                if (rawImageJson.has("title")) {
                                                    imageJson.add("title", rawImageJson.get("title"));
                                                }

                                                fullImages.add(imageJson);
                                            }
                                            objJson.add("images", fullImages);
                                        } else {
                                            // Add empty images array if none exists
                                            objJson.add("images", new JsonArray());
                                        }
                                    }
                                    
                                } else if (field.equals("images")) {
                                    // Images: include contentType, imageType, name, and title
                                    objJson.addProperty("contentType", "image");
                                    if (rawObjJson.has("imageType")) {
                                        objJson.add("imageType", rawObjJson.get("imageType"));
                                    }
                                    if (rawObjJson.has("name")) {
                                        objJson.add("name", rawObjJson.get("name"));
                                    }
                                    if (rawObjJson.has("title")) {
                                        objJson.add("title", rawObjJson.get("title"));
                                    }
                                }

                                // Process images array for genres and badges
                                if (field.equals("genres") || field.equals("badges")) {
                                    JsonArray imageIds = rawObjJson.getAsJsonArray("images");
                                    if (imageIds != null) {
                                        JsonArray fullImages = new JsonArray();
                                        for (JsonElement imageIdElem : imageIds) {
                                            BigInteger imageId = imageIdElem.getAsBigInteger();
                                            String imageKey = "image:" + imageId;
                                            String imageJsonStr = jedis.get(imageKey);
                                            if (imageJsonStr == null) {
                                                System.out.println("Image data not found for key: " + imageKey);
                                                continue;
                                            }

                                            JsonObject rawImageJson = JsonParser.parseString(imageJsonStr).getAsJsonObject();
                                            JsonObject imageJson = new JsonObject();
                                            imageJson.addProperty("contentType", "image");
                                            if (rawImageJson.has("imageType")) {
                                                imageJson.add("imageType", rawImageJson.get("imageType"));
                                            }
                                            if (rawImageJson.has("name")) {
                                                imageJson.add("name", rawImageJson.get("name"));
                                            }
                                            if (rawImageJson.has("title")) {
                                                imageJson.add("title", rawImageJson.get("title"));
                                            }

                                            fullImages.add(imageJson);
                                        }
                                        objJson.add("images", fullImages);
                                    } else {
                                        // Add empty images array if none exists
                                        objJson.add("images", new JsonArray());
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
                rowJson.add("contents", fullShows); // Replace "shows" with "contents"
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