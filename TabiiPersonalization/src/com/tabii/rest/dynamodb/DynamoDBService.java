package com.tabii.rest.dynamodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@Repository
public class DynamoDBService {

	private final DynamoDbClient dynamoDb;

	public DynamoDBService(DynamoDbClient dynamoDb) {
		this.dynamoDb = dynamoDb;
	}

	public void save(String tableName, Item item) {
		Map<String, AttributeValue> itemValues = new HashMap<>();
		itemValues.put("id", AttributeValue.builder().s(item.getId()).build());
		itemValues.put("name", AttributeValue.builder().s(item.getName()).build());
		itemValues.put("description", AttributeValue.builder().s(item.getDescription()).build());

		dynamoDb.putItem(PutItemRequest.builder().tableName(tableName).item(itemValues).build());
	}

	public Item getById(String tableName, String id) {
		Map<String, AttributeValue> key = Map.of("id", AttributeValue.builder().s(id).build());
		GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder().tableName(tableName).key(key).build());
		if (response.hasItem()) {
			Map<String, AttributeValue> item = response.item();
			return new Item(item.get("id").s(), item.get("name").s(), item.get("description").s());
		}
		return null;
	}

	public void deleteById(String tableName, String id) {
		Map<String, AttributeValue> key = Map.of("id", AttributeValue.builder().s(id).build());
		dynamoDb.deleteItem(DeleteItemRequest.builder().tableName(tableName).key(key).build());
	}

	public List<Item> listAll(String tableName) {
		ScanResponse scanResponse = dynamoDb.scan(ScanRequest.builder().tableName(tableName).build());

		List<Item> items = new ArrayList<>();
		for (Map<String, AttributeValue> map : scanResponse.items()) {
			items.add(new Item(map.get("id").s(), map.get("name").s(), map.get("description").s()));
		}
		return items;
	}
}
