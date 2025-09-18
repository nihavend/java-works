package com.tabii.loaders.menu;

import static java.util.Map.entry;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.restclient.TabiiClient;

public class MenuLoader {

	public static void main(String[] args) throws IOException {

		String configMenuId = "135075";
		getMenuData(null, configMenuId);
		
	}
	
	public static MenuData getMenuData(String accessToken, String configMenuId) throws IOException {
		
		// ObjectMapper mapper = new ObjectMapper();
		// Bilinmeyen alanlara takılma
		// mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		// 1) JSON string olarak parse (örnek)
		// String json = "{ /* buraya verdiğiniz JSON'u yapıştırabilirsiniz */ }";
		// Eğer gerçek kullanımda dosyadan okumak isterseniz: mapper.readValue(new
		// File("menu.json"), MenuData.class);

		// Örnek: dosyadan okumak için (yorum satırını kaldırın)
		// MenuData menu = mapper.readValue(new File("menu.json"), MenuData.class);

		// Örnek: doğrudan string'den okuma
		// MenuData menu = mapper.readValue(json, MenuData.class);

		
		String urlString = "https://eu1.tabii.com/apigateway/catalog/v1/menus/" + configMenuId;
//		HttpURLConnection conn = createConnection(urlString, "GET");
//
//		// Read response
//		int status = conn.getResponseCode();
//		InputStream responseStream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
//
//		BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, "utf-8"));
//		StringBuilder response = new StringBuilder();
//		String line;
//		while ((line = in.readLine()) != null) {
//			response.append(line).append("\n");
//		}
//		in.close();
		
		Map<String, String> headerMap = Map.ofEntries(entry("Authorization", "Bearer " + accessToken));
		String response = TabiiClient.restResponse(null, urlString, headerMap, "GET");

		MenuData menuData = parseMenuResponse(response);

		// Basit çıktı
		for (MenuItem item : menuData.getData()) {
			System.out.printf("Title: %s, targetId: %s, default: %b, viewType: %s, universalUrl: %s%n", item.getTitle(),
					item.getTargetId(), item.isDefault(), item.getViewType(), item.getUniversalUrl());

			if (item.getImages() != null) {
				for (MenuImage img : item.getImages()) {
					System.out.printf("  - image: %s (%s) name=%s title=%s%n", img.getImageType(), img.getContentType(),
							img.getName(), img.getTitle());
				}
			}

			System.out.println("  targetContentTypes: " + item.getTargetContentTypes());
			System.out.println();
		}
		
		return menuData;
		
	}

	public static MenuData parseMenuResponse(String jsonString) {

		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		MenuData menuData = null;
		try {
			// Read JSON file and map to ContentList object
			menuData = objectMapper.readValue(jsonString, MenuData.class);
		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			System.out.println("Client Response JSON " + jsonString);
			e.printStackTrace();
		}

		return menuData;
	}
}