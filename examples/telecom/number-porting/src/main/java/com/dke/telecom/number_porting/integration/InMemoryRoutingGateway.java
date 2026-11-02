package com.dke.telecom.number_porting.integration;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRoutingGateway implements RoutingGateway {

    private final Map<String, String> routes = new ConcurrentHashMap<>();

    @Override
    public void routeTo(String msisdn, String operator) {
        routes.put(msisdn, operator);
    }

    public Optional<String> routeOf(String msisdn) {
        return Optional.ofNullable(routes.get(msisdn));
    }
}
