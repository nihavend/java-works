package com.tabii.restclient.responsemodels;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TabiiJsonParser {
	
	public static ProfileListResponse parseProfileListResponse(String jsonString) {

		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		ProfileListResponse profileListResponse = null;
		try {
			// Read JSON file and map to ContentList object
			profileListResponse = objectMapper.readValue(jsonString, ProfileListResponse.class);
		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			System.out.println("Client Response JSON " + jsonString);
			e.printStackTrace();
		}
		
		return profileListResponse;
	}
	

	public static LoginResponse parseLoginResponse(String jsonString) {

		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();
		LoginResponse loginResponse = null;
		try {
			// Read JSON file and map to ContentList object
			loginResponse = objectMapper.readValue(jsonString, LoginResponse.class);
		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			System.out.println("Client Response JSON " + jsonString);
			e.printStackTrace();
		}
		
		return loginResponse;
	}
}
