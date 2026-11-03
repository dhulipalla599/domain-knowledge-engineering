-- Two sample PNRs: one ON_HOLD (ticketing limit 24h from start-up) and one already TICKETED.
INSERT INTO pnr (record_locator, status, contact_email, ticket_time_limit)
VALUES ('K7M2QP', 'ON_HOLD', 'ana.silva@example.com', DATEADD('HOUR', 24, CURRENT_TIMESTAMP));
INSERT INTO pnr_passenger (pnr_id, first_name, last_name) SELECT id, 'ANA', 'SILVA' FROM pnr WHERE record_locator = 'K7M2QP';
INSERT INTO pnr_passenger (pnr_id, first_name, last_name) SELECT id, 'LUCAS', 'SILVA' FROM pnr WHERE record_locator = 'K7M2QP';
INSERT INTO pnr_segment (pnr_id, flight_number, departure_date, origin, destination)
SELECT id, 'SK101', DATEADD('DAY', 30, CURRENT_DATE), 'LIS', 'AMS' FROM pnr WHERE record_locator = 'K7M2QP';

INSERT INTO pnr (record_locator, status, contact_email, ticket_time_limit)
VALUES ('R4T9WZ', 'TICKETED', 'omar.haddad@example.com', DATEADD('HOUR', 24, CURRENT_TIMESTAMP));
INSERT INTO pnr_passenger (pnr_id, first_name, last_name) SELECT id, 'OMAR', 'HADDAD' FROM pnr WHERE record_locator = 'R4T9WZ';
INSERT INTO pnr_segment (pnr_id, flight_number, departure_date, origin, destination)
SELECT id, 'SK220', DATEADD('DAY', 45, CURRENT_DATE), 'AMS', 'JFK' FROM pnr WHERE record_locator = 'R4T9WZ';
INSERT INTO pnr_ticket (pnr_id, ticket_number) SELECT id, '176-R4T9WZ-1' FROM pnr WHERE record_locator = 'R4T9WZ';
