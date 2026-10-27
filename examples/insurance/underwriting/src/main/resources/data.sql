INSERT INTO policy_application (idempotency_key, applicant_name, sum_insured, year_built, flood_zone, prior_claims, risk_score, status, decision_reason, decided_by, created_at)
VALUES ('seed-001', 'Elena Rossi', 320000.00, 2008, false, 0, 10, 'APPROVED', 'Within automatic acceptance appetite', 'auto-rules', CURRENT_TIMESTAMP);
INSERT INTO policy_application (idempotency_key, applicant_name, sum_insured, year_built, flood_zone, prior_claims, risk_score, status, decision_reason, decided_by, created_at)
VALUES ('seed-002', 'Priya Nair', 900000.00, 1936, true, 1, 70, 'REFERRED', 'Sum insured above auto-approval limit', 'auto-rules', CURRENT_TIMESTAMP);
INSERT INTO policy_application (idempotency_key, applicant_name, sum_insured, year_built, flood_zone, prior_claims, risk_score, status, decision_reason, decided_by, created_at)
VALUES ('seed-003', 'Marcus Webb', 410000.00, 1975, false, 4, 50, 'DECLINED', '4 claims in 5 years exceeds the knock-out limit', 'auto-rules', CURRENT_TIMESTAMP);
