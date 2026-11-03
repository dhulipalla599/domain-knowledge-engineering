package com.dke.airline.passenger_name_record_pnr.integration;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

/** Deterministic stub: X00001, X00002 ... Real locators avoid look-alike characters such as 0/O and 1/I. */
@Component
public class SequentialLocatorGenerator implements RecordLocatorGenerator {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public String next() {
        return String.format("X%05d", counter.incrementAndGet());
    }
}
