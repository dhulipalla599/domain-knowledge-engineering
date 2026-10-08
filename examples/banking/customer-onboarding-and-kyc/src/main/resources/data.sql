-- Sample data so the analyst queue has something to show on first start.
INSERT INTO onboarding_application
  (id, idempotency_key, legal_name, date_of_birth, nationality, occupation, status,
   screening_outcome, risk_rating, decision_reason, created_at, version)
VALUES
  (CAST('7b0f6c1e-2d4a-4c55-9a7e-1f2e3d4c5b6a' AS UUID), 'seed-1', 'Viktor Petrov', DATE '1985-03-14', 'GB',
   'Accountant', 'IN_REVIEW', 'POSSIBLE_MATCH', 'LOW', NULL, TIMESTAMP WITH TIME ZONE '2026-10-01 09:15:00+00:00', 0),
  (CAST('3c9d2b7a-8e1f-4a6b-b2c4-5d6e7f8a9b0c' AS UUID), 'seed-2', 'Amara Okafor', DATE '1990-11-02', 'XA',
   'Car dealer', 'IN_REVIEW', 'CLEAR', 'HIGH', NULL, TIMESTAMP WITH TIME ZONE '2026-10-01 10:40:00+00:00', 0);
