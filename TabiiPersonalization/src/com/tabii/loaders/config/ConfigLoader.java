package com.tabii.loaders.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigLoader {
	public static void main(String[] args) {
		getMenuId();
	}

	public static int getMenuId() {
		
		Config config = null;
		
		try {

			// JSON dosyasından oku
			// ObjectMapper mapper = new ObjectMapper();
			// Config config = mapper.readValue(Paths.get("config.json").toFile(),
			// Config.class);

			String urlString = "https://autodiscover-cdn.tabii.com/default?environment=prod&platform=web";
			HttpURLConnection conn = createConnection(urlString, "GET");

			// Read response
			int status = conn.getResponseCode();
			InputStream responseStream = (status >= 200 && status < 300) ? conn.getInputStream()
					: conn.getErrorStream();

			BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, "utf-8"));
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				response.append(line).append("\n");
			}
			in.close();

			config = parseConfigResponse(response.toString());

			// Örnek kullanım
			System.out.println("Base Path: " + config.getBasePath());
			System.out.println("Ad URL: " + config.getAds_url());
			System.out.println("Region: " + config.getRegion());
			System.out.println("Cookie Essential Required: " + config.getCookies().getEssential_cookies().isRequired());

			System.out.println("Menu Id : " + config.getMenu_id());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return config.getMenu_id();
	}

	public static Config parseConfigResponse(String jsonString) {

		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		Config profileListResponse = null;
		try {
			// Read JSON file and map to ContentList object
			profileListResponse = objectMapper.readValue(jsonString, Config.class);
		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			System.out.println("Client Response JSON " + jsonString);
			e.printStackTrace();
		}

		return profileListResponse;
	}

	private static HttpURLConnection createConnection(String urlString, String methodType) {
		try {

			// Create connection
			URL url = new URI(urlString).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(methodType);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);

			return conn;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
