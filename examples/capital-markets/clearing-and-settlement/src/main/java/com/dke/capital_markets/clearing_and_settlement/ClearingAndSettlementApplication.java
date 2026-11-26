package com.dke.capital_markets.clearing_and_settlement;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Entry point for the "Clearing and Settlement" example. Run with: mvn spring-boot:run */
@SpringBootApplication
public class ClearingAndSettlementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClearingAndSettlementApplication.class, args);
    }

    /** A fixed "now" keeps the demo deterministic; leave clearing.fixed-now empty to use the real clock. */
    @Bean
    Clock clock(@Value("${clearing.fixed-now:}") String fixedNow) {
        return fixedNow.isBlank() ? Clock.systemUTC() : Clock.fixed(Instant.parse(fixedNow), ZoneOffset.UTC);
    }
}
