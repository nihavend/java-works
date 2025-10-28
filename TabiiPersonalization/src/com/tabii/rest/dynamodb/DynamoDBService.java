package com.tabii.rest.dynamodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@Repository
public class DynamoDBService {

	private final DynamoDbClient dynamoDbClient;

	public DynamoDBService(DynamoDbClient dynamoDb) {
		this.dynamoDbClient = dynamoDb;
	}

	public void save(String tableName, Item item) {
		Map<String, AttributeValue> itemValues = new HashMap<>();
		itemValues.put("id", AttributeValue.builder().s(item.getId()).build());
		itemValues.put("name", AttributeValue.builder().s(item.getName()).build());
		itemValues.put("description", AttributeValue.builder().s(item.getDescription()).build());

		dynamoDbClient.putItem(PutItemRequest.builder().tableName(tableName).item(itemValues).build());
	}

	public Item getById(String tableName, String id) {
	    Map<String, AttributeValue> key = Map.of(
		        "PK", AttributeValue.builder().s(id).build(),
		        "SK", AttributeValue.builder().s(id).build()
		    );
		
		GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder().tableName(tableName).key(key).build());
		if (response.hasItem()) {
			Map<String, AttributeValue> item = response.item();
			return new Item(item.get("PK").s(), item.get("SK").s(), item.get("data").s());
		}
		return null;
	}

	public void deleteById(String tableName, String id) {
		Map<String, AttributeValue> key = Map.of("id", AttributeValue.builder().s(id).build());
		dynamoDbClient.deleteItem(DeleteItemRequest.builder().tableName(tableName).key(key).build());
	}

	public List<Item> listAll(String tableName) {
		ScanResponse scanResponse = dynamoDbClient.scan(ScanRequest.builder().tableName(tableName).build());

		List<Item> items = new ArrayList<>();
		for (Map<String, AttributeValue> map : scanResponse.items()) {
			items.add(new Item(map.get("id").s(), map.get("name").s(), map.get("description").s()));
		}
		return items;
	}
	
	/**
     * Batch get for multiple keys — reduces network round trips drastically.
     *
     * @param tableName DynamoDB table
     * @param ids list of keys to fetch (e.g., ["image:1", "image:2", ...])
     * @return Map of id -> Item
     */
    public Map<String, Item> batchGet(String tableName, List<String> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();

        final int BATCH_LIMIT = 100; // DynamoDB BatchGetItem max per table

        Map<String, Item> resultMap = new HashMap<>();

        // Partition the list into chunks of 100
        for (int i = 0; i < ids.size(); i += BATCH_LIMIT) {
            List<String> batchIds = ids.subList(i, Math.min(i + BATCH_LIMIT, ids.size()));

            Map<String, KeysAndAttributes> requestItems = Map.of(
                tableName,
                KeysAndAttributes.builder()
                    .keys(batchIds.stream()
                        .map(id -> Map.of("pk", AttributeValue.fromS(id)))
                        .collect(Collectors.toList()))
                    .consistentRead(false)
                    .build()
            );

            BatchGetItemRequest batchRequest = BatchGetItemRequest.builder()
                    .requestItems(requestItems)
                    .build();

            try {
                BatchGetItemResponse response = dynamoDbClient.batchGetItem(batchRequest);

                List<Map<String, AttributeValue>> items = response.responses().get(tableName);
                if (items != null) {
                    for (Map<String, AttributeValue> itemMap : items) {
                        String pkValue = itemMap.get("pk").s();
                        resultMap.put(pkValue, new Item(itemMap));
                    }
                }

                // Handle unprocessed keys (retry once)
                Map<String, KeysAndAttributes> unprocessed = response.unprocessedKeys();
                if (!unprocessed.isEmpty()) {
                    retryUnprocessed(unprocessed, resultMap, tableName);
                }

            } catch (Exception e) {
                System.err.println("Batch get failed: " + e.getMessage());
            }
        }

        return resultMap;
    }
    
    /**
     * Retry handler for unprocessed keys
     */
    private void retryUnprocessed(Map<String, KeysAndAttributes> unprocessed,
                                  Map<String, Item> resultMap,
                                  String tableName) {
        if (unprocessed == null || unprocessed.isEmpty()) return;

        try {
            BatchGetItemRequest retryRequest = BatchGetItemRequest.builder()
                    .requestItems(unprocessed)
                    .build();

            BatchGetItemResponse retryResponse = dynamoDbClient.batchGetItem(retryRequest);
            List<Map<String, AttributeValue>> items = retryResponse.responses().get(tableName);
            if (items != null) {
                for (Map<String, AttributeValue> itemMap : items) {
                    String pkValue = itemMap.get("pk").s();
                    resultMap.put(pkValue, new Item(itemMap));
                }
            }
        } catch (Exception e) {
            System.err.println("Retry failed for unprocessed items: " + e.getMessage());
        }
    }
}
