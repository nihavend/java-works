package com.tabii.rest.redis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tabii.RedisQueueToJson;

@RestController
@RequestMapping("/api/queues")
public class QueueController {

	private final RedisService redisService;

	public QueueController(RedisService redisService) {
		this.redisService = redisService;
	}

	@GetMapping("/{queueId}")
	public String getQueue(@PathVariable("queueId") int queueId) throws Exception {

		ObjectMapper mapper = new ObjectMapper();

		// long start =System.currentTimeMillis();
		// System.out.println("Başladı : " + System.currentTimeMillis());
		String queueJson = redisService.getValue("queue:" + queueId);
		if (queueJson == null) {
			throw new RuntimeException("Queue not found!");
		}
		ObjectNode resultJson = RedisQueueToJson.parseQueueJson(redisService, mapper, queueJson);
		// System.out.println("Bitti : " + (System.currentTimeMillis() - start));

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultJson);

	}

}
