package com.dke.telecom.number_porting.integration;

/** Updates network routing so calls to the number reach the new operator. Real adapter: number portability database. */
public interface RoutingGateway {
    void routeTo(String msisdn, String operator);
}
