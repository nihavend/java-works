package com.tabii.ref_impl.flink.model;

import java.io.IOException;

import com.tabii.ref_impl.flink.model.message.LogWrapper;
import org.apache.flink.api.common.serialization.AbstractDeserializationSchema;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;


public class TabiiLogSchema extends AbstractDeserializationSchema<LogWrapper> {

	private static final long serialVersionUID = 1L;

	private transient ObjectMapper objectMapper;

	/**
	 * For performance reasons it's better to create on ObjectMapper in this open
	 * method rather than creating a new ObjectMapper for every record.
	 */
	@Override
	public void open(InitializationContext context) {
		objectMapper = new ObjectMapper();
	}

	/**
	 * If our deserialize method needed access to the information in the Kafka
	 * headers of a KafkaConsumerRecord, we would have implemented a
	 * KafkaRecordDeserializationSchema instead of extending
	 * AbstractDeserializationSchema.
	 */
	@Override
	public LogWrapper deserialize(byte[] message) throws IOException {
		return objectMapper.readValue(message, LogWrapper.class);
	}
}