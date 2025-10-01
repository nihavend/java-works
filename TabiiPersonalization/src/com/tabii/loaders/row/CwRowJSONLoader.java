package com.tabii.loaders.row;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.data.model.json.CwRow;
import com.tabii.data.model.json.entities.GenreCard;
import com.tabii.data.model.json.entities.ShowCard;

// Main class to load and parse JSON from a file
public class CwRowJSONLoader {
	public static void main(String[] args) {
		// Path to the JSON file (adjust as needed)
		String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/3.cwRow.json";

		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			// Read JSON file and map to ContentList object
			CwRow cwRow = objectMapper.readValue(new File(filePath), CwRow.class);

			// Print the deserialized object
			System.out.println("Loaded ContentList: " + cwRow);

			// Example: Accessing specific fields
			System.out.println("\nRow Type: " + cwRow.getRowType());
			System.out.println("ID: " + cwRow.getId());
			System.out.println("Number of Contents: " + cwRow.getContents().size());
			System.out.println("\nContent Titles and Genres:");
			for (ShowCard content : cwRow.getContents()) {
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