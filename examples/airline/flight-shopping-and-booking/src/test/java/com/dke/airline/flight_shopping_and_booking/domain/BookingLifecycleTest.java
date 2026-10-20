package com.dke.airline.flight_shopping_and_booking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingLifecycleTest {

    private static final Instant NOW = Instant.parse("2027-03-01T10:00:00Z");

    private static Booking newBooking() {
        return new Booking("k1", UUID.randomUUID(), "Amara Osei", "amara@example.com",
                new BigDecimal("289.00"), "USD", NOW, NOW.plusSeconds(900));
    }

    @Test
    void heldBookingMovesThroughPaymentToTicket() {
        var booking = newBooking();
        booking.confirmPayment();
        booking.issueTicket("DK1001", "0012000001001");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.TICKETED);
        assertThat(booking.getPnr()).isEqualTo("DK1001");
    }

    @Test
    void cannotTicketWithoutPayment() {
        assertThatThrownBy(() -> newBooking().issueTicket("DK1", "1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HELD to TICKETED");
    }

    @Test
    void expiredAndCancelledAreTerminal() {
        var expired = newBooking();
        expired.expire();
        assertThatThrownBy(() -> expired.cancel("changed mind")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void holdExpiresExactlyAtDeadline() {
        var booking = newBooking();
        assertThat(booking.isHoldExpired(NOW.plusSeconds(899))).isFalse();
        assertThat(booking.isHoldExpired(NOW.plusSeconds(900))).isTrue();
    }

    @Test
    void lastSeatCanBeHeldOnlyOnce() {
        var offer = new FlightOffer(UUID.randomUUID(), "DK205", "SFO", "JFK", LocalDate.of(2027, 3, 15),
                LocalTime.of(13, 10), "ECONOMY_SAVER", new BigDecimal("312.50"), "USD", 1);
        offer.holdSeat();
        assertThat(offer.getSeatsAvailable()).isZero();
        assertThatThrownBy(offer::holdSeat).isInstanceOf(IllegalStateException.class).hasMessageContaining("No seats left");
        offer.releaseSeat();
        assertThat(offer.getSeatsAvailable()).isEqualTo(1);
    }
}
