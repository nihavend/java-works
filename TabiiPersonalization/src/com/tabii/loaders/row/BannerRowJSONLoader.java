package com.tabii.loaders.row;

import java.io.File;
import java.io.IOException;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.data.model.BannerRow;
import com.tabii.data.model.entities.GenreCard;
import com.tabii.data.model.entities.ShowCard;

// Main class to load and parse JSON from a file
public class BannerRowJSONLoader {
	public static void main(String[] args) {
		// Path to the JSON file (adjust as needed)
		String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/banner_row.json";

		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			// Read JSON file and map to ContentList object
			BannerRow bannerRow = objectMapper.readValue(new File(filePath), BannerRow.class);

			// Print the deserialized object
			System.out.println("Loaded ContentList: " + bannerRow);

			// Example: Accessing specific fields
			System.out.println("\nRow Type: " + bannerRow.getRowType());
			System.out.println("ID: " + bannerRow.getId());
			System.out.println("Number of Contents: " + bannerRow.getContents().size());
			System.out.println("\nContent Titles and Genres:");
			for (ShowCard content : bannerRow.getContents()) {
				System.out.println(" - " + content.getTitle() + " (" + content.getContentType() + ")");
				for (GenreCard genre : content.getGenres()) {
					System.out.println("   * Genre: " + genre.getTitle());
				}
			}

		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			e.printStackTrace();
		}
	}
}