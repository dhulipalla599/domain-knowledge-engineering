INSERT INTO card_accounts (card_token, holder_name, currency, credit_limit, posted_balance, pending_holds, status, version) VALUES
  ('tok_4111_0001', 'Priya Raman', 'USD', 5000.00, 1200.00, 0.00, 'ACTIVE', 0),
  ('tok_4111_0002', 'Diego Alvarez', 'USD', 1000.00, 950.00, 0.00, 'ACTIVE', 0),
  ('tok_4111_0003', 'Mei Tanaka', 'USD', 3000.00, 0.00, 0.00, 'BLOCKED', 0);

INSERT INTO authorizations (id, network_reference, card_token, merchant_name, mcc, amount, currency, status,
                            response_code, approval_code, cleared_amount, created_at, updated_at) VALUES
  ('5b1f2c3e-0000-4000-8000-000000000001', 'NET-SEED-0001', 'tok_4111_0001', 'Blue Fern Bistro', '5812',
   80.00, 'USD', 'APPROVED', '00', '481516', NULL, TIMESTAMP '2026-11-18 19:42:00', TIMESTAMP '2026-11-18 19:42:00');

UPDATE card_accounts SET pending_holds = 80.00 WHERE card_token = 'tok_4111_0001';
