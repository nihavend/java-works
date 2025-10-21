package com.tabii.rest.hazelcast;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

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
    
    public void putValue(String mapName, String key, String value) {
        IMap<String, String> map = hazelcastInstance.getMap(mapName);
        map.put(key, value);
    }

    public String getValue(String mapName, String key) {
        IMap<String, String> map = hazelcastInstance.getMap(mapName);
        return map.get(key);
    }
    
    public Map<String, Object> getAllValues(String mapName, Set<String> keys) {
        IMap<String, Object> map = hazelcastInstance.getMap(mapName);
        return map.getAll(keys);
    }

}
