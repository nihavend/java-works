package com.tabii.rest.dynamodb;

import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class Item {

	private String id;
	private String name;
	private String description;
	
	private final Map<String, AttributeValue> attributes;

    public Item(Map<String, AttributeValue> attributes) {
        this.attributes = attributes;
    }
	
	public Item(String id, String name, String description) {
		this.attributes = null;
		this.id = id;
		this.name = name;
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
    public Map<String, AttributeValue> getAttributes() {
		return attributes;
	}



}
