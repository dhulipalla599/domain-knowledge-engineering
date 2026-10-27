package com.dke.insurance.underwriting.integration;

import java.math.BigDecimal;
import java.util.Optional;

/** Port to the HR/IAM system that knows each underwriter's signing authority. */
public interface AuthorityDirectory {

    Optional<BigDecimal> approvalLimit(String underwriterId);
}
