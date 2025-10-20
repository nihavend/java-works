package com.tabii.rest.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.stereotype.Service;

@Service
public class HazelcastService {

    private final HazelcastInstance hazelcastInstance;

    public HazelcastService(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public void putValue(String key, String value) {
        IMap<String, String> map = hazelcastInstance.getMap("myMap");
        map.put(key, value);
    }

    public String getValue(String key) {
        IMap<String, String> map = hazelcastInstance.getMap("myMap");
        return map.get(key);
    }
}
