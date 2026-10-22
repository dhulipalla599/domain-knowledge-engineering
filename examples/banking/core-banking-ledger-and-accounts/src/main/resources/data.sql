-- Fictional customers of "Northwind Bank". Amounts are minor units (pence/cents).
INSERT INTO accounts (id, number, owner_name, currency, balance_minor, overdraft_limit_minor, status, version) VALUES
 ('11111111-1111-1111-1111-111111111001', 'ACC-1001', 'Amara Okafor',         'GBP',  100000,      0, 'ACTIVE', 0),
 ('11111111-1111-1111-1111-111111111002', 'ACC-1002', 'Bilal Haddad',         'GBP',   25000,  50000, 'ACTIVE', 0),
 ('11111111-1111-1111-1111-111111111003', 'ACC-1003', 'Chen Wei',             'GBP',    5000,      0, 'FROZEN', 0),
 ('11111111-1111-1111-1111-111111111004', 'ACC-1004', 'Northwind Traders Ltd','GBP', 9000000,      0, 'ACTIVE', 0),
 ('11111111-1111-1111-1111-111111112001', 'ACC-2001', 'Dmitri Volkov',        'EUR',   80000,      0, 'ACTIVE', 0);
