package com.dke.banking.core_banking_ledger_and_accounts.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dke.banking.core_banking_ledger_and_accounts.integration.AuditTrailListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Drives the API over HTTP (MockMvc) with the real database, listeners and rules. */
@SpringBootTest
@AutoConfigureMockMvc
class LedgerApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private AuditTrailListener audit;

    private JsonNode transfer(String key, String from, String to, long amount, String currency, int expected) throws Exception {
        String body = """
                {"fromAccount":"%s","toAccount":"%s","amountMinor":%d,"currency":"%s","reference":"test"}
                """.formatted(from, to, amount, currency);
        String response = mvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expected))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private long balance(String number) throws Exception {
        String response = mvc.perform(get("/api/v1/accounts/" + number)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("balanceMinor").asLong();
    }

    @Test
    void transferMovesMoneyOnceEvenWhenRetried() throws Exception {
        long fromBefore = balance("ACC-1001");
        long toBefore = balance("ACC-1002");

        JsonNode first = transfer("api-1", "ACC-1001", "ACC-1002", 2_500, "GBP", 201);
        JsonNode retry = transfer("api-1", "ACC-1001", "ACC-1002", 2_500, "GBP", 201);

        assertThat(first.get("status").asText()).isEqualTo("POSTED");
        assertThat(retry.get("entryId").asText()).isEqualTo(first.get("entryId").asText());
        assertThat(balance("ACC-1001")).isEqualTo(fromBefore - 2_500);
        assertThat(balance("ACC-1002")).isEqualTo(toBefore + 2_500);
        assertThat(audit.trail()).anyMatch(l -> l.contains("ledger.entry.posted " + first.get("entryId").asText()));
    }

    @Test
    void insufficientFundsIsRejectedWith422AndNothingMoves() throws Exception {
        long before = balance("ACC-1001");
        JsonNode problem = transfer("api-2", "ACC-1001", "ACC-1002", 50_000_000, "GBP", 422);
        assertThat(problem.get("code").asText()).isIn("INSUFFICIENT_FUNDS", "FRAUD_HOLD");
        JsonNode small = transfer("api-2b", "ACC-1001", "ACC-1002", before + 1, "GBP", 422);
        assertThat(small.get("code").asText()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(balance("ACC-1001")).isEqualTo(before);
    }

    @Test
    void frozenAccountCannotReceiveMoneyAndCurrencyMustMatch() throws Exception {
        assertThat(transfer("api-3", "ACC-1001", "ACC-1003", 100, "GBP", 422).get("code").asText())
                .isEqualTo("ACCOUNT_NOT_ACTIVE");
        assertThat(transfer("api-4", "ACC-1001", "ACC-2001", 100, "GBP", 422).get("code").asText())
                .isEqualTo("CURRENCY_MISMATCH");
    }

    @Test
    void largeTransferIsHeldByFraudScreening() throws Exception {
        assertThat(transfer("api-5", "ACC-1004", "ACC-1002", 6_000_000, "GBP", 422).get("code").asText())
                .isEqualTo("FRAUD_HOLD");
    }

    @Test
    void reversalRestoresBalancesAndCanOnlyHappenOnce() throws Exception {
        long fromBefore = balance("ACC-1002");
        long toBefore = balance("ACC-1001");
        String entryId = transfer("api-6", "ACC-1002", "ACC-1001", 1_000, "GBP", 201).get("entryId").asText();

        mvc.perform(post("/api/v1/transfers/" + entryId + "/reversal"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reversalOf").value(entryId));
        assertThat(balance("ACC-1002")).isEqualTo(fromBefore);
        assertThat(balance("ACC-1001")).isEqualTo(toBefore);

        mvc.perform(post("/api/v1/transfers/" + entryId + "/reversal"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NOT_REVERSIBLE"));
        mvc.perform(get("/api/v1/accounts/ACC-1002/statement")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entryId").isNotEmpty());
    }

    @Test
    void missingIdempotencyKeyIsABadRequest() throws Exception {
        mvc.perform(post("/api/v1/transfers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromAccount\":\"ACC-1001\",\"toAccount\":\"ACC-1002\",\"amountMinor\":1,\"currency\":\"GBP\"}"))
                .andExpect(status().isBadRequest());
    }
}
