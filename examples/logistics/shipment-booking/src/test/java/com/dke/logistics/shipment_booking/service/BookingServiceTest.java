package com.dke.logistics.shipment_booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dke.logistics.shipment_booking.domain.Booking;
import com.dke.logistics.shipment_booking.domain.BookingStatus;
import com.dke.logistics.shipment_booking.domain.RateQuote;
import com.dke.logistics.shipment_booking.integration.StubCarrierGateway;
import com.dke.logistics.shipment_booking.repository.BookingRepository;
import com.dke.logistics.shipment_booking.repository.RateQuoteRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");

    private final BookingRepository bookings = Mockito.mock(BookingRepository.class);
    private final RateQuoteRepository quotes = Mockito.mock(RateQuoteRepository.class);
    private final List<Object> published = new ArrayList<>();
    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, quotes, new StubCarrierGateway(), published::add,
                Clock.fixed(NOW, ZoneOffset.UTC));
        Mockito.when(bookings.findByIdempotencyKey(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(quotes.findById("OK")).thenReturn(Optional.of(quote(NOW.plusSeconds(60), 1000, false)));
        Mockito.when(quotes.findById("OLD")).thenReturn(Optional.of(quote(NOW, 1000, false)));
    }

    @Test
    void confirmsBookingAndPublishesEvents() {
        Booking b = service.book("k1", "C1", "OK", 500, null);
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(b.getCarrierConfirmation()).startsWith("NOR-");
        assertThat(published).hasSize(2);
    }

    @Test
    void rejectsExpiredQuote() {
        assertThatThrownBy(() -> service.book("k2", "C1", "OLD", 500, null))
                .isInstanceOf(BookingRuleException.class).hasMessageContaining("expired");
    }

    @Test
    void rejectsOverweightAndDangerousGoodsOnUnsupportedQuote() {
        assertThatThrownBy(() -> service.book("k3", "C1", "OK", 1500, null))
                .hasMessageContaining("exceeds");
        assertThatThrownBy(() -> service.book("k4", "C1", "OK", 100, "UN1203"))
                .hasMessageContaining("dangerous goods");
    }

    @Test
    void replayedIdempotencyKeyReturnsExistingBooking() {
        Booking first = service.book("k5", "C1", "OK", 500, null);
        Mockito.when(bookings.findByIdempotencyKey("k5")).thenReturn(Optional.of(first));
        assertThat(service.book("k5", "C1", "OK", 500, null)).isSameAs(first);
        assertThat(published).hasSize(2);
    }

    @Test
    void cancelledBookingCannotBeConfirmedAgain() {
        Booking b = service.book("k6", "C1", "OK", 500, null);
        b.cancel();
        assertThatThrownBy(() -> b.confirm("X")).isInstanceOf(IllegalStateException.class);
    }

    private static RateQuote quote(Instant validUntil, double maxKg, boolean dg) {
        return new RateQuote("Q", "Northwind Freight", "A", "B", "LCL", BigDecimal.TEN, "USD", maxKg, dg, validUntil);
    }
}
