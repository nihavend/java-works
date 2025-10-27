package com.tabii.utils;

public class DynamoDBProperties {

	String uri;
	String accessKey;
	String secretKey;
	
	DynamoDBProperties(String uri, String accessKey, String secretKey) {
		this.uri = uri;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
	}
	
	public String getUri() {
		return uri;
	}
	
	public String getAccessKey() {
		return accessKey;
	}
	
	public String getSecretKey() {
		return secretKey;
	}

}
