package com.tabii.restclient;

import static java.util.Map.entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import com.google.gson.*;
import com.tabii.data.model.Queue;
import com.tabii.data.model.entities.ProfileCard;
import com.tabii.loaders.config.ConfigLoader;
import com.tabii.loaders.menu.MenuData;
import com.tabii.loaders.menu.MenuItem;
import com.tabii.loaders.menu.MenuLoader;
import com.tabii.restclient.responsemodels.LoginResponse;
import com.tabii.restclient.responsemodels.ProfileListResponse;
import com.tabii.restclient.responsemodels.TabiiJsonParser;

public class TabiiClient {

	public static void main(String[] args) {

		try {
			
			if(args.length < 2 || args[0] == null || args[1] == null) {
				System.out.println("Usaage : username password");
				System.exit(-1);
			}
			
			String userName = args[0];
			String password = args[1];
					
			
			// 1. adım
			LoginResponse loginResponse = login(userName, password);
			System.out.println("AccessToken : " + loginResponse.getAccessToken());
			System.out.println("RefreshToken : " + loginResponse.getRefreshToken());
			System.out.println("Comparison of refresh and access token : "
					+ loginResponse.getAccessToken().compareTo(loginResponse.getRefreshToken()));

			// 2. adım
			ProfileListResponse profileListResponse = getProfileList(loginResponse.getAccessToken());
			System.out.println("count of profiles : " + profileListResponse.getCount());
			String subjectProfile = null;
			for (ProfileCard profileCard : profileListResponse.getData()) {
				if (profileCard.getName().toLowerCase(Locale.ENGLISH).compareTo("misafir") == 0) {
					subjectProfile = profileCard.getSK();
				}
				System.out.println(profileCard.getName());
				System.out.println(profileCard.getSK());
			}

			// 3. adım
			LoginResponse profileTokenResponse = getProfileToken(subjectProfile, loginResponse.getAccessToken(),
					loginResponse.getRefreshToken());
			System.out.println("AccessToken : " + profileTokenResponse.getAccessToken());
			System.out.println("RefreshToken : " + profileTokenResponse.getRefreshToken());
			System.out.println("Comparison of PROFILE refresh and access token : "
					+ loginResponse.getAccessToken().compareTo(loginResponse.getRefreshToken()));

			// 4. adım
			int menuId = ConfigLoader.getMenuId();
			System.out.println("Menu Item id : " + menuId);

			// 5. adım
			MenuData getMenuData = MenuLoader.getMenuData(profileTokenResponse.getAccessToken(), "" + menuId);

			String targetId = null;
			for (MenuItem menuItem : getMenuData.getData()) {
				if (menuItem.getTitle().compareTo("Home Page") == 0) {
					targetId = menuItem.getTargetId();
					System.out.println("Target id for home page : " + targetId);
					break;
				}
			}
			
			// 6. adım
			Queue queue = getQueue(targetId, profileTokenResponse.getAccessToken());
			System.out.println("Row count of queue : " + queue.getData().size());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static Queue getQueue(String targetId, String accessToken) throws IOException {

		String reqUrl = "https://eu1.tabii.com/apigateway/catalog/v1/pages/queues/" + targetId + "?targetContentTypes=movie,series&targetCategoryIds=";


		Map<String, String> headerMap = Map.ofEntries(entry("Authorization", "Bearer " + accessToken));

		String response = restResponse(null, reqUrl, headerMap, "GET");

		System.out.println("Queue json : " + prettyPrint(response));
		
		Queue queue = TabiiJsonParser.parseQueueResponse(response.toString());

		return queue;
	}

	private static LoginResponse getProfileToken(String profileSK, String accessToken, String refreshToken)
			throws IOException {

		String reqUrl = "https://eu1.tabii.com/apigateway/profiles/v2/" + profileSK + "/token";

		String requestBody = "{ \"refreshToken\": \"" + refreshToken + "\" }";

		Map<String, String> headerMap = Map.ofEntries(entry("Authorization", "Bearer " + accessToken),
				entry("Device-Id", ""));

		String response = restResponse(requestBody, reqUrl, headerMap, "POST");

		LoginResponse loginResponse = TabiiJsonParser.parseLoginResponse(response.toString());

		return loginResponse;
	}

	private static ProfileListResponse getProfileList(String accessToken) throws IOException {

		String reqUrl = "https://eu1.tabii.com/apigateway/profiles/v2/";

		Map<String, String> headerMap = Map.ofEntries(entry("Authorization", "Bearer " + accessToken));

		String response = restResponse(null, reqUrl, headerMap, "GET");

		ProfileListResponse profileListResponse = TabiiJsonParser.parseProfileListResponse(response);

		return profileListResponse;

	}

	private static LoginResponse login(String userName, String password) throws IOException {
		
		String reqUrl = "https://eu1.tabii.com/apigateway/auth/v2/login";
		
		String requestBody = "{ \"email\": \"" + userName + "\", \"password\": \"" + password + "\" }";
		
		String response = restResponse(requestBody, reqUrl, null, "POST");
		
		LoginResponse loginResponse = TabiiJsonParser.parseLoginResponse(response.toString());
		
		return loginResponse;
	}

	public static String restResponse(String requestBody, String urlString, Map<String, String> headerMap,
			String methodType) throws IOException {

		HttpURLConnection conn = createConnection(urlString, methodType);

		if (headerMap != null) {
			for (var entry : headerMap.entrySet()) {
				conn.setRequestProperty(entry.getKey(), entry.getValue());
			}
		}

		if (requestBody != null) {
			// Send request body
			try (OutputStream os = conn.getOutputStream()) {
				byte[] input = requestBody.getBytes("utf-8");
				os.write(input, 0, input.length);
			}
		}

		// Read response
		int status = conn.getResponseCode();
		InputStream responseStream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

		BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, "utf-8"));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = in.readLine()) != null) {
			response.append(line).append("\n");
		}
		in.close();

		return response.toString();

	}

	private static LoginResponse token(String urlString) throws IOException {

		// Example login body
		String requestBody = "{ \"email\": \"a.b@c.com\", \"password\": \"password\" }";

		HttpURLConnection conn = createConnection(urlString, "POST");

		// Send request body
		try (OutputStream os = conn.getOutputStream()) {
			byte[] input = requestBody.getBytes("utf-8");
			os.write(input, 0, input.length);
		}

		// Read response
		int status = conn.getResponseCode();
		InputStream responseStream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

		BufferedReader in = new BufferedReader(new InputStreamReader(responseStream, "utf-8"));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = in.readLine()) != null) {
			response.append(line).append("\n");
		}
		in.close();

		LoginResponse loginResponse = TabiiJsonParser.parseLoginResponse(response.toString());

		// System.out.println("AccessToken : " + loginResponse.getAccessToken());

		/*
		 * 
		 * // ---- Parse JSON ---- JsonObject json =
		 * JsonParser.parseString(response.toString()).getAsJsonObject();
		 * 
		 * String accessToken = json.get("accessToken").getAsString(); int expiresIn =
		 * json.get("expiresIn").getAsInt(); int refreshExpiresIn =
		 * json.get("refreshExpiresIn").getAsInt(); String refreshToken =
		 * json.get("refreshToken").getAsString(); String sessionState =
		 * json.get("sessionState").getAsString(); String tokenType =
		 * json.get("tokenType").getAsString();
		 * 
		 * System.out.println("Access Token: " + accessToken);
		 * System.out.println("Expires In: " + expiresIn);
		 * System.out.println("Refresh Expires In: " + refreshExpiresIn);
		 * System.out.println("Refresh Token: " + refreshToken);
		 * System.out.println("Session State: " + sessionState);
		 * System.out.println("Token Type: " + tokenType);
		 */

		/*
		 * 
		 * 
		 * String outputFile =
		 * "/Users/serkantas/dev/eclipse-workspace/TabiiPersonalization/auth_response.json";
		 * // ---- Save original JSON to file ---- try (FileWriter writer = new
		 * FileWriter(outputFile)) { writer.write(response.toString()); }
		 * 
		 * System.out.println("Response saved to " + outputFile);
		 */

		conn.disconnect();

		return loginResponse;
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
	
	public static String prettyPrint(String uglyJsonString) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement je = JsonParser.parseString(uglyJsonString);
		String prettyJsonString = gson.toJson(je);
		return prettyJsonString;
	}
}
