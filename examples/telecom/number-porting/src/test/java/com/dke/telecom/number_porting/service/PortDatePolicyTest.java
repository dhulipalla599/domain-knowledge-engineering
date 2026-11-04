package com.dke.telecom.number_porting.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PortDatePolicyTest {

    private final PortDatePolicy policy = new PortDatePolicy();
    private final LocalDate friday = LocalDate.of(2026, 11, 6);

    @Test
    void earliestDateSkipsWeekend() {
        assertEquals(LocalDate.of(2026, 11, 9), policy.earliestAllowed(friday));
    }

    @Test
    void sameDayIsRejected() {
        assertNotNull(policy.violation(friday, friday));
    }

    @Test
    void weekendIsRejected() {
        assertTrue(policy.violation(LocalDate.of(2026, 11, 7), friday).contains("not a business day"));
    }

    @Test
    void nextBusinessDayIsAccepted() {
        assertNull(policy.violation(LocalDate.of(2026, 11, 9), friday));
    }
}
