INSERT INTO installment (policy_number, sequence_no, amount_due, amount_paid, due_date, grace_ends_on, status, version)
VALUES ('HM-2026-900001', 12, 100.00, 0.00, DATE '2026-12-01', DATE '2026-12-31', 'BILLED', 0);
INSERT INTO installment (policy_number, sequence_no, amount_due, amount_paid, due_date, grace_ends_on, status, version)
VALUES ('MT-2026-410022', 3, 85.50, 0.00, DATE '2026-10-15', DATE '2026-11-14', 'OVERDUE', 0);
INSERT INTO installment (policy_number, sequence_no, amount_due, amount_paid, due_date, grace_ends_on, status, version)
VALUES ('HM-2026-900003', 1, 1200.00, 1200.00, DATE '2026-11-20', DATE '2026-12-20', 'PAID', 0);
INSERT INTO installment (policy_number, sequence_no, amount_due, amount_paid, due_date, grace_ends_on, status, version)
VALUES ('HM-2026-900004', 6, 64.20, 0.00, DATE '2026-12-05', DATE '2027-01-04', 'BILLED', 0);
INSERT INTO payment (idempotency_key, installment_id, amount, gateway_reference, received_at)
VALUES ('seed-0003', 3, 1200.00, 'PSP-5e1f0a2c', TIMESTAMP '2026-11-18 09:30:00');
