-- Illustrative venues and fees (fictional numbers, not real exchange fee schedules).
INSERT INTO venue (code, name, take_fee_per_share, enabled) VALUES ('XNAS', 'Nasdaq Stock Market', 0.00300, TRUE);
INSERT INTO venue (code, name, take_fee_per_share, enabled) VALUES ('ARCX', 'NYSE Arca', 0.00300, TRUE);
INSERT INTO venue (code, name, take_fee_per_share, enabled) VALUES ('BATS', 'Cboe BZX', 0.00250, TRUE);
INSERT INTO venue (code, name, take_fee_per_share, enabled) VALUES ('IEXG', 'IEX', 0.00090, TRUE);
INSERT INTO venue (code, name, take_fee_per_share, enabled) VALUES ('EDGX', 'Cboe EDGX', 0.00280, FALSE);

-- One completed order from earlier in the day, so GET /api/orders/1 returns something.
INSERT INTO parent_order (client_order_id, account_id, symbol, side, quantity, limit_price, filled_quantity,
                          average_price, status, received_at, version)
VALUES ('SEED-0001', 'ACC-7781', 'GLBX', 'BUY', 1000, 41.13, 1000, 41.1220, 'FILLED', TIMESTAMP '2026-11-11 14:30:05', 0);
INSERT INTO child_order (parent_order_id, venue_code, quantity, limit_price, filled_quantity, fill_price, venue_exec_id, status)
VALUES (1, 'XNAS', 800, 41.12, 800, 41.12, 'XNAS-0001', 'FILLED');
INSERT INTO child_order (parent_order_id, venue_code, quantity, limit_price, filled_quantity, fill_price, venue_exec_id, status)
VALUES (1, 'ARCX', 200, 41.13, 200, 41.13, 'ARCX-0001', 'FILLED');
