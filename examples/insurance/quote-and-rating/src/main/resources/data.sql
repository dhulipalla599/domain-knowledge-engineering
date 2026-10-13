INSERT INTO base_rate (state, annual_amount) VALUES ('CA', 1180.00);
INSERT INTO base_rate (state, annual_amount) VALUES ('TX', 1320.00);
INSERT INTO base_rate (state, annual_amount) VALUES ('OH', 940.00);
INSERT INTO base_rate (state, annual_amount) VALUES ('NY', 1450.00);

-- A quote that is ready to bind, and one already bound.
INSERT INTO quote (id, driver_name, driver_age, state, vehicle_type, prior_claims, premium, status, valid_until, bound_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'Maria Alvarez', 34, 'TX', 'SEDAN', 0, 1320.00, 'RATED',
        DATEADD('DAY', 30, CURRENT_TIMESTAMP), NULL);
INSERT INTO quote (id, driver_name, driver_age, state, vehicle_type, prior_claims, premium, status, valid_until, bound_at)
VALUES ('22222222-2222-2222-2222-222222222222', 'Sam Okafor', 52, 'OH', 'SUV', 1, 1297.20, 'BOUND',
        DATEADD('DAY', 20, CURRENT_TIMESTAMP), DATEADD('DAY', -5, CURRENT_TIMESTAMP));
