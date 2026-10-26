-- A previous run already loaded one order; batch 1 replays it, so it is skipped as a duplicate.
INSERT INTO pipeline_run (source, status, started_at, finished_at, records_read, records_loaded, duplicates_skipped, records_rejected)
VALUES ('orders-batch-0', 'SUCCEEDED', TIMESTAMP '2026-10-19 18:05:00', TIMESTAMP '2026-10-19 18:05:02', 1, 1, 0, 0);

INSERT INTO order_fact (event_id, order_id, customer_id, country, amount_cents, currency, occurred_at)
VALUES ('E-0001', 'O-4001', 'C-090', 'US', 4200, 'USD', TIMESTAMP '2026-10-19 18:00:00');
