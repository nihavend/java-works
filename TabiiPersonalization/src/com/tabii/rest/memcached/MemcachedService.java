package com.tabii.rest.memcached;

import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

@Service
public class MemcachedService {

    private final MemcachedClient memcachedClient;

    @Value("${memcached.expire.seconds:900}")
    private int defaultExpireSeconds;

    public MemcachedService(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    // --- Basic operations ---

    public void setValue(String key, Object value) {
        memcachedClient.set(key, defaultExpireSeconds, value);
    }

    public Object getValue(String key) {
        return memcachedClient.get(key);
    }

    public boolean deleteValue(String key) {
        Future<Boolean> future = memcachedClient.delete(key);
        try {
            return future.get();
        } catch (Exception e) {
            return false;
        }
    }

    // --- Bulk operations ---

    public Map<String, Object> getBulkValues(Collection<String> keys) {
        return memcachedClient.getBulk(keys);
    }
}
