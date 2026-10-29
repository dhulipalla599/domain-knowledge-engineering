package com.dke.retail.inventory_management.service;

import com.dke.retail.inventory_management.domain.DomainException;
import com.dke.retail.inventory_management.domain.InventoryEvent;
import com.dke.retail.inventory_management.domain.Reservation;
import com.dke.retail.inventory_management.domain.ReservationStatus;
import com.dke.retail.inventory_management.domain.StockItem;
import com.dke.retail.inventory_management.repository.ReservationRepository;
import com.dke.retail.inventory_management.repository.StockItemRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    static final Duration HOLD = Duration.ofMinutes(15);

    /** created=false means the same (orderRef, sku) was already reserved and the original is returned. */
    public record Outcome(Reservation reservation, boolean created) {
    }

    private final StockItemRepository stock;
    private final ReservationRepository reservations;
    private final ApplicationEventPublisher events;

    public InventoryService(StockItemRepository stock, ReservationRepository reservations,
                            ApplicationEventPublisher events) {
        this.stock = stock;
        this.reservations = reservations;
        this.events = events;
    }

    @Transactional
    public Outcome reserve(String orderRef, String sku, int quantity) {
        var existing = reservations.findByOrderRefAndSku(orderRef, sku);
        if (existing.isPresent()) {
            return new Outcome(existing.get(), false); // idempotent replay: no second hold
        }
        StockItem item = stock.findBySku(sku).orElseThrow(() -> new DomainException.NotFound("SKU", sku));
        item.reserve(quantity); // throws InsufficientStock when quantity > onHand - reserved

        Reservation reservation = reservations.save(new Reservation(orderRef, sku, quantity, Instant.now().plus(HOLD)));
        events.publishEvent(new InventoryEvent.InventoryReserved(reservation.getId(), orderRef, sku, quantity));
        if (item.belowReorderPoint()) {
            events.publishEvent(new InventoryEvent.LowStockDetected(sku, item.available(), item.getReorderPoint()));
        }
        return new Outcome(reservation, true);
    }

    @Transactional
    public Reservation confirm(String reservationId) {
        Reservation reservation = find(reservationId);
        reservation.confirm();
        itemFor(reservation).commit(reservation.getQuantity());
        events.publishEvent(new InventoryEvent.ReservationConfirmed(
                reservation.getId(), reservation.getSku(), reservation.getQuantity()));
        return reservation;
    }

    @Transactional
    public Reservation release(String reservationId) {
        Reservation reservation = find(reservationId);
        reservation.release();
        return giveBack(reservation);
    }

    /** Run by a scheduler in production; releases holds whose checkout never completed. */
    @Transactional
    public int expireOverdue(Instant now) {
        var overdue = reservations.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, now);
        overdue.forEach(r -> {
            r.expire();
            giveBack(r);
        });
        return overdue.size();
    }

    @Transactional(readOnly = true)
    public Reservation get(String reservationId) {
        return find(reservationId);
    }

    @Transactional(readOnly = true)
    public StockItem stockFor(String sku) {
        return stock.findBySku(sku).orElseThrow(() -> new DomainException.NotFound("SKU", sku));
    }

    private Reservation giveBack(Reservation reservation) {
        itemFor(reservation).release(reservation.getQuantity());
        events.publishEvent(new InventoryEvent.ReservationReleased(
                reservation.getId(), reservation.getSku(), reservation.getQuantity(), reservation.getStatus()));
        return reservation;
    }

    private Reservation find(String id) {
        return reservations.findById(id).orElseThrow(() -> new DomainException.NotFound("Reservation", id));
    }

    private StockItem itemFor(Reservation reservation) {
        return stockFor(reservation.getSku());
    }
}
