package com.dke.airline.passenger_name_record_pnr.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PnrTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");
    private static final List<FlightSegment> SEGMENTS =
            List.of(new FlightSegment("SK101", LocalDate.of(2026, 12, 1), "LIS", "AMS"));

    private Pnr open(Instant limit) {
        return Pnr.open("ABC123", "a@example.com", limit, List.of(new PassengerName(" ana ", "da silva")), SEGMENTS);
    }

    @Test
    void normalisesNamesAndStartsOnHold() {
        Pnr pnr = open(NOW.plusSeconds(3600));
        assertThat(pnr.getStatus()).isEqualTo(PnrStatus.ON_HOLD);
        assertThat(pnr.getPassengers()).containsExactly(new PassengerName("ANA", "DA SILVA"));
    }

    @Test
    void rejectsTicketingAfterTimeLimit() {
        Pnr pnr = open(NOW.minusSeconds(1));
        assertThatThrownBy(() -> pnr.ensureTicketable(NOW)).isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("time limit");
    }

    @Test
    void rejectsMoreThanNinePassengers() {
        List<PassengerName> ten = IntStream.range(0, 10).mapToObj(i -> new PassengerName("PAX", "ONE")).toList();
        assertThatThrownBy(() -> Pnr.open("ABC123", "a@example.com", NOW, ten, SEGMENTS))
                .isInstanceOf(RuleViolationException.class);
    }

    @Test
    void cancelledPnrCannotBeTicketed() {
        Pnr pnr = open(NOW.plusSeconds(3600));
        pnr.cancel();
        assertThatThrownBy(() -> pnr.markTicketed(List.of("1"))).isInstanceOf(RuleViolationException.class)
                .hasMessageContaining("Illegal transition");
    }
}
