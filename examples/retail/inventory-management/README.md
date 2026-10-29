# Inventory Management (runnable example)

Part of **Retail** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/retail/inventory-management.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/retail/inventory-management.md)

## What this example shows
A stock reservation flow for an online retailer. A checkout reserves units of a SKU, which lowers *available* stock (`onHand - reserved`) without touching what is physically on the shelf. The reservation is later confirmed (stock ships, so `onHand` drops) or released (the hold is given back).

Business rules enforced:
- **No overselling:** a reservation larger than available stock is rejected with `409 Conflict`.
- **Idempotent reserve:** the same `orderRef` + `sku` returns the original reservation (`200`) instead of holding stock twice.
- **Legal lifecycle only:** `PENDING` can become `CONFIRMED`, `RELEASED` or `EXPIRED`; anything else is `409`.
- **Reorder point:** dropping to or below the reorder point publishes `inventory.low-stock`, which triggers a (stubbed) purchase request.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/retail/inventory-management
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Reserve 3 pairs of shoes (seeded with 12 on hand):

```bash
curl -i -X POST localhost:8080/api/reservations -H 'Content-Type: application/json' \
  -d '{"orderRef":"ORD-1001","sku":"SKU-1001","quantity":3}'
```
```
HTTP/1.1 201
{"id":"4bff2a8b-...","orderRef":"ORD-1001","sku":"SKU-1001","quantity":3,"status":"PENDING","expiresAt":"2026-10-29T15:29:15Z"}
```

Check stock, then confirm using the `id` returned above:

```bash
curl localhost:8080/api/stock/SKU-1001
# {"sku":"SKU-1001","name":"Trail Running Shoes, size 42","onHand":12,"reserved":3,"available":9,"reorderPoint":5}
curl -X POST localhost:8080/api/reservations/<id>/confirm
```

Rule violation: SKU-1003 only has 3 units.

```bash
curl -i -X POST localhost:8080/api/reservations -H 'Content-Type: application/json' \
  -d '{"orderRef":"ORD-1002","sku":"SKU-1003","quantity":9}'
```
```
HTTP/1.1 409
{"type":"about:blank","title":"Conflict","status":409,"detail":"Cannot reserve 9 of SKU-1003: only 3 available","instance":"/api/reservations"}
```

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `StockItem` | `domain` | Stock per SKU; `reserve`, `release`, `commit` enforce the no-oversell rule |
| `Reservation` | `domain` | Hold on stock; enforces legal state transitions |
| `ReservationStatus` | `domain` | Lifecycle enum with allowed next states |
| `InventoryEvent` | `domain` | Sealed event records named after Kafka topics |
| `DomainException` | `domain` | `NotFound`, `InsufficientStock`, `InvalidTransition` |
| `StockItemRepository`, `ReservationRepository` | `repository` | Spring Data JPA access |
| `InventoryService` | `service` | Reserve / confirm / release / expire use cases, publishes events |
| `InventoryController`, `InventoryDtos` | `api` | REST endpoints and request/response records |
| `ApiExceptionHandler` | `api` | Maps rule violations to `ProblemDetail` responses |
| `InventoryEventListeners` | `integration` | `@EventListener` stand-ins for Kafka consumers |
| `ReplenishmentGateway`, `StubReplenishmentGateway` | `integration` | Port and in-memory stub for purchasing |

## From example to production
| Here | Production |
|---|---|
| H2 in memory, `ddl-auto: create-drop` | PostgreSQL (`postgres` profile), schema managed by Flyway migrations |
| `ApplicationEventPublisher` | Apache Kafka topics (`inventory.reserved`, `inventory.confirmed`, `inventory.released`, `inventory.low-stock`) written via a transactional outbox, keyed by SKU |
| `StubReplenishmentGateway` | Adapter to the ERP / supplier purchase-order API |
| `expireOverdue` called manually | Scheduled job (Kubernetes CronJob or ShedLock-guarded `@Scheduled`) |
| Optimistic lock (`@Version`) on `StockItem` | Same, plus retry on conflict; hot SKUs may need sharded counters |
| No authentication | OIDC/JWT via an API gateway; service-to-service auth inside the cluster |
| Single process | Amazon EKS, Amazon RDS for PostgreSQL, Amazon MSK for Kafka, OpenTelemetry traces and metrics to CloudWatch |
