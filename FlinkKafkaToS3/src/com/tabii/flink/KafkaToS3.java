package com.tabii.flink;

import java.time.Duration;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.CheckpointingOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.plugin.PluginManager;
import org.apache.flink.core.plugin.PluginUtils;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class KafkaToS3 {

	public static void init() {

		final Logger LOG = LoggerFactory.getLogger(KafkaToS3.class);

		// Get the Logback LoggerContext
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		// Optionally, set log level for Flink packages
		// Set log level programmatically for your class
		ch.qos.logback.classic.Logger appLogger = loggerContext.getLogger(KafkaToS3.class);
		appLogger.setLevel(Level.ERROR); // Change to DEBUG, INFO, WARN, ERROR, etc.

		// Optionally, set log level for Flink packages
		ch.qos.logback.classic.Logger flinkLogger = loggerContext.getLogger("org.apache.flink");
		flinkLogger.setLevel(Level.ERROR); // Reduce Flink noise

		// Optionally, set log level for Flink packages
		ch.qos.logback.classic.Logger kafkaLogger = loggerContext.getLogger("org.apache.kafka.clients");
		kafkaLogger.setLevel(Level.ERROR); // Reduce Flink noise

		// Test the logging levels
		LOG.debug("Debug: Starting Flink job configuration");
		LOG.info("Info: Starting Flink job in local environment");
		LOG.warn("Warn: This is a warning message");
		LOG.error("Error: This is an error message (just for testing)");
	}

	private static String s3Bucket = "s3a://int-avatar-test/flink-1/";

	public static void main(String[] args) throws Exception {

		init();

		// KafkaToS3.print();

		KafkaToS3.sinkToS3();
	}

	public static void sinkToS3() throws Exception {

		StreamExecutionEnvironment see = StreamExecutionEnvironment.getExecutionEnvironment();
		see.setRuntimeMode(RuntimeExecutionMode.STREAMING);
		see.enableCheckpointing(60000);
		see.setParallelism(2);
		
		
		Configuration configs3 = new Configuration();
		configs3.set(CheckpointingOptions.CHECKPOINT_STORAGE, "filesystem");
		configs3.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, "s3://int-avatar-test/flink/checkpoint/");
		see.configure(configs3);

		KafkaSource<String> source = KafkaSource.<String>builder().setBootstrapServers("127.0.0.1:9092")
				.setTopics("test-flink-stream")
				.setGroupId("flink_streamer")
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		DataStream<String> messageStream = see.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

//		FileSink<String> sink = FileSink.forRowFormat(new Path(s3Bucket), new SimpleStringEncoder<String>("UTF-8"))
//				.withRollingPolicy(
//						DefaultRollingPolicy.builder().withRolloverInterval(Duration.ofSeconds(15 * 60 * 1000L))
//								.withInactivityInterval(Duration.ofSeconds(5 * 60 * 1000L))
//								.withMaxPartSize(new MemorySize(128 * 1024 * 1024L)).build())
//				.build();

		FileSink<String> sink = FileSink.forRowFormat(new Path(s3Bucket), new SimpleStringEncoder<String>("UTF-8"))
				.build();

		messageStream.sinkTo(sink);

		// messageStream.print();

		see.execute();
	}

	public static void print() throws Exception {

		// System.exit(0);

		// String awsRegion = "eu-west-1";

		StreamExecutionEnvironment see = StreamExecutionEnvironment.getExecutionEnvironment();
		// Configuration configs3 = new Configuration();
		// configs3.set(CheckpointingOptions.CHECKPOINT_STORAGE, "filesystem");
		// configs3.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY,
		// "s3://int-avatar-test/logs/flink/");

		// configs3.setString("s3.endpoint", s3Bucket);
		// configs3.setString("s3.region", awsRegion);
		// configs3.setBoolean("s3.path.style.access", true); // Required for MinIO
		// configs3.setBoolean("s3.ssl.enabled", false);

		// see.configure(configs3);

		KafkaSource<String> source = KafkaSource.<String>builder().setBootstrapServers("127.0.0.1:9092")
				.setTopics("test-flink-stream").setGroupId("flink_streamer")
				.setStartingOffsets(OffsetsInitializer.earliest()).setValueOnlyDeserializer(new SimpleStringSchema())
				.build();

		DataStream<String> messageStream = see.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

		// Configure AWS credentials programmatically
		Configuration config = new Configuration();
		// config.setString("fs.s3a.access.key", awsAccessKey);
		// config.setString("fs.s3a.secret.key", awsSecretKey);
		// config.setString("fs.s3a.session.token", awsSessionToken);
//		config.setString("fs.s3a.region", awsRegion);
		// config.setString("fs.s3a.aws.credentials.provider",
		// "org.apache.flink.fs.s3.common.token.DynamicTemporaryAWSCredentialsProvider");
		// config.setString("fs.s3a.ssl.enabled", "false");

		// Create a PluginManager instance (required for the updated initialize method)
		PluginManager pluginManager = PluginUtils.createPluginManagerFromRootFolder(config);

		// Initialize the S3 filesystem with the configuration and plugin manager
		// FileSystem.initialize(config, pluginManager);

		// Define the S3 output path
		// String outputPath = "s3a://int-avatar-test/logs/flink-output/";

		// Configure the FileSink to write to S3
//		FileSink<String> sink = FileSink.forRowFormat(new Path(s3Bucket), new SimpleStringEncoder<String>("UTF-8"))
//				/*
//				 * .withRollingPolicy( DefaultRollingPolicy.builder()
//				 * .withRolloverInterval(Duration.ofSeconds(15 * 60 * 1000L)) // Roll over every
//				 * 15 minutes .withInactivityInterval(Duration.ofSeconds(5 * 60 * 1000L)) //
//				 * Roll over if inactive for 5 minutes .withMaxPartSize(new MemorySize(128 *
//				 * 1024 * 1024L)) // Roll over if file size exceeds 128 MB .build() )
//				 */
//				.build();

		// Connect the stream to the sink
//		messageStream.rebalance().map(new MapFunction<String, String>() {
//			private static final long serialVersionUID = -6867736771747690202L;
//
//			@Override
//			public String map(String value) throws Exception {
//				return "Kafka and Flink says: " + value;
//			}
//		}).sinkTo(sink);

		FileSink<String> sink = FileSink.forRowFormat(new Path(s3Bucket), new SimpleStringEncoder<String>("UTF-8"))
				.withRollingPolicy(
						DefaultRollingPolicy.builder().withRolloverInterval(Duration.ofSeconds(15 * 60 * 1000L))
								.withInactivityInterval(Duration.ofSeconds(5 * 60 * 1000L))
								.withMaxPartSize(new MemorySize(128 * 1024 * 1024L)).build())
				.build();

		// messageStream.sinkTo(sink);
		messageStream.print();

		see.execute();
	}

}