package com.tabii.memcachedclient;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabii.QueueToJson;

@RestController
@RequestMapping("/cache")
public class MemcachedController {

    private final MemcachedService memcachedService;

    public MemcachedController(MemcachedService memcachedService) {
        this.memcachedService = memcachedService;
    }

    @PostMapping("/set")
    public String set(@RequestParam String key, @RequestParam String value) {
        memcachedService.setValue(key, value);
        return "Value set successfully for key: " + key;
    }

    @GetMapping("/get")
    public Object get(@RequestParam String key) {
        return memcachedService.getValue(key);
    }

    @GetMapping("/bulk")
    public Map<String, Object> getBulk(@RequestParam List<String> keys) {
        return memcachedService.getBulkValues(keys);
    }

    @DeleteMapping("/delete")
    public String delete(@RequestParam String key) {
        boolean result = memcachedService.deleteValue(key);
        return result ? "Deleted successfully." : "Delete failed.";
    }
    
    @GetMapping("/queues/{queueId}")
	public String getQueue(@PathVariable int queueId) throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		// long start =System.currentTimeMillis();
		// System.out.println("Başladı : " + System.currentTimeMillis());
		String queueJson = memcachedService.getValue("queue:" + queueId).toString();
		if (queueJson == null) {
			throw new RuntimeException("Queue not found!");
		}
		ObjectNode resultJson = QueueToJson.parseQueueJson(memcachedService, mapper, queueJson);
		// System.out.println("Bitti : " + (System.currentTimeMillis() - start));

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultJson);

	}
}
