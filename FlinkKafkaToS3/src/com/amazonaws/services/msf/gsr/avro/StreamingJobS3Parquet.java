package com.amazonaws.services.msf.gsr.avro;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.AvroParquetWriters;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tabii.flink.KafkaToS3Parquet;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * Sample message {"symbol": "\"TBV\"", "count": 758} {"symbol": "\"AMZN\"",
 * "count": 749}{"symbol": "\"AAPL\"","count": 777}
 * 
 */

public class StreamingJobS3Parquet {

	private static final Logger LOG = LoggerFactory.getLogger(StreamingJobS3Parquet.class);

	private static String s3Bucket = "s3a://int-avatar-test/flink-parquet/";

	private static FileSink<TradeCount> S3Sink(String s3SinkPath) {
		return FileSink.forBulkFormat(new Path(s3SinkPath), AvroParquetWriters.forSpecificRecord(TradeCount.class))
				.withBucketAssigner(new DateTimeBucketAssigner<>("'year='yyyy'/month='MM'/day='dd'/hour='HH/"))
				.withOutputFileConfig(OutputFileConfig.builder().withPartSuffix(".parquet").build()).build();
	}

	public static void init() {

		// Get the Logback LoggerContext
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		// Optionally, set log level for Flink packages
		// Set log level programmatically for your class
		ch.qos.logback.classic.Logger appLogger = loggerContext.getLogger(KafkaToS3Parquet.class);
		appLogger.setLevel(Level.DEBUG); // Change to DEBUG, INFO, WARN, ERROR, etc.

		// Optionally, set log level for Flink packages
		ch.qos.logback.classic.Logger flinkLogger = loggerContext.getLogger("org.apache.flink");
		flinkLogger.setLevel(Level.WARN); // Reduce Flink noise

		// Optionally, set log level for Flink packages
		ch.qos.logback.classic.Logger kafkaLogger = loggerContext.getLogger("org.apache.kafka.clients");
		kafkaLogger.setLevel(Level.ERROR); // Reduce Flink noise

		// Optionally, set log level for Hadoop packages
		ch.qos.logback.classic.Logger hadoopLogger = loggerContext.getLogger("org.apache.hadoop");
		hadoopLogger.setLevel(Level.ERROR); // Reduce Flink noise

		// Test the logging levels
		LOG.debug("Debug: Starting Flink job configuration");
		LOG.info("Info: Starting Flink job in local environment");
		LOG.warn("Warn: This is a warning message");
		LOG.error("Error: This is an error message (just for testing)");
	}

	public static void main(String[] args) throws Exception {

		init();

		// set up the streaming execution environment
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		env.setRuntimeMode(RuntimeExecutionMode.STREAMING);
		env.enableCheckpointing(60000);
		env.setParallelism(1);
		
		
		KafkaSource<String> source = KafkaSource.<String>builder().setBootstrapServers("127.0.0.1:9092")
				.setTopics("test-flink-stream").setGroupId("flink_streamer")
				.setStartingOffsets(OffsetsInitializer.latest()).setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		// Sink
		// FileSink<TradeCount> sink = S3Sink(s3Bucket);

		// Define a data stream from Kafka
		DataStream<String> messageStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

		// Mapper to be used for parsing String to JSON
		ObjectMapper jsonParser = new ObjectMapper();

		SingleOutputStreamOperator<TradeCount> dss = messageStream.map(value -> { // Parse the JSON
			JsonNode jsonNode = jsonParser.readValue(value, JsonNode.class);
			return new Tuple2<>(jsonNode.get("symbol").toString(), 1);
		}).returns(Types.TUPLE(Types.STRING, Types.INT)).uid("string-to-tuple-map")
				// Logically partition the stream for each word
				.keyBy(v -> v.f0)
				 // Count the appearances by product per partition
				.window(TumblingProcessingTimeWindows.of(Time.minutes(1))).sum(1)
				.map(t -> new TradeCount(t.f0, t.f1)); // .sinkTo(S3Sink(s3Bucket)); //.uid("s3-parquet-sink");

		dss.sinkTo(S3Sink(s3Bucket));

		dss.print();

		env.execute("Flink S3 Streaming Sink Job");
	}

}