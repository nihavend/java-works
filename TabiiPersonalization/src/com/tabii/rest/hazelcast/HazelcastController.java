package com.tabii.rest.hazelcast;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabii.HzcQueueToJson;
import com.tabii.utils.HcMaps;

@RestController
@RequestMapping("/hazelcast")
public class HazelcastController {

	private final HazelcastService hazelcastService;

	public HazelcastController(HazelcastService hazelcastService) {
		this.hazelcastService = hazelcastService;
	}

	@PostMapping("/{key}")
	public String put(@PathVariable String key, @RequestBody String value) {
		hazelcastService.putValue(key, value);
		return "Stored " + key;
	}

	@GetMapping("/{key}")
	public String get(@PathVariable String key) {
		return hazelcastService.getValue(key);
	}

	@GetMapping("/queues/{queueId}")
	public String getQueue(@PathVariable int queueId) throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		// long start =System.currentTimeMillis();
		// System.out.println("Başladı : " + System.currentTimeMillis());
		String queueJson = hazelcastService.getValue(HcMaps.QUEUES.getMapName(), "queue:" + queueId);
		if (queueJson == null) {
			throw new RuntimeException("Queue not found!");
		}
		ObjectNode resultJson = HzcQueueToJson.parseQueueJson(hazelcastService, mapper, queueJson);
		// System.out.println("Bitti : " + (System.currentTimeMillis() - start));

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultJson);

	}
}
