INSERT INTO sim_card (iccid, status, msisdn, version) VALUES ('8944000000000000001', 'AVAILABLE', NULL, 0);
INSERT INTO sim_card (iccid, status, msisdn, version) VALUES ('8944000000000000002', 'AVAILABLE', NULL, 0);
INSERT INTO sim_card (iccid, status, msisdn, version) VALUES ('8944000000000000003', 'AVAILABLE', NULL, 0);
INSERT INTO sim_card (iccid, status, msisdn, version) VALUES ('8944000000000000099', 'ASSIGNED', '+15550100001', 0);

INSERT INTO phone_number (msisdn, status, version) VALUES ('+15550100001', 'IN_USE', 0);
INSERT INTO phone_number (msisdn, status, version) VALUES ('+15550100002', 'AVAILABLE', 0);
INSERT INTO phone_number (msisdn, status, version) VALUES ('+15550100003', 'AVAILABLE', 0);
INSERT INTO phone_number (msisdn, status, version) VALUES ('+15550100004', 'AVAILABLE', 0);

INSERT INTO subscription (id, idempotency_key, full_name, date_of_birth, id_document_number, plan, iccid,
                          msisdn, status, rejection_reason, created_at, activated_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'seed-001', 'Maya Okafor', '1990-04-12', 'ID-770001',
        'POSTPAID_UNLIMITED', '8944000000000000099', '+15550100001', 'ACTIVE', NULL,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
