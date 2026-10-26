package com.dke.data_engineering.real_time_streaming_etl_with_kafka.repository;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.OrderFact;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFactRepository extends JpaRepository<OrderFact, Long> {

    boolean existsByEventId(String eventId);
}
