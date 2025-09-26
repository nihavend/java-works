package com.tabii.simulation.api;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

@RestController
@RequestMapping("/api/queues")
public class QueueController {

	private final ObjectMapper mapper = new ObjectMapper();
	private final JedisPool jedisPool;

	// Constructor ile pool oluşturuluyor
	public QueueController() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(50); // Maksimum toplam bağlantı
		poolConfig.setMinIdle(5); // Minimum idle bağlantı
		poolConfig.setMaxIdle(20); // Maksimum idle bağlantı
		poolConfig.setTestOnBorrow(true); // Bağlantı alınmadan önce test et
		poolConfig.setTestWhileIdle(true);// Boşta test et
		poolConfig.setBlockWhenExhausted(true); // Pool boşsa bekle

		this.jedisPool = new JedisPool(poolConfig, "localhost", 6379, 2000); // 2 saniye timeout
	}

	@GetMapping("/{queueId}")
	public String getQueue(@PathVariable("queueId") int queueId) throws Exception {
		int retries = 3; // Retry sayısı
		while (retries > 0) {
			long start =System.currentTimeMillis();
			// System.out.println("Başladı : " + System.currentTimeMillis());
			try (Jedis jedis = jedisPool.getResource()) {
				// Bağlantı aktif mi kontrol et
				if (!jedis.isConnected()) {
					jedis.connect();
				}

				// Örnek Redis GET komutu
				String queueJson = jedis.get("queue:" + queueId);
				if (queueJson == null) {
					return "{\"error\": \"Queue not found\"}";
				}

				// Parse queue JSON
				ObjectNode queueMap = (ObjectNode) mapper.readTree(queueJson);
				ArrayNode rowIds = (ArrayNode) queueMap.get("rows");

				// Replace row IDs with real row objects
				ArrayNode rowsArray = mapper.createArrayNode();

				List<Integer> ids = StreamSupport.stream(rowIds.spliterator(), false).map(JsonNode::asInt)
						.collect(Collectors.toList());

				for (Integer rowId : ids) {
					String rowJson = jedis.get("row:" + rowId);
					if (rowJson != null) {
						rowsArray.add(mapper.readTree(rowJson));
					}
				}

				queueMap.set("rows", rowsArray);
				
				System.out.println("Bitti : " + (System.currentTimeMillis() - start));

				return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(queueMap);

			} catch (JedisConnectionException e) {
				retries--;
				System.err.println("Redis bağlantı hatası, retry kalan: " + retries);
				try {
					Thread.sleep(500); // kısa bekleme
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
				}
			}
			
		}
		throw new RuntimeException("Redis bağlantısı kurulamadı!");
	}

	// Uygulama kapanırken pool'u kapat
	public void shutdown() {
		if (jedisPool != null && !jedisPool.isClosed()) {
			jedisPool.close();
		}
	}
}
