-- Yesterday's run, already loaded, so GET /api/runs and the claim table have data straight away.
INSERT INTO validation_run (file_name, checksum, business_date, status, rows_received, rows_loaded, rows_rejected,
                            rows_warned, error_ratio, status_reason, released_by, received_at, finished_at)
VALUES ('tpa-claims-2026-11-22.csv', '5f1c0a7e9b2d4c6e8a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f', '2026-11-22',
        'LOADED', 3, 3, 0, 0, 0.0, NULL, NULL, TIMESTAMP '2026-11-23 06:30:00', TIMESTAMP '2026-11-23 06:30:02');

INSERT INTO claim (claim_id, policy_number, loss_date, claim_amount, currency, loaded_by_run_id) VALUES
('CLM-2026-008801', 'POL-104233', '2026-11-18', 1840.00, 'GBP', 1),
('CLM-2026-008802', 'POL-118904', '2026-11-20', 620.50, 'GBP', 1),
('CLM-2026-008803', 'POL-100517', '2026-11-21', 12750.00, 'EUR', 1);
