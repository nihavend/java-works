package com.tabii.ref_impl.flink;

import java.time.Duration;

import com.tabii.ref_impl.flink.model.Config;
import com.tabii.ref_impl.flink.model.TabiiLogSchema;
import com.tabii.ref_impl.flink.model.message.LogWrapper;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.AvroParquetWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.flink.streaming.api.functions.source.SourceFunction;



public class StreamingTabiiLogsToS3Parquet {

	public StreamingTabiiLogsToS3Parquet(SourceFunction<Long> source, SinkFunction<Long> sink) {
	}

	public StreamingTabiiLogsToS3Parquet() {
	}

	
	private static FileSink<com.tabii.ref_impl.flink.model.avro.LogWrapperAvro> S3Sink(String s3SinkPath) {
		return 
				FileSink
				.forBulkFormat(new Path(s3SinkPath), AvroParquetWriters.forSpecificRecord(com.tabii.ref_impl.flink.model.avro.LogWrapperAvro.class))
				.withBucketAssigner(new DateTimeBucketAssigner<>("'year='yyyy'/month='MM'/day='dd'/hour='HH/"))
				.withOutputFileConfig(OutputFileConfig.builder().withPartSuffix(".parquet").build()).build();
	}

	
	public void execute() throws Exception {

		Config config = StreamingTabiiLogsToS3Parquet.readConfig();

		Configuration conf = new Configuration();
		StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);
		env.setRuntimeMode(RuntimeExecutionMode.STREAMING);
		env.enableCheckpointing(60000);
		env.setParallelism(1);
		
		
		KafkaSource<LogWrapper> kafkaSource = KafkaSource.<LogWrapper>builder()
				.setBootstrapServers(config.bootstrapServers) // "localhost:9092"
				.setTopics(config.kafkaTopic) // "flinktopic"
				.setGroupId(config.kafkaGroup) // "flinkgroup"
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new TabiiLogSchema()).build();

		DataStream<LogWrapper> logStream = env.fromSource(
				kafkaSource
				,WatermarkStrategy
				.forBoundedOutOfOrderness(Duration.ofMinutes(1)), "Kafka Source").setParallelism(1);

		
		DataStream<com.tabii.ref_impl.flink.model.avro.LogWrapperAvro> avroStream = logStream.map(new LogWrapperToAvroMapper());
		

		avroStream.sinkTo(S3Sink(config.bucketPath));
		
		
		env.execute();
	}

	private static Config readConfig() {

		Config config  = new Config();

		config.bootstrapServers = System.getenv("BOOTSTRAP_SERVERS");
		config.kafkaTopic = System.getenv("KAFKA_TOPIC");
		config.kafkaGroup = System.getenv("KAFKA_GROUP");
		config.bucketPath = System.getenv("BUCKET_PATH");

		// Validate environment variables
		if (config.bootstrapServers == null || config.bootstrapServers.isEmpty()) {
			throw new IllegalStateException("BOOTSTRAP_SERVERS environment variable is not set or empty");
		}
		if (config.kafkaTopic  == null || config.kafkaTopic .isEmpty()) {
			throw new IllegalStateException("KAFKA_TOPIC environment variable is not set or empty");
		}
		if (config.kafkaGroup  == null || config.kafkaGroup .isEmpty()) {
			throw new IllegalStateException("KAFKA_GROUP environment variable is not set or empty");
		}
		if (config.bucketPath == null || config.bucketPath.isEmpty()) {
			throw new IllegalStateException("BUCKET_PATH environment variable is not set or empty");
		}

		return config;
	}

	public static void main(String[] args) throws Exception {
		StreamingTabiiLogsToS3Parquet job = new StreamingTabiiLogsToS3Parquet();
		job.execute();

	}
}