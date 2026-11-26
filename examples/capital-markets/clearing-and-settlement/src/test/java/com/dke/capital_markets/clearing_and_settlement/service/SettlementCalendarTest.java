package com.dke.capital_markets.clearing_and_settlement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SettlementCalendarTest {

    private final SettlementCalendar calendar = new SettlementCalendar("2026-11-26, 2026-12-25");

    @Test
    void tPlusOneIsNextBusinessDay() {
        assertThat(calendar.settlementDate(LocalDate.of(2026, 11, 24), 1)).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void tPlusOneSkipsHolidayAndWeekend() {
        // Wed 25 Nov -> Thu 26 Nov is a holiday -> Fri 27 Nov
        assertThat(calendar.settlementDate(LocalDate.of(2026, 11, 25), 1)).isEqualTo(LocalDate.of(2026, 11, 27));
        // Fri 27 Nov -> Mon 30 Nov
        assertThat(calendar.settlementDate(LocalDate.of(2026, 11, 27), 1)).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void affirmationCutoffIsNinePmNewYork() {
        // 21:00 EST (UTC-5) = 02:00 UTC the next day
        assertThat(calendar.affirmationCutoff(LocalDate.of(2026, 11, 24)).toInstant())
                .hasToString("2026-11-25T02:00:00Z");
    }
}
