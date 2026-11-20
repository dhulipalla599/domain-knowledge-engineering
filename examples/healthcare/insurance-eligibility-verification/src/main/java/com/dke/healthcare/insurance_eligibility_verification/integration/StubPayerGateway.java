package com.dke.healthcare.insurance_eligibility_verification.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Deterministic stand-in for a clearinghouse. Answers are keyed by member ID; one payer is "down". */
@Component
public class StubPayerGateway implements PayerGateway {

    private enum Plan { ACTIVE, TERMINATED }

    private static final Map<String, Plan> MEMBERS = Map.of(
            "NWH884213307", Plan.ACTIVE,
            "NWH551902118", Plan.TERMINATED,
            "BHP200417765", Plan.ACTIVE);
    private static final Set<String> OFFLINE_PAYERS = Set.of("BLUEHARBOR");

    @Override
    public PayerResponse inquire(EligibilityInquiry inquiry) {
        if (OFFLINE_PAYERS.contains(inquiry.payerId())) {
            throw new PayerUnavailableException("Payer " + inquiry.payerId() + " did not respond within 5 seconds");
        }
        Plan plan = MEMBERS.get(inquiry.memberId());
        if (plan == null) {
            return PayerResponse.rejected("72", "Invalid/Missing Subscriber/Insured ID");
        }
        LocalDate dos = inquiry.serviceDate();
        if (plan == Plan.TERMINATED) {
            // Coverage ended at the end of the month before the date of service.
            LocalDate end = dos.withDayOfMonth(1).minusDays(1);
            return new PayerResponse(false, end.minusYears(1).plusDays(1), end, null, null, null,
                    "Coverage terminated " + end);
        }
        return new PayerResponse(true, LocalDate.of(dos.getYear(), 1, 1), LocalDate.of(dos.getYear(), 12, 31),
                new BigDecimal("30.00"), new BigDecimal("450.00"), null, null);
    }
}
