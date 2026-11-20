INSERT INTO payer (id, name, real_time_enabled) VALUES
  ('NORTHWIND', 'Northwind Health Plan', TRUE),
  ('BLUEHARBOR', 'Blue Harbor Health', TRUE),
  ('SMALLTOWN', 'Smalltown Mutual Benefit', FALSE);

INSERT INTO coverage (id, patient_id, payer_id, member_id, subscriber_first_name, subscriber_last_name, subscriber_date_of_birth) VALUES
  ('cov-1001', 'pat-maria',  'NORTHWIND',  'NWH884213307', 'Maria',  'Alvarez', DATE '1988-04-12'),
  ('cov-1002', 'pat-daniel', 'NORTHWIND',  'NWH551902118', 'Daniel', 'Okafor',  DATE '1975-09-30'),
  ('cov-1003', 'pat-priya',  'NORTHWIND',  'NWH55190211',  'Priya',  'Raman',   DATE '1992-01-05'),
  ('cov-1004', 'pat-tomas',  'BLUEHARBOR', 'BHP200417765', 'Tomas',  'Lindqvist', DATE '1969-11-21'),
  ('cov-1005', 'pat-grace',  'SMALLTOWN',  'SMB0042871',   'Grace',  'Whitfield', DATE '1957-06-14');
