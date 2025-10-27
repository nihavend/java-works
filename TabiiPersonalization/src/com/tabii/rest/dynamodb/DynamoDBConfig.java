package com.tabii.rest.dynamodb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Duration;

@Configuration
public class DynamoDBConfig {

	@Bean
    DynamoDbClient dynamoDbClient() {
        // Configure connection pooling and timeouts
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder()
                .maxConnections(100) // Default is 50
                .connectionTimeout(Duration.ofSeconds(10))
                .socketTimeout(Duration.ofSeconds(20))
                .connectionAcquisitionTimeout(Duration.ofSeconds(5));

        return DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:8000")) // DynamoDB local
                .region(Region.US_EAST_1)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("dummy", "dummy")
                        )
                )
                .httpClientBuilder(httpClientBuilder)
                .build();
    }
}
