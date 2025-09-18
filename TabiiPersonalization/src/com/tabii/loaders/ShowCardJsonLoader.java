package com.tabii.loaders;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.data.model.entities.GenreCard;
import com.tabii.data.model.entities.ShowCard;

// Main class to load and parse JSON from a file
public class ShowCardJsonLoader {
	public static void main(String[] args) {
		// Path to the JSON file (adjust as needed)
		String filePath = "src/main/resources/series.json";

		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			// Read JSON file and map to Series object
			ShowCard showCard = objectMapper.readValue(new File(filePath), ShowCard.class);

			// Print the deserialized object
			System.out.println("Loaded Series: " + showCard);

			// Example: Accessing specific fields
			System.out.println("\nTitle: " + showCard.getTitle());
			System.out.println("Content Type: " + showCard.getContentType());
			System.out.println("Genres: ");
			for (GenreCard genre : showCard.getGenres()) {
				System.out.println(" - " + genre.getTitle());
			}
			System.out.println("Number of Images: " + showCard.getImages().size());

		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			e.printStackTrace();
		}
	}
}