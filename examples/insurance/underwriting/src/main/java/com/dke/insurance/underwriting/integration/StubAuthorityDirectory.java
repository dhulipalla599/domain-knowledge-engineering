package com.dke.insurance.underwriting.integration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StubAuthorityDirectory implements AuthorityDirectory {

    private static final Map<String, BigDecimal> LIMITS = Map.of(
            "uw-junior", new BigDecimal("500000"),
            "uw-senior", new BigDecimal("2000000"));

    @Override
    public Optional<BigDecimal> approvalLimit(String underwriterId) {
        return Optional.ofNullable(LIMITS.get(underwriterId));
    }
}
