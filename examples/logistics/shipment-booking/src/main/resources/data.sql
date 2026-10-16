INSERT INTO rate_quote (id, carrier, origin, destination, service_level, price, currency, max_weight_kg, accepts_dangerous_goods, valid_until) VALUES
('Q-1001', 'Northwind Freight', 'Rotterdam, NL', 'Chicago, US', 'FCL-20FT', 2450.00, 'USD', 18000, false, TIMESTAMP WITH TIME ZONE '2099-12-31 23:59:59+00'),
('Q-1002', 'Harbourline Chemicals Logistics', 'Antwerp, BE', 'Houston, US', 'LCL', 880.50, 'USD', 5000, true, TIMESTAMP WITH TIME ZONE '2099-12-31 23:59:59+00'),
('Q-1003', 'Northwind Freight', 'Rotterdam, NL', 'Chicago, US', 'FCL-40FT', 3900.00, 'USD', 26000, false, TIMESTAMP WITH TIME ZONE '2020-01-01 00:00:00+00'),
('Q-1004', 'OFFLINE-LINES', 'Hamburg, DE', 'Boston, US', 'LCL', 640.00, 'USD', 3000, false, TIMESTAMP WITH TIME ZONE '2099-12-31 23:59:59+00');
