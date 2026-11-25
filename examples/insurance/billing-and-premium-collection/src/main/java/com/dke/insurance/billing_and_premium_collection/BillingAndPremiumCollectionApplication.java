package com.dke.insurance.billing_and_premium_collection;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Entry point for the "Billing and Premium Collection" example. Run with: mvn spring-boot:run */
@SpringBootApplication
public class BillingAndPremiumCollectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingAndPremiumCollectionApplication.class, args);
    }

    /** Injected so payment timestamps are testable; production uses UTC system time too. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
