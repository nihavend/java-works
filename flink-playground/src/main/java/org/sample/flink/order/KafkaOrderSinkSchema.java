package org.sample.flink.order;

import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaOrderSinkSchema implements KafkaRecordSerializationSchema<OrderAgg> {

	private static final long serialVersionUID = 1L;

	private String topic;
	private static final ObjectMapper objectMapper = JsonMapper.builder().build().registerModule(new JavaTimeModule())
			.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

	public KafkaOrderSinkSchema(String topic) {
		this.topic = topic;
	}

	@Override
	public ProducerRecord<byte[], byte[]> serialize(OrderAgg order, KafkaSinkContext context, Long timestamp) {
		try {
			return new ProducerRecord<>(topic, null, // choosing not to specify the partition
					order.eventTime, order.userId.getBytes(), objectMapper.writeValueAsBytes(order));
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Could not serialize record: " + order, e);
		}

	}

}