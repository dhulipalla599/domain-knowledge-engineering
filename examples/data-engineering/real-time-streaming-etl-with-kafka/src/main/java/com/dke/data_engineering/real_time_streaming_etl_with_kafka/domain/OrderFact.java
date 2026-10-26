package com.dke.data_engineering.real_time_streaming_etl_with_kafka.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Curated, validated order event. eventId is unique so replays cannot create duplicates. */
@Entity
@Table(name = "order_fact", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
public class OrderFact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventId;
    @Column(nullable = false)
    private String orderId;
    @Column(nullable = false)
    private String customerId;
    @Column(nullable = false, length = 2)
    private String country;
    @Column(nullable = false)
    private long amountCents;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false)
    private Instant occurredAt;

    protected OrderFact() {
    }

    public OrderFact(String eventId, String orderId, String customerId, String country,
                     long amountCents, String currency, Instant occurredAt) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.country = country;
        this.amountCents = amountCents;
        this.currency = currency;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getCountry() { return country; }
    public long getAmountCents() { return amountCents; }
    public String getCurrency() { return currency; }
    public Instant getOccurredAt() { return occurredAt; }
}
