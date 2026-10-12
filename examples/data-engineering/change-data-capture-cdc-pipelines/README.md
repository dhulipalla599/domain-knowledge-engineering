# Change Data Capture (CDC) Pipelines (runnable example)

Part of **Data Engineering** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/data-engineering/change-data-capture-cdc-pipelines.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/data-engineering/change-data-capture-cdc-pipelines.md)

## What this example shows
A small in-process CDC pipeline for the fictional retailer Harbor & Pine. It reads row-level change events (insert, update, delete) from `src/main/resources/sample-data/customers-batch-1.json`, validates and orders them by log sequence number (LSN), and loads them into a `customer_replica` table in H2. Every run is recorded in `pipeline_run` with its metrics, and invalid events go to a `rejected_event` dead-letter table.

Rules enforced: malformed events are rejected (not dropped), stale or duplicate events (LSN not newer than the row's last applied LSN) are skipped so replays are idempotent, deletes are soft, only one run may be active at a time (409), and source names are restricted to `[a-z0-9-]+` (400).


## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/data-engineering/change-data-capture-cdc-pipelines
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
```bash
curl -s -X POST localhost:8080/api/pipeline/runs -H 'Content-Type: application/json' \
  -d '{"source":"customers-batch-1"}'
```

Expected (HTTP 201): 7 events read, 3 applied, 2 skipped as stale or duplicate, 2 rejected.

```json
{"id":2,"source":"customers-batch-1","status":"SUCCEEDED","triggeredBy":"API","eventsRead":7,"applied":3,"skippedStale":2,"rejected":2,"errorMessage":null}
```

Run it again: it is idempotent, so `applied` is 0 and `skippedStale` is 5. Then look at the dead letters and the replica:

```bash
curl -s localhost:8080/api/pipeline/runs/2/rejected
curl -s localhost:8080/api/replica/customers
```

A rule violation (HTTP 400, `ProblemDetail`):

```bash
curl -s -X POST localhost:8080/api/pipeline/runs -H 'Content-Type: application/json' -d '{"source":"../x"}'
# {"type":"about:blank","title":"Bad Request","status":400,"detail":"source must contain only a-z, 0-9 and '-'","instance":"/api/pipeline/runs"}
```

An unknown source returns 404, and triggering while another run is `RUNNING` returns 409.

The scheduled job is off by default; start with `--cdc.schedule.enabled=true` to enable it (cron and source are in `application.yml`).


## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `ChangeDataCaptureCdcPipelinesApplication` | root | Spring Boot entry point |
| `PipelineRun`, `RunStatus` | domain | Run entity and its lifecycle (`PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`) |
| `CustomerReplica` | domain | Target row, tracks `lastLsn` and soft delete |
| `RejectedEvent` | domain | Dead-letter record |
| `ChangeEvent` | domain | One change from the log |
| `DomainEvents` | domain | `CustomerChangeApplied`, `PipelineRunCompleted` |
| `PipelineRunRepository`, `CustomerReplicaRepository`, `RejectedEventRepository` | repository | Spring Data JPA |
| `ReplicationPolicy` | service | Core rules: reject, skip stale, apply |
| `CdcPipelineService` | service | Extract, validate, load, metrics, events |
| `ChangeLogSource` | service | Port for the change log |
| `CdcController`, `RunRequest`, `RunResponse`, `ApiExceptionHandler` | api | REST API and `ProblemDetail` errors |
| `ClasspathChangeLogSource` | integration | Reads the JSON file |
| `ScheduledCdcRunner` | integration | Optional `@Scheduled` trigger |
| `AnalyticsEventListeners` | integration | `@EventListener` stand-ins for Kafka consumers |


## From example to production
| Here | Production on the target stack |
|---|---|
| JSON file via `ChangeLogSource` | Debezium reading the PostgreSQL write-ahead log (logical replication) and publishing to Apache Kafka (MSK), with a consumer implementing the same port |
| `lsn` in the file | The real PostgreSQL LSN or Kafka offset carried in the event |
| `ApplicationEventPublisher` | Kafka producers for `cdc.customers.applied` and `cdc.pipeline.completed`, keyed by `customerId`, ideally via a transactional outbox |
| H2 | PostgreSQL (`postgres` profile) or a warehouse such as Amazon Redshift or S3 plus Iceberg |
| `@Scheduled` job | A continuous consumer on Amazon EKS, or a Kubernetes CronJob for batch catch-up |
| Check-then-run guard for concurrent runs | A database advisory lock or Kafka consumer-group ownership, so two pods cannot race |
| Log output | OpenTelemetry metrics and traces exported to CloudWatch |

