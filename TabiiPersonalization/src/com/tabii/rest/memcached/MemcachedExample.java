package com.tabii.rest.memcached;
import net.spy.memcached.MemcachedClient;
import java.net.InetSocketAddress;

public class MemcachedExample {
    public static void main(String[] args) {
        try {
            // Connect to Memcached server on localhost:11211
            MemcachedClient memcachedClient = new MemcachedClient(
                    new InetSocketAddress("127.0.0.1", 11211));

            System.out.println("Connected to Memcached!");

            // Set a key-value pair (expires in 900 seconds = 15 min)
            memcachedClient.set("user:1", 900, "John Doe");
            System.out.println("Value set successfully.");

            // Get the value by key
            Object value = memcachedClient.get("user:1");
            System.out.println("Fetched from Memcached: " + value);

            // Delete the key
            memcachedClient.delete("user:1");
            System.out.println("Key deleted.");

            // Shutdown the client
            memcachedClient.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
