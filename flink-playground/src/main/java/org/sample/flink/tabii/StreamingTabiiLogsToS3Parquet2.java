package org.sample.flink.tabii;

import java.time.Duration;

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
import org.sample.flink.tabii.model.TabiiLogSchema;
import org.sample.flink.tabii.model.avro.LogWrapperAvro;
import org.sample.flink.tabii.model.message.LogWrapper;


public class StreamingTabiiLogsToS3Parquet2 {
	// private SourceFunction<Long> source;
	// private SinkFunction<Long> sink;

	private static String s3Bucket = "s3a://int-avatar-test/flink-parquet/";
	
	public StreamingTabiiLogsToS3Parquet2(SourceFunction<Long> source, SinkFunction<Long> sink) {
		// this.source = source;
		// this.sink = sink;
	}

	public StreamingTabiiLogsToS3Parquet2() {
	}

	
	private static FileSink<LogWrapperAvro> S3Sink(String s3SinkPath) {
		return 
				FileSink
				.forBulkFormat(new Path(s3SinkPath), AvroParquetWriters.forSpecificRecord(LogWrapperAvro.class))
				.withBucketAssigner(new DateTimeBucketAssigner<>("'year='yyyy'/month='MM'/day='dd'/hour='HH/"))
				.withOutputFileConfig(OutputFileConfig.builder().withPartSuffix(".parquet").build()).build();
	}

	
	public void execute() throws Exception {
		
		// StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		Configuration conf = new Configuration();
		StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(conf);

		// StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		// env.setRuntimeMode(RuntimeExecutionMode.STREAMING);
		// env.enableCheckpointing(60000);
		// env.setParallelism(1);
		
		
		
		KafkaSource<LogWrapper> kafkaSource = KafkaSource.<LogWrapper>builder()
				.setBootstrapServers("localhost:9092")
				.setTopics("flinktopic")
				.setGroupId("flinkgroup")
				.setStartingOffsets(OffsetsInitializer.latest())
				.setValueOnlyDeserializer(new TabiiLogSchema()).build();

		DataStream<LogWrapper> logStream = env.fromSource(
				kafkaSource
				,WatermarkStrategy
				.forBoundedOutOfOrderness(Duration.ofMinutes(1)), "Kafka Source").setParallelism(1);

		
		DataStream<LogWrapperAvro> avroStream = logStream.map(new LogWrapperToAvroMapper());
		
//		SerializableTimestampAssigner<LogWrapper> sz = new SerializableTimestampAssigner<LogWrapper>() {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public long extractTimestamp(LogWrapper order, long l) {
//				try {
//					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//					Date date = sdf.parse(order.eventTime);
//					return date.getTime();
//				} catch (ParseException e) {
//					return 0;
//				}
//			}
//		};
		
//		WatermarkStrategy<LogWrapper> watermarkStrategy = WatermarkStrategy
//				.<LogWrapper>forBoundedOutOfOrderness(Duration.ofMillis(100)).withTimestampAssigner(sz)
//				.withIdleness(Duration.ofSeconds(10));

		// DataStream<LogWrapper> watermarkDataStream = stream.assignTimestampsAndWatermarks(watermarkStrategy);
		
		
//		@SuppressWarnings("deprecation")
//		DataStream<OrderAgg> groupedData = watermarkDataStream.keyBy("userId")
//				.window(TumblingProcessingTimeWindows.of(Time.milliseconds(2500), Time.milliseconds(500)))
//				// .sum("priceAmount");
//				.apply(new Avg());

		
		
		
//		KafkaSink<LogWrapper> sink = KafkaSink.<LogWrapper>builder().setBootstrapServers("localhost:9092")
//				.setRecordSerializer(new TabiiLogSinkSchema("flinkout"))
//				.setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE).build();

		// groupedData.sinkTo(sink);
		
		// stream.sinkTo(sink);

		avroStream.sinkTo(S3Sink(s3Bucket));
		
		
		env.execute();
	}

	public static void main(String[] args) throws Exception {
		StreamingTabiiLogsToS3Parquet2 job = new StreamingTabiiLogsToS3Parquet2();
		job.execute();

	}
}