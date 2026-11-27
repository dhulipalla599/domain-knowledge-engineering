# Store Replenishment (runnable example)

Part of **Retail** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/retail/store-replenishment.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/retail/store-replenishment.md)

## What this example shows

A small store replenishment service for a fictional grocer, Harbourline Markets:

- A **replenishment run** (`POST /api/replenishment-runs`) applies a min/max (s, S) rule to every SKU in a store:
  when the inventory position (on-hand + on-order) is at or below the reorder point, it orders up to the target,
  rounded up to whole **case packs**, and backs off one case if that would overflow the **shelf capacity**.
- The run is **idempotent** per store, SKU and run date: re-running the same night returns the same orders.
- A store manager can key a **manual order**; it is rejected if it is not whole cases (422) or would overflow the shelf (409).
- Orders follow a lifecycle `PROPOSED -> RELEASED -> RECEIVED` (or `PROPOSED -> CANCELLED`), enforced in the entity.
- Releasing an order sends it to the distribution center (a stub gateway) and adds it to on-order stock;
  receiving records short deliveries.
- Domain events `replenishment.order-proposed`, `replenishment.order-released` and `replenishment.order-received`
  are published with Spring's `ApplicationEventPublisher` as stand-ins for Apache Kafka topics.

Seed data (`src/main/resources/data.sql`) loads stores `S-104` and `S-207` with milk, bread, eggs, coffee and bananas.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/retail/store-replenishment
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it

Run tonight's replenishment for store S-104 (MILK-2L and EGGS-12 are below their reorder points):

```bash
curl -s -X POST localhost:8080/api/replenishment-runs \
  -H 'Content-Type: application/json' \
  -d '{"storeId":"S-104","runDate":"2026-11-27"}'
```

```json
{"storeId":"S-104","runDate":"2026-11-27","created":2,"orders":[
  {"id":"<uuid>","storeId":"S-104","sku":"EGGS-12","quantity":30,"source":"AUTO","runDate":"2026-11-27","status":"PROPOSED","dcOrderRef":null,"receivedQuantity":null},
  {"id":"<uuid>","storeId":"S-104","sku":"MILK-2L","quantity":48,"source":"AUTO","runDate":"2026-11-27","status":"PROPOSED","dcOrderRef":null,"receivedQuantity":null}]}
```

Running it again returns HTTP 200 with `"created":0` and the same two orders.

Release and receive an order (use an `id` from above):

```bash
curl -s -X POST localhost:8080/api/orders/<id>/release
curl -s -X POST localhost:8080/api/orders/<id>/receive \
  -H 'Content-Type: application/json' -d '{"receivedQuantity":44}'
```

```json
{"id":"<id>","storeId":"S-104","sku":"MILK-2L","quantity":48,"source":"AUTO","runDate":"2026-11-27","status":"RECEIVED","dcOrderRef":"DC-EAST-1001","receivedQuantity":44}
```

Business-rule failures:

```bash
# not a whole number of cases (EGGS-12 ships in 15s) -> 422
curl -s -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"storeId":"S-104","sku":"EGGS-12","quantity":20}'
# {"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Quantity 20 of EGGS-12 is not a multiple of the case pack 15","instance":"/api/orders"}

# would overflow the shelf -> 409
curl -s -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"storeId":"S-207","sku":"MILK-2L","quantity":36}'
# {"type":"about:blank","title":"Conflict","status":409,"detail":"Ordering 36 of MILK-2L would raise the inventory position from 40 to 76, above shelf capacity 72","instance":"/api/orders"}
```

Check stock: `curl -s localhost:8080/api/stores/S-104/skus/MILK-2L`.

## Project structure

| Class | Package | Responsibility |
|---|---|---|
| `StoreSku` | `domain` | Stock and replenishment parameters for one SKU in one store; `suggestedQuantity()` and `checkOrderQuantity()` hold the core rules |
| `ReplenishmentOrder` | `domain` | Order to ship cases from the DC to a store; enforces lifecycle transitions |
| `OrderStatus` | `domain` | `PROPOSED`, `RELEASED`, `RECEIVED`, `CANCELLED` and the legal moves between them |
| `ReplenishmentEvent` | `domain` | Sealed interface of event records, one per Kafka topic |
| `DomainException` | `domain` | Sealed hierarchy of business-rule violations |
| `StoreSkuRepository`, `ReplenishmentOrderRepository` | `repository` | Spring Data JPA access |
| `ReplenishmentService` | `service` | Replenishment run, manual order, release, receive, cancel |
| `ReplenishmentController` | `api` | REST endpoints |
| `ReplenishmentDtos` | `api` | Request and response records |
| `ApiExceptionHandler` | `api` | Maps rule violations to `ProblemDetail` (404, 409, 422, 400) |
| `DistributionCenterGateway` | `integration` | Port to the distribution center's warehouse management system |
| `StubDistributionCenterGateway` | `integration` | Deterministic stub returning `DC-EAST-1001`, `DC-EAST-1002`, ... |
| `ReplenishmentEventListeners` | `integration` | `@EventListener` stand-ins for Kafka consumers; logs short deliveries |

## From example to production

| Here | In production |
|---|---|
| H2 in-memory database, `ddl-auto` | PostgreSQL (Amazon RDS or Aurora) with Flyway or Liquibase migrations; the unique key on `(store_id, sku, run_date)` stays |
| `ApplicationEventPublisher` + `@EventListener` | Apache Kafka (for example Amazon MSK) via a transactional outbox, keyed by `storeId:sku` so events for one shelf stay ordered |
| `StubDistributionCenterGateway` | An adapter to the DC's warehouse management system (REST, EDI or a Kafka topic), with retries and an idempotency key per order |
| Static reorder point and order-up-to level | Parameters recalculated from a demand forecast, lead time and service-level target by a planning service |
| Run triggered by HTTP | A scheduled job per store or region, ahead of the DC's order cut-off |
| Single process | Containers on Amazon EKS, provisioned with Terraform, traced with OpenTelemetry and monitored in Amazon CloudWatch |
