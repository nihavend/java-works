package com.tabii.flink;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class ReadFromKafkaTopic {

	public static void main(String[] args) throws Exception {
		
		
		StreamExecutionEnvironment see = StreamExecutionEnvironment.getExecutionEnvironment();
		// see.setRuntimeMode(RuntimeExecutionMode.AUTOMATIC);
		
		// LocalStreamEnvironment see = StreamExecutionEnvironment.createLocalEnvironment();
		
		
		KafkaSource<String> source = KafkaSource.<String>builder()
				.setBootstrapServers("127.0.0.1:9092")
				.setTopics("test-flink-stream")
				.setGroupId("flink_streamer")
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new SimpleStringSchema()).build();
		
		
		DataStream<String> messageStream = see.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
		
		
		messageStream.rebalance().map(new MapFunction<String, String>() {
			private static final long serialVersionUID = -6867736771747690202L;

			@Override
			public String map(String value) throws Exception {
				return "Kafka and Flink says: " + value;
			}
		}).print();

		see.execute();
	}

}