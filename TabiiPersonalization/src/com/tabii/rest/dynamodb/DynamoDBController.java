package com.tabii.rest.dynamodb;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/dynamodb")
public class DynamoDBController {

	private final DynamoDBService dynamoDBService;

	public DynamoDBController(DynamoDBService dynamoDBService) {
		this.dynamoDBService = dynamoDBService;
	}

	@PostMapping
	public Item create(@PathVariable String tableName, @RequestBody Item item) {
		if (item.getId() == null) {
			item.setId(UUID.randomUUID().toString());
		}
		dynamoDBService.save(tableName, item);
		return item;
	}

	@GetMapping("/{tableName}/{id}")
	public Item getById(@PathVariable String tableName, @PathVariable String id) {
		return dynamoDBService.getById(tableName, id);
	}

	@GetMapping("/{tableName}")
	public List<Item> listAll(@PathVariable String tableName) {
		return dynamoDBService.listAll(tableName);
	}

	@DeleteMapping("/{tableName}/{id}")
	public void delete(@PathVariable String tableName, @PathVariable String id) {
		dynamoDBService.deleteById(tableName, id);
	}

	@GetMapping("/queues/{queueId}")
	public String getQueue(@PathVariable int queueId) throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		// long start =System.currentTimeMillis();
		// System.out.println("Başladı : " + System.currentTimeMillis());
		Item item  = dynamoDBService.getById("queues", "queue:" + queueId);
		if (item == null) {
			throw new RuntimeException("Queue not found!");
		}
		ObjectNode resultJson = DynamoDBQueueToJson.parseQueueJson(dynamoDBService, mapper, item.getDescription());
		// System.out.println("Bitti : " + (System.currentTimeMillis() - start));

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultJson);

	}
}
