package com.dke.capital_markets.trade_execution_and_smart_order_routing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;

/** An execution venue (exchange or ATS) the router may send child orders to. */
@Entity
public class Venue {

    @Id
    @Column(length = 8)
    private String code;              // ISO 10383 MIC, e.g. XNAS
    private String name;
    @Column(precision = 10, scale = 5)
    private BigDecimal takeFeePerShare; // fee for removing liquidity
    private boolean enabled;

    protected Venue() {
    }

    public Venue(String code, String name, BigDecimal takeFeePerShare, boolean enabled) {
        this.code = code;
        this.name = name;
        this.takeFeePerShare = takeFeePerShare;
        this.enabled = enabled;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getTakeFeePerShare() { return takeFeePerShare; }
    public boolean isEnabled() { return enabled; }
}
