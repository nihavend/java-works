package com.tabii.loaders.row;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabii.data.model.json.GenreRow;
import com.tabii.data.model.json.entities.GenreCard;
import com.tabii.data.model.json.entities.ShowCard;

// Main class to load and parse JSON from a file
public class GenreRowJSONLoader {
	public static void main(String[] args) {
		// Path to the JSON file (adjust as needed)
		String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/16.genre_row.json";
		
		// Create ObjectMapper instance
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			// Read JSON file and map to ContentList object
			GenreRow genreRow = objectMapper.readValue(new File(filePath), GenreRow.class);

			// Print the deserialized object
			System.out.println("Loaded ContentList: " + genreRow);

			// Example: Accessing specific fields
			System.out.println("\nRow Type: " + genreRow.getRowType());
			System.out.println("ID: " + genreRow.getId());
			System.out.println("Number of genreRow: " + genreRow.getContents().size());
			System.out.println("\nContent Titles and Genres:");
			for (ShowCard content : genreRow.getContents()) {
				System.out.println(" - " + content.getTitle() + " (" + content.getContentType() + ")");
				if(content.getGenres() != null) {
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