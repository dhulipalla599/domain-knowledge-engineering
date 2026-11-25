# Data Quality and Validation (runnable example)

Part of **Data Engineering** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/data-engineering/data-quality-and-validation.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/data-engineering/data-quality-and-validation.md)

## What this example shows
A small in-process data quality pipeline for a fictional insurer, Brightwater Mutual, which receives a daily claims file from a third-party administrator (TPA). A REST call (or a `@Scheduled` job, disabled by default) validates one business date: **extract** `tpa-claims-<date>.csv` from `src/main/resources/sample-data/`, check the **schema contract**, run **row-level rules**, apply a **quality gate**, then **load** the good rows into the H2 table `claim`. Every run records its metrics in `validation_run`, and every failed check is kept in `rule_violation` (the quarantine).

Rules enforced:
- **Idempotency:** the same file (same SHA-256 checksum) cannot be processed twice unless the earlier run `FAILED` (409).
- **Schema contract:** the header must be exactly `claim_id,policy_number,loss_date,claim_amount,currency`, otherwise the run ends `FAILED` and cannot be released.
- **Row rules (ERROR, row rejected):** `CLAIM_ID_REQUIRED`, `CLAIM_ID_UNIQUE`, `POLICY_NUMBER_FORMAT` (`POL-nnnnnn`), `LOSS_DATE_VALID` (ISO date, not after the file date), `AMOUNT_POSITIVE` (> 0, at most 2 decimals), `CURRENCY_ALLOWED` (GBP, EUR, USD).
- **Row rule (WARN, row kept):** `HIGH_AMOUNT` above 250,000.
- **Quality gate:** if more than 10% of rows are rejected, or the file is empty, the run is `QUARANTINED` and nothing is loaded.
- **Steward release:** only a `QUARANTINED` run can be released (409 otherwise), and the release needs an approver and a reason of at least 10 characters (400). Release loads the valid rows only, after re-checking the file checksum.
- **Upsert by `claim_id`**, so loading a claim twice never duplicates it.

Run lifecycle: `RECEIVED -> VALIDATING -> LOADED | QUARANTINED | FAILED`, `RECEIVED -> FAILED`, and `QUARANTINED -> LOADED` on release. Events `BatchLoaded`, `BatchQuarantined` and `BatchFailed` (stand-ins for Kafka topics `dq.batch.loaded`, `dq.batch.quarantined`, `dq.batch.failed`) are published with `ApplicationEventPublisher` and consumed with `@EventListener`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/data-engineering/data-quality-and-validation
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path. `tpa-claims-2026-11-23.csv` has 12 rows: one negative amount (rejected) and one claim above 250,000 (warning). 1 of 12 = 8.3%, under the 10% gate:

```bash
curl -s -X POST localhost:8080/api/runs -H 'Content-Type: application/json' -d '{"businessDate":"2026-11-23"}'
```
```json
{"id":2,"fileName":"tpa-claims-2026-11-23.csv","businessDate":"2026-11-23","status":"LOADED","rowsReceived":12,"rowsLoaded":11,"rowsRejected":1,"rowsWarned":1,"errorRatio":0.0833,"statusReason":null,"releasedBy":null,"receivedAt":"...","finishedAt":"..."}
```

Rule violation: send the same file again.

```bash
curl -s -X POST localhost:8080/api/runs -H 'Content-Type: application/json' -d '{"businessDate":"2026-11-23"}'
```
```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"File tpa-claims-2026-11-23.csv was already processed by run 2","instance":"/api/runs"}
```

Quality gate: `{"businessDate":"2026-11-24"}` returns 201 with `"status":"QUARANTINED"`, `"rowsRejected":4`, `"errorRatio":0.4` and `"statusReason":"Quality gate: 4 of 10 rows failed (40.0% > 10.0%)"`. See why with `GET /api/runs/3/violations`, then release it:

```bash
curl -s -X POST localhost:8080/api/runs/3/release -H 'Content-Type: application/json' \
  -d '{"approvedBy":"m.okafor","reason":"TPA confirmed the 4 bad rows will be resent"}'
```
```json
{"id":3,"fileName":"tpa-claims-2026-11-24.csv","businessDate":"2026-11-24","status":"LOADED","rowsReceived":10,"rowsLoaded":6,"rowsRejected":4,"rowsWarned":0,"errorRatio":0.4,"statusReason":"Released by m.okafor: TPA confirmed the 4 bad rows will be resent","releasedBy":"m.okafor","receivedAt":"...","finishedAt":"..."}
```

Schema drift: `{"businessDate":"2026-11-25"}` (column renamed to `amount`) returns 201 with `"status":"FAILED"`. A date with no file (for example `2020-01-01`) returns 404. List runs with `GET /api/runs`.

To enable the morning job: `mvn spring-boot:run -Dspring-boot.run.arguments=--dq.schedule.enabled=true`.

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `RunStatus` | domain | Lifecycle enum with legal transitions |
| `ValidationRun` | domain | Run entity: enforces transitions, holds quality metrics |
| `RawClaim` | domain | Untyped source row with its line number |
| `Severity` | domain | `ERROR` (reject row) or `WARN` (keep row) |
| `RuleViolation` | domain | One failed check on one row (the quarantine) |
| `Claim` | domain | Trusted claim row, keyed by `claim_id` |
| `QualityEvents` | domain | `BatchLoaded`, `BatchQuarantined`, `BatchFailed` events |
| `ValidationRunRepository`, `RuleViolationRepository`, `ClaimRepository` | repository | Spring Data JPA |
| `ClaimValidator` | service | Row-level rules, pure logic |
| `ValidationService` | service | Idempotency, schema check, quality gate, load, release |
| `QualityException` | service | Rule violation carrying an HTTP status |
| `ValidationController`, `ValidateRequest`, `ReleaseRequest`, `RunResponse` | api | REST endpoints and DTO records |
| `ApiExceptionHandler` | api | Maps errors to `ProblemDetail` |
| `BatchSource`, `ClasspathBatchSource` | integration | Extract port and classpath CSV adapter (computes SHA-256) |
| `QualityEventListener` | integration | `@EventListener` consumers |
| `DailyFeedScheduler` | integration | `@Scheduled` job, off by default |

## From example to production
| Here | Production |
|---|---|
| H2 in memory (`postgres` profile available) | PostgreSQL (Amazon RDS or Aurora) for `validation_run`, `rule_violation`, `claim`, plus a rules table so stewards can tune thresholds without a deploy |
| Classpath CSV via `BatchSource` | Files land in an Amazon S3 bucket (SFTP via AWS Transfer Family); an S3 event triggers the run; the adapter streams from S3 |
| Hand-written rules in `ClaimValidator` | The same idea as declarative checks (for example a Great Expectations suite, dbt tests or Deequ) versioned in Git |
| `ApplicationEventPublisher` | Apache Kafka topics `dq.batch.loaded`, `dq.batch.quarantined`, `dq.batch.failed` (key = file checksum), written through a transactional outbox |
| `@Scheduled` job | S3 event notification or an orchestrator (Airflow on MWAA, Step Functions) with retries |
| Synchronous run in the request thread | A Kubernetes Job or worker on Amazon EKS; the API returns 202 and the client polls |
| Checksum check by query | Unique index on `(checksum)` for non-failed runs, so two workers cannot race |
| `QualityEventListener` logs | SNS or PagerDuty alert to the data steward, and OpenTelemetry metrics (error ratio, rows rejected) in CloudWatch |
