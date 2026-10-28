# Order Management System (runnable example)

Part of **Capital Markets and Brokerage** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/capital-markets/order-management-system.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/capital-markets/order-management-system.md)

## What this example shows
A small order management core: a client places a limit order, pre-trade checks run, cash is reserved for buys, the order is routed to a (stub) venue, and execution reports move it through `ACCEPTED -> PARTIALLY_FILLED -> FILLED` (or `CANCELED`). Rules enforced:

- **Idempotency** - replaying the same `accountId` + `clientOrderId` returns the original order (HTTP 200) instead of creating a duplicate.
- **Tick size** - limit prices must be positive multiples of 0.01 (HTTP 422, rule `TICK_SIZE`).
- **Order size** - quantity between 1 and 100,000 (HTTP 422, rule `MAX_ORDER_SIZE`).
- **Buying power** - a buy needs `quantity x limit price` available; it is reserved on accept (HTTP 422, rule `BUYING_POWER`).
- **Lifecycle** - illegal moves (cancel a filled order, over-fill) return HTTP 409.

Domain events (`OrderAccepted`, `ExecutionReported`, `OrderCanceled`) use Spring's `ApplicationEventPublisher`; they stand in for the Kafka topics `oms.order.accepted.v1`, `oms.order.execution.v1` and `oms.order.canceled.v1`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/capital-markets/order-management-system
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Two accounts are seeded: `ACC-1001` (50,000.00 buying power) and `ACC-1002` (1,000.00).

```bash
# Place a limit order -> 201
curl -s -XPOST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"accountId":"ACC-1001","clientOrderId":"web-1","symbol":"ACME","side":"BUY","quantity":100,"limitPrice":25.50}'
# {"id":1,"accountId":"ACC-1001","clientOrderId":"web-1","symbol":"ACME","side":"BUY","quantity":100,"limitPrice":25.50,"filledQuantity":0,"status":"ACCEPTED"}

# Venue reports a partial fill (price improvement is released back to the account)
curl -s -XPOST localhost:8080/api/orders/1/executions -H 'Content-Type: application/json' -d '{"quantity":40,"price":25.40}'
# {"id":1,...,"filledQuantity":40,"status":"PARTIALLY_FILLED"}

# Business-rule failure: ACC-1002 cannot afford 50,000.00 -> 422
curl -s -XPOST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"accountId":"ACC-1002","clientOrderId":"web-2","symbol":"ACME","side":"BUY","quantity":1000,"limitPrice":50.00}'
# {"type":"about:blank","title":"Order rejected by pre-trade check","status":422,"detail":"order needs 50000.00 but buying power is 1000.00","instance":"/api/orders","rule":"BUYING_POWER"}
```

Other endpoints: `POST /api/orders/{id}/cancel`, `GET /api/orders/{id}`, `GET /api/orders?accountId=ACC-1001`.

## Project structure

| Class | Package | Responsibility |
|---|---|---|
| `Order` | `domain` | Entity; lifecycle methods `accept`, `applyFill`, `cancel`, `reject` |
| `OrderStatus` | `domain` | Lifecycle enum with the legal transitions |
| `Account` | `domain` | Buying power and cash reservation |
| `Side` | `domain` | `BUY` / `SELL` |
| `OrderEvents` | `domain` | Event records (Kafka topic stand-ins) |
| `OrderRepository`, `AccountRepository` | `repository` | Spring Data JPA |
| `OrderService` | `service` | Pre-trade rules, executions, cancels, event publishing |
| `RuleViolationException`, `NotFoundException` | `service` | Business errors |
| `OrderController`, `OrderRequests` | `api` | REST endpoints and DTO records |
| `ApiExceptionHandler` | `api` | Maps errors to `ProblemDetail` |
| `ExecutionVenue`, `StubExecutionVenue` | `integration` | Venue port and in-memory stub |
| `OrderEventListeners` | `integration` | Router and audit-trail consumers |

## From example to production

| Here | Production |
|---|---|
| H2 in memory (`create-drop`) | PostgreSQL (`--spring.profiles.active=postgres`) with Flyway migrations |
| `ApplicationEventPublisher` | Apache Kafka topics keyed by `orderId` (transactional outbox so DB write and publish cannot diverge) |
| `StubExecutionVenue` | FIX session adapter to a broker or exchange gateway |
| Executions posted over REST | Venue execution reports consumed from Kafka, de-duplicated by execution id |
| Sell orders skip position checks | Position and short-sale checks against a positions service |
| No authentication | OAuth2/OIDC at the gateway, per-account authorization |
| Local JVM | Container on Amazon EKS, Amazon RDS for PostgreSQL, Amazon MSK, OpenTelemetry traces to CloudWatch |
