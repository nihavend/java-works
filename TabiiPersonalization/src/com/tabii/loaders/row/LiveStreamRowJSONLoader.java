package com.tabii.loaders.row;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.data.model.LiveStreamRow;
import com.tabii.data.model.entities.GenreCard;
import com.tabii.data.model.entities.ShowCard;

// Main class to load and parse JSON from a file
public class LiveStreamRowJSONLoader {
	public static void main(String[] args) {
		// Path to the JSON file (adjust as needed)
		// String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/4.ls_row.json";
		String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/10.ls_row.json";
		
		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			// Read JSON file and map to ContentList object
			LiveStreamRow liveStreamRow = objectMapper.readValue(new File(filePath), LiveStreamRow.class);

			// Print the deserialized object
			System.out.println("Loaded ContentList: " + liveStreamRow);

			// Example: Accessing specific fields
			System.out.println("\nRow Type: " + liveStreamRow.getRowType());
			System.out.println("ID: " + liveStreamRow.getId());
			System.out.println("Number of Contents: " + liveStreamRow.getContents().size());
			System.out.println("\nContent Titles and Genres:");
			for (ShowCard content : liveStreamRow.getContents()) {
				System.out.println(" - " + content.getTitle() + " (" + content.getContentType() + ")");
				if (content.getGenres() != null) {
					for (GenreCard genre : content.getGenres()) {
						System.out.println("   * Genre: " + genre.getTitle());
					}
				}
			}

		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			e.printStackTrace();
		}
	}
}