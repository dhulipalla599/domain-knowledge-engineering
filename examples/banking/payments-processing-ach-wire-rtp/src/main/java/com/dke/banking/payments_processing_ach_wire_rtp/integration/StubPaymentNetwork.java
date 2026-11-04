package com.dke.banking.payments_processing_ach_wire_rtp.integration;

import com.dke.banking.payments_processing_ach_wire_rtp.domain.PaymentOrder;
import org.springframework.stereotype.Component;

/** Deterministic stand-in: refuses the all-zero routing number, accepts everything else. */
@Component
public class StubPaymentNetwork implements PaymentNetwork {

    @Override
    public NetworkResponse submit(PaymentOrder order) {
        if ("000000000".equals(order.getBeneficiaryRouting())) {
            return NetworkResponse.refused("Invalid routing number");
        }
        return NetworkResponse.ok();
    }
}
