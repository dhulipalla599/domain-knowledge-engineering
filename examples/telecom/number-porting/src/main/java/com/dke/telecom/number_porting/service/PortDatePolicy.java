package com.dke.telecom.number_porting.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/** Port date rule: a business day, no earlier than the next business day after submission. */
@Component
public class PortDatePolicy {

    public LocalDate earliestAllowed(LocalDate today) {
        LocalDate d = today.plusDays(1);
        while (isWeekend(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    /** @return null when acceptable, otherwise the reason. */
    public String violation(LocalDate requested, LocalDate today) {
        if (isWeekend(requested)) {
            return "Port date " + requested + " is not a business day";
        }
        LocalDate earliest = earliestAllowed(today);
        if (requested.isBefore(earliest)) {
            return "Port date " + requested + " is before the earliest allowed date " + earliest;
        }
        return null;
    }

    private boolean isWeekend(LocalDate d) {
        return d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
