package com.dke.telecom.number_porting.api;

import com.dke.telecom.number_porting.domain.PortRequest;
import java.time.LocalDate;

public record PortRequestResponse(Long id, String msisdn, String subscriberName, String donorOperator,
                                  String recipientOperator, LocalDate requestedPortDate, String status,
                                  String rejectionReason) {
    static PortRequestResponse from(PortRequest r) {
        return new PortRequestResponse(r.getId(), r.getMsisdn(), r.getSubscriberName(), r.getDonorOperator(),
                r.getRecipientOperator(), r.getRequestedPortDate(), r.getStatus().name(), r.getRejectionReason());
    }
}
