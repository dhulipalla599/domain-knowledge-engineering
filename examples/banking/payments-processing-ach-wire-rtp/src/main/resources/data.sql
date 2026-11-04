INSERT INTO accounts (account_number, owner_name, balance, held, version) VALUES
  ('ACC-1001', 'Maple Street Bakery', 50000.00, 0.00, 0),
  ('ACC-1002', 'Northwind Logistics', 250000.00, 0.00, 0);

INSERT INTO payment_orders (id, idempotency_key, from_account, rail, beneficiary_name, beneficiary_routing,
                            beneficiary_account, amount, status, failure_reason, created_at) VALUES
  ('11111111-1111-1111-1111-111111111111', 'seed-0001', 'ACC-1002', 'WIRE', 'Harbor Steel Supply',
   '021000021', '8800123456', 12500.00, 'SETTLED', NULL, TIMESTAMP '2026-11-03 09:15:00');
