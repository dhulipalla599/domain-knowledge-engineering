package com.dke.retail.order_management_and_fulfillment.integration;

import org.springframework.stereotype.Component;

/** Deterministic label service: tracking number = TRK-{node}-{order}. */
@Component
public class StubCarrierGateway implements CarrierGateway {

    @Override
    public String createLabel(String nodeCode, String orderNumber) {
        return "TRK-" + nodeCode + "-" + orderNumber;
    }
}
