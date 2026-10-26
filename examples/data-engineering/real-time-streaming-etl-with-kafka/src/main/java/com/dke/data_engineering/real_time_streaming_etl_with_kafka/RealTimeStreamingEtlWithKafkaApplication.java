package com.dke.data_engineering.real_time_streaming_etl_with_kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Entry point for the "Real-Time Streaming ETL with Kafka" example. Run with: mvn spring-boot:run */
@SpringBootApplication
@EnableScheduling
public class RealTimeStreamingEtlWithKafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealTimeStreamingEtlWithKafkaApplication.class, args);
    }
}
