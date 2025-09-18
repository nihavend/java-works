package com.tabii.loaders.row;

import java.io.IOException;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tabii.data.model.Queue;
import com.tabii.data.model.Row;

// Main class to load and parse JSON from a file
public class DynamicQueueJSONLoader {
	public static void main(String[] args) {
		// Path to the JSON file (adjust as needed)
		String filePath = "/Users/serkantas/dev/trt-works/tabii/p13n/jsons/home_page.json";

		try {
			// Create ObjectMapper instance
			ObjectMapper mapper = new ObjectMapper();

			SimpleModule module = new SimpleModule();
			module.addDeserializer(Row.class, new RowDeserializer());
			mapper.registerModule(module);

			Queue queue = mapper.readValue(Paths.get(filePath).toFile(), Queue.class);

			for (Row row : queue.getData()) {
				System.out.println(row.getClass().getSimpleName() + " with id=" + row.getId());
			}

		} catch (IOException e) {
			System.err.println("Error reading JSON file: " + e.getMessage());
			e.printStackTrace();
		}
	}
}