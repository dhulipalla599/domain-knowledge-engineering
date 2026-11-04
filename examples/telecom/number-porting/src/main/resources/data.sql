INSERT INTO port_request (msisdn, subscriber_name, donor_operator, recipient_operator, requested_port_date, status, rejection_reason, idempotency_key, created_at)
VALUES ('+15550109001', 'Maria Santos', 'Contoso Telecom', 'Northwind Mobile', '2026-11-03', 'SCHEDULED', NULL, 'seed-1', CURRENT_TIMESTAMP),
       ('+15550109002', 'Jon Weber', 'Fabrikam Wireless', 'Northwind Mobile', '2026-10-20', 'COMPLETED', NULL, 'seed-2', CURRENT_TIMESTAMP),
       ('+15550109003', 'Priya Nair', 'Contoso Telecom', 'Northwind Mobile', '2026-10-28', 'REJECTED', 'SUBSCRIBER_NAME_MISMATCH', 'seed-3', CURRENT_TIMESTAMP);
