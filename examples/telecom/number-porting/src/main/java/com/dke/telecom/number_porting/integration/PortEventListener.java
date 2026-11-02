package com.dke.telecom.number_porting.integration;

import com.dke.telecom.number_porting.domain.PortEvents.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Stand-in for Kafka consumers: routing update, subscriber notifications, audit. */
@Component
public class PortEventListener {

    private static final Logger log = LoggerFactory.getLogger(PortEventListener.class);
    private final RoutingGateway routing;

    public PortEventListener(RoutingGateway routing) {
        this.routing = routing;
    }

    @EventListener
    void onSubmitted(PortRequestSubmitted e) {
        log.info("[port.request.submitted] request={} msisdn={}", e.requestId(), e.msisdn());
    }

    @EventListener
    void onScheduled(PortRequestScheduled e) {
        log.info("[port.request.scheduled] SMS: your number {} moves on {}", e.msisdn(), e.portDate());
    }

    @EventListener
    void onRejected(PortRequestRejected e) {
        log.info("[port.request.rejected] request={} reason={}", e.requestId(), e.reason());
    }

    @EventListener
    void onCompleted(PortCompleted e) {
        routing.routeTo(e.msisdn(), e.newOperator());
        log.info("[port.request.completed] {} now routed to {}", e.msisdn(), e.newOperator());
    }
}
