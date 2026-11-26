package com.dke.capital_markets.clearing_and_settlement.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** US equities convention: T+1 business days, New York time, affirmation cutoff 9:00 pm ET on trade date. */
@Component
public class SettlementCalendar {

    public static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private static final int AFFIRMATION_CUTOFF_HOUR = 21;

    private final Set<LocalDate> holidays;

    /** @param holidays comma-separated ISO dates, e.g. "2026-11-26,2026-12-25" */
    public SettlementCalendar(@Value("${clearing.holidays:}") String holidays) {
        this.holidays = Arrays.stream(holidays.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .map(LocalDate::parse)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isBusinessDay(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY && !holidays.contains(date);
    }

    /** Trade date plus {@code cycle} business days (cycle = 1 for T+1). */
    public LocalDate settlementDate(LocalDate tradeDate, int cycle) {
        LocalDate date = tradeDate;
        for (int added = 0; added < cycle; ) {
            date = date.plusDays(1);
            if (isBusinessDay(date)) {
                added++;
            }
        }
        return date;
    }

    public ZonedDateTime affirmationCutoff(LocalDate tradeDate) {
        return tradeDate.atTime(AFFIRMATION_CUTOFF_HOUR, 0).atZone(MARKET_ZONE);
    }
}
