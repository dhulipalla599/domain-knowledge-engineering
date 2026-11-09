# Batch ETL and Workflow Orchestration (runnable example)

Part of **Data Engineering** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/data-engineering/batch-etl-and-workflow-orchestration.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/data-engineering/batch-etl-and-workflow-orchestration.md)

## What this example shows
A small in-process batch ETL for a fictional retailer, Northwind Outfitters. A REST call (or a `@Scheduled` job, disabled by default) loads one business date: **extract** a CSV from `src/main/resources/sample-data/`, **transform** (type, validate, de-duplicate), **load** into the H2 table `sales_fact`, and record metrics on a `pipeline_run` row.

Rules enforced:
- A business date that already `SUCCEEDED` is never reloaded (409), and only one active run per date is allowed (409).
- A missing source file is a 404.
- Data quality gate: if more than 20% of rows are rejected the run ends `FAILED` and loads nothing.
- The load is an upsert by `order_id` in one transaction, so it is idempotent.

Run lifecycle: `PENDING -> EXTRACTING -> TRANSFORMING -> LOADING -> SUCCEEDED`, with `FAILED` reachable from any non-terminal state. Events `RunStarted`, `RunCompleted`, `RunFailed` (stand-ins for Kafka topics `etl.run.started`, `etl.run.completed`, `etl.run.failed`) are published with `ApplicationEventPublisher` and consumed with `@EventListener`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/data-engineering/batch-etl-and-workflow-orchestration
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path (`sales-2026-11-08.csv` has 10 rows, 1 with quantity 0):

```bash
curl -s -X POST localhost:8080/api/runs -H 'Content-Type: application/json' -d '{"businessDate":"2026-11-08"}'
```
```json
{"id":2,"businessDate":"2026-11-08","status":"SUCCEEDED","triggeredBy":"API","rowsExtracted":10,"rowsLoaded":9,"rowsRejected":1,"failureReason":null,"startedAt":"...","finishedAt":"..."}
```

Rule violation: run the same date again.

```bash
curl -s -X POST localhost:8080/api/runs -H 'Content-Type: application/json' -d '{"businessDate":"2026-11-08"}'
```
```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"Business date 2026-11-08 is already loaded","instance":"/api/runs"}
```

Data quality gate: `{"businessDate":"2026-11-09"}` returns 201 with `"status":"FAILED"`, `"rowsLoaded":0` and a `failureReason`. A date with no file (for example `2020-01-01`) returns 404. List runs with `GET /api/runs`.

To enable the nightly job: `mvn spring-boot:run -Dspring-boot.run.arguments=--etl.schedule.enabled=true`.

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `RunStatus` | domain | Lifecycle enum with legal transitions |
| `PipelineRun` | domain | Run entity, enforces transitions, holds run metrics |
| `SalesFact` | domain | Warehouse row, unique by `order_id` |
| `RawSale` | domain | Untyped source row |
| `PipelineEvents` | domain | `RunStarted`, `RunCompleted`, `RunFailed` events |
| `PipelineRunRepository`, `SalesFactRepository` | repository | Spring Data JPA |
| `PipelineService` | service | Extract/transform/load and the business rules |
| `PipelineException` | service | Rule violation carrying an HTTP status |
| `RunController`, `RunRequest`, `RunResponse` | api | REST endpoints and DTO records |
| `ApiExceptionHandler` | api | Maps errors to `ProblemDetail` |
| `SourceReader`, `ClasspathCsvSourceReader` | integration | Extract port and CSV adapter |
| `Notifier`, `LoggingNotifier` | integration | Alerting port and log stub |
| `RunEventListener` | integration | `@EventListener` consumers |
| `NightlyScheduler` | integration | `@Scheduled` job, off by default |

## From example to production
| Here | Production |
|---|---|
| H2 in memory (`postgres` profile available) | PostgreSQL (Amazon RDS or Aurora) for `pipeline_run`, `sales_fact` and a staging schema |
| Classpath CSV via `SourceReader` | S3 landing bucket (Parquet or CSV) read by an adapter, or a JDBC extract |
| `ApplicationEventPublisher` | Apache Kafka topics `etl.run.started`, `etl.run.completed`, `etl.run.failed` (key = business date, idempotent consumers) |
| `@Scheduled` job | An orchestrator such as Airflow (MWAA) or Step Functions with EventBridge, with retries and backfills |
| Synchronous run in the request thread | Run as a Kubernetes Job on Amazon EKS; the API only enqueues and returns 202 |
| `LoggingNotifier` | SNS to PagerDuty/Slack, plus OpenTelemetry metrics in CloudWatch |
| Active-run check by query | Unique partial index or advisory lock so two workers cannot race |
