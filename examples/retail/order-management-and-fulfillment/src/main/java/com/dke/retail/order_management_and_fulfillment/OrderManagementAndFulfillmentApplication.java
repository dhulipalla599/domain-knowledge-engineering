package com.dke.retail.order_management_and_fulfillment;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Entry point for the "Order Management and Fulfillment" example. Run with: mvn spring-boot:run */
@SpringBootApplication
public class OrderManagementAndFulfillmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderManagementAndFulfillmentApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
