package com.dke.retail.order_management_and_fulfillment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A place that can ship orders: a distribution centre (DC) or a store doing ship-from-store. */
@Entity
public class FulfillmentNode {

    @Id
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String nodeType;
    @Column(nullable = false)
    private String region;
    /** Lower is preferred (cheaper to ship from) among nodes in the same region. */
    @Column(nullable = false)
    private int priority;
    @Column(nullable = false)
    private boolean active;

    protected FulfillmentNode() {
    }

    public FulfillmentNode(String code, String name, String nodeType, String region, int priority, boolean active) {
        this.code = code;
        this.name = name;
        this.nodeType = nodeType;
        this.region = region;
        this.priority = priority;
        this.active = active;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getNodeType() { return nodeType; }
    public String getRegion() { return region; }
    public int getPriority() { return priority; }
    public boolean isActive() { return active; }
}
