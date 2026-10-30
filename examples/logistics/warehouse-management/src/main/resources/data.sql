INSERT INTO bin (code, capacity_units, used_units, hazmat_certified) VALUES ('A-01-01', 500, 120, FALSE);
INSERT INTO bin (code, capacity_units, used_units, hazmat_certified) VALUES ('A-01-02', 100, 90, FALSE);
INSERT INTO bin (code, capacity_units, used_units, hazmat_certified) VALUES ('H-01-01', 300, 0, TRUE);

INSERT INTO receipt (id, asn_number, sku, hazmat, expected_qty, received_qty, status) VALUES (1, 'ASN-1001', 'SKU-HEADPHONES', FALSE, 200, 0, 'EXPECTED');
INSERT INTO receipt (id, asn_number, sku, hazmat, expected_qty, received_qty, status) VALUES (2, 'ASN-1002', 'SKU-LITHIUM-PACK', TRUE, 100, 0, 'EXPECTED');
INSERT INTO receipt (id, asn_number, sku, hazmat, expected_qty, received_qty, status) VALUES (3, 'ASN-1003', 'SKU-CABLES', FALSE, 50, 0, 'EXPECTED');
