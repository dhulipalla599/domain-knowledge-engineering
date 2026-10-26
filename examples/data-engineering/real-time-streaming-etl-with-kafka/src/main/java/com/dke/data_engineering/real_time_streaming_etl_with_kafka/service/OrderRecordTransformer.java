package com.dke.data_engineering.real_time_streaming_etl_with_kafka.service;

import com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain.OrderFact;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Transform + validate: CSV line -> OrderFact (amounts in minor units, codes upper-cased). */
@Component
public class OrderRecordTransformer {

    static final Set<String> CURRENCIES = Set.of("USD", "EUR", "GBP");

    public OrderFact transform(String line) {
        String[] f = line.split(",", -1);
        if (f.length != 7) {
            throw new InvalidRecordException("Expected 7 fields but found " + f.length);
        }
        for (int i = 0; i < f.length; i++) {
            f[i] = f[i].trim();
            if (f[i].isEmpty()) {
                throw new InvalidRecordException("Field " + (i + 1) + " is empty");
            }
        }
        String currency = f[5].toUpperCase();
        if (!CURRENCIES.contains(currency)) {
            throw new InvalidRecordException("Unsupported currency " + currency);
        }
        String country = f[3].toUpperCase();
        if (country.length() != 2) {
            throw new InvalidRecordException("Country must be an ISO 3166 alpha-2 code");
        }
        return new OrderFact(f[0], f[1], f[2], country, toCents(f[4]), currency, parseTime(f[6]));
    }

    private long toCents(String amount) {
        try {
            BigDecimal value = new BigDecimal(amount);
            if (value.signum() <= 0) {
                throw new InvalidRecordException("Amount must be positive");
            }
            return value.movePointRight(2).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new InvalidRecordException("Invalid amount '" + amount + "'");
        }
    }

    private Instant parseTime(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidRecordException("Invalid timestamp '" + value + "'");
        }
    }
}
