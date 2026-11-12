package com.dke.retail.order_management_and_fulfillment.integration;

/** Port to the parcel carrier / multi-carrier shipping API that buys labels. */
public interface CarrierGateway {

    /** Buys a shipping label for the parcel leaving {@code nodeCode} and returns its tracking number. */
    String createLabel(String nodeCode, String orderNumber);
}
