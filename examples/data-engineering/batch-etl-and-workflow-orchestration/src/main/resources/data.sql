-- A previous night's successful run and its facts, so the API returns something useful straight away.
INSERT INTO pipeline_run (business_date, status, triggered_by, rows_extracted, rows_loaded, rows_rejected, started_at, finished_at)
VALUES ('2026-11-07', 'SUCCEEDED', 'SCHEDULE', 3, 3, 0, TIMESTAMP '2026-11-08 02:00:00', TIMESTAMP '2026-11-08 02:00:04');
INSERT INTO sales_fact (order_id, customer_id, sku, quantity, unit_price, total_amount, currency, business_date) VALUES
('SO-0901', 'C-101', 'TENT-2P', 1, 189.00, 189.00, 'USD', '2026-11-07'),
('SO-0902', 'C-102', 'BOOT-42', 2, 94.50, 189.00, 'USD', '2026-11-07'),
('SO-0903', 'C-103', 'SOCK-M', 4, 12.25, 49.00, 'USD', '2026-11-07');
