# Real-Time Streaming ETL with Kafka (runnable example)

Part of **Data Engineering** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/data-engineering/real-time-streaming-etl-with-kafka.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/data-engineering/real-time-streaming-etl-with-kafka.md)

## What this example shows
A fictional retailer, Harbor & Pine, receives order events as CSV lines. The pipeline runs in-process:
**extract** a file from `src/main/resources/sample-data/`, **transform** each line (upper-case codes, amount to minor units),
**validate**, and **load** into H2, recording metrics for every run.

Business rules enforced:
- Only one `RUNNING` run per source (HTTP 409 otherwise).
- A bad line never fails the batch: it is written to `rejected_record` (dead-letter) with the reason.
- Replays are idempotent: an `eventId` that is already loaded is counted as a duplicate and skipped.
- A run is a state machine: `RUNNING` -> `SUCCEEDED` / `COMPLETED_WITH_REJECTS` / `FAILED`; terminal states never change.

Spring `ApplicationEventPublisher` events stand in for Kafka topics: `OrdersRaw` (orders.raw), `OrdersCurated` (orders.curated),
`OrdersDlq` (orders.dlq) and `PipelineRunCompleted` (pipeline.run-completed).

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/data-engineering/real-time-streaming-etl-with-kafka
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

To run the pipeline on a timer instead, start with `--etl.schedule.enabled=true` (processes `orders-batch-2` every minute).

## Try it
Run batch 1 (10 lines: 4 valid, 2 duplicates, 4 invalid):

```bash
curl -s -X POST localhost:8080/api/pipeline-runs -H 'Content-Type: application/json' -d '{"source":"orders-batch-1"}'
```
```json
{"runId":2,"source":"orders-batch-1","status":"COMPLETED_WITH_REJECTS","recordsRead":10,"recordsLoaded":4,"duplicatesSkipped":2,"recordsRejected":4}
```

See the dead-lettered lines and the live revenue view (minor units per country):

```bash
curl -s localhost:8080/api/pipeline-runs/2/rejects
curl -s localhost:8080/api/revenue        # {"DE":3550,"GB":12000,"US":7790}
```

Business-rule failure (unknown source, HTTP 404 `ProblemDetail`):

```bash
curl -s -X POST localhost:8080/api/pipeline-runs -H 'Content-Type: application/json' -d '{"source":"nope"}'
```
```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"Unknown source: nope","instance":"/api/pipeline-runs"}
```

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `EtlController` | `api` | REST endpoints for runs, rejects and revenue |
| `ApiExceptionHandler` | `api` | Maps rule violations to `ProblemDetail` (404, 409, 400) |
| `RunRequest` / `RunResponse` | `api` | Request and response records |
| `StreamingEtlService` | `service` | Orchestrates extract, transform, validate, load and enforces the rules |
| `OrderRecordTransformer` | `service` | Parses and validates one CSV line into an `OrderFact` |
| `RecordSource` | `service` | Extract port (Kafka consumer in production) |
| `PipelineRun`, `OrderFact`, `RejectedRecord` | `domain` | JPA entities; `PipelineRun` enforces legal transitions |
| `RunStatus`, `DomainEvents` | `domain` | Lifecycle enum and the topic-named events |
| `*Repository` | `repository` | Spring Data repositories |
| `ClasspathRecordSource` | `integration` | Reads `sample-data/{source}.csv` |
| `RevenueByCountry`, `DeadLetterLogger` | `integration` | `@EventListener` consumers of the events |
| `ScheduledEtlRunner` | `integration` | `@Scheduled` trigger, disabled by default |

## From example to production
| Here | Production on the target stack |
|---|---|
| CSV file on the classpath | Kafka consumer group on `orders.raw` (Amazon MSK), manual offset commit after the load |
| `ApplicationEventPublisher` | Kafka producers and consumers on `orders.curated`, `orders.dlq`, `pipeline.run-completed`, keyed by `orderId` |
| H2 (in-memory) | PostgreSQL (Amazon RDS); activate the `postgres` profile, see `docker-compose.yml` |
| `existsByEventId` then insert | Insert with `ON CONFLICT (event_id) DO NOTHING` so concurrent consumers cannot race |
| One run per source check in code | A unique partial index or a lease, since several pods may start a run |
| `RevenueByCountry` in memory | A materialised table or a stream processor (Kafka Streams or Flink) |
| Logging dead letters | A real `orders.dlq` topic with alerting in CloudWatch, deployed on Amazon EKS |
