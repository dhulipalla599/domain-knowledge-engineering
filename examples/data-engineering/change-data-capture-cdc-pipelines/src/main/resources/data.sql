INSERT INTO customer_replica (customer_id, email, full_name, city, deleted, last_lsn, updated_at) VALUES
  (1, 'ana@harborpine.example', 'Ana Martins', 'Lisbon', FALSE, 100, CURRENT_TIMESTAMP),
  (2, 'ben.okafor@harborpine.example', 'Ben Okafor', 'Lagos', FALSE, 150, CURRENT_TIMESTAMP),
  (3, 'chloe.dubois@harborpine.example', 'Chloe Dubois', 'Lyon', FALSE, 120, CURRENT_TIMESTAMP);

INSERT INTO pipeline_run (source, status, triggered_by, started_at, finished_at, events_read, applied, skipped_stale, rejected)
VALUES ('customers-initial', 'SUCCEEDED', 'SCHEDULE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 3, 3, 0, 0);
