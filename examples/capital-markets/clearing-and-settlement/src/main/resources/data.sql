-- Depository positions. Asset USD is cash.
INSERT INTO position (id, account_id, asset, quantity) VALUES ('NORTHWIND:USD', 'NORTHWIND', 'USD', 250000.00);
INSERT INTO position (id, account_id, asset, quantity) VALUES ('NORTHWIND:ACME', 'NORTHWIND', 'ACME', 400);
INSERT INTO position (id, account_id, asset, quantity) VALUES ('BLUEHARBOR:USD', 'BLUEHARBOR', 'USD', 100000.00);
INSERT INTO position (id, account_id, asset, quantity) VALUES ('BLUEHARBOR:ACME', 'BLUEHARBOR', 'ACME', 2000);
INSERT INTO position (id, account_id, asset, quantity) VALUES ('REDCLIFF:USD', 'REDCLIFF', 'USD', 50000.00);
INSERT INTO position (id, account_id, asset, quantity) VALUES ('REDCLIFF:GLBX', 'REDCLIFF', 'GLBX', 100);

-- Trades executed Tuesday 24 Nov 2026, settling T+1 on Wednesday 25 Nov 2026.
INSERT INTO trade (trade_id, buyer_account, seller_account, symbol, quantity, price, trade_date, settlement_date, status, version)
VALUES ('T-1001', 'NORTHWIND', 'BLUEHARBOR', 'ACME', 1000, 45.10, DATE '2026-11-24', DATE '2026-11-25', 'AFFIRMED', 0);
INSERT INTO trade (trade_id, buyer_account, seller_account, symbol, quantity, price, trade_date, settlement_date, status, version)
VALUES ('T-1002', 'NORTHWIND', 'REDCLIFF', 'GLBX', 500, 120.00, DATE '2026-11-24', DATE '2026-11-25', 'AFFIRMED', 0);
INSERT INTO trade (trade_id, buyer_account, seller_account, symbol, quantity, price, trade_date, settlement_date, status, version)
VALUES ('T-1003', 'BLUEHARBOR', 'NORTHWIND', 'ACME', 300, 45.20, DATE '2026-11-24', DATE '2026-11-25', 'PENDING_AFFIRMATION', 0);
