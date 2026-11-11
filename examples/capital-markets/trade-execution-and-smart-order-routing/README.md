# Trade Execution and Smart Order Routing (runnable example)

Part of **Capital Markets and Brokerage** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/capital-markets/trade-execution-and-smart-order-routing.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/capital-markets/trade-execution-and-smart-order-routing.md)

## What this example shows
A small smart order router (SOR) for US-style equities. A client sends a limit order (the *parent order*);
the service runs pre-trade checks, reads the top-of-book quote on every enabled venue, and splits the order
into *child orders*:

- sweep displayed liquidity **best price first**, breaking price ties by the **lowest take fee**;
- **never cross the client's limit price**; any remainder rests at the limit on the cheapest venue;
- track fills on each child and keep the parent's filled quantity and volume-weighted average price.

Business rules enforced (each returns an RFC 9457 `ProblemDetail`):

| Rule | HTTP | `rule` code |
|---|---|---|
| Limit more than 5% away from the NBBO midpoint (fat-finger collar) | 422 | `PRICE_COLLAR` |
| Symbol is halted | 422 | `SYMBOL_HALTED` |
| No venue quotes the symbol | 422 | `NO_MARKET_DATA` |
| `clientOrderId` already used (idempotency) | 409 | `DUPLICATE_CLIENT_ORDER_ID` |
| Illegal lifecycle move, e.g. cancelling a `FILLED` or `CANCELLED` order | 409 | - |

Parent order lifecycle: `NEW -> ROUTED -> PARTIALLY_FILLED -> FILLED`, with `CANCELLED` reachable from any
non-terminal state. Domain events are published with `ApplicationEventPublisher` and named after the Kafka
topics on the concept page: `execution.order-routed`, `execution.fill-reported`, `execution.order-rejected`,
`execution.order-cancelled`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/capital-markets/trade-execution-and-smart-order-routing
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Market data is a deterministic stub. `ACME` is quoted on four venues (best ask 100.02 on BATS 200 and XNAS 300,
then ARCX 100.03 x 500, IEXG 100.05 x 1000). `HLTD` is halted. Order `1` is a seeded, filled `GLBX` order.

```bash
# Consolidated book the router sees
curl -s localhost:8080/api/market/ACME/quotes

# Happy path: buy 800 ACME, limit 100.04 -> split across BATS, XNAS, ARCX and fully filled
curl -s -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"clientOrderId":"T-100","accountId":"ACC-7781","symbol":"ACME","side":"BUY","quantity":800,"limitPrice":100.04}'
```

Expected (201 Created):

```json
{"id":2,"clientOrderId":"T-100","symbol":"ACME","side":"BUY","quantity":800,"limitPrice":100.04,
 "filledQuantity":800,"leavesQuantity":0,"averagePrice":100.0238,"status":"FILLED",
 "childOrders":[
  {"id":3,"venue":"BATS","quantity":200,"limitPrice":100.02,"filledQuantity":200,"fillPrice":100.02,"status":"FILLED"},
  {"id":4,"venue":"XNAS","quantity":300,"limitPrice":100.02,"filledQuantity":300,"fillPrice":100.02,"status":"FILLED"},
  {"id":5,"venue":"ARCX","quantity":300,"limitPrice":100.03,"filledQuantity":300,"fillPrice":100.03,"status":"FILLED"}]}
```

```bash
# Rule violation: fat-finger limit far from the market
curl -s -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"clientOrderId":"T-102","accountId":"ACC-7781","symbol":"ACME","side":"BUY","quantity":100,"limitPrice":120.00}'
```

Expected (422 Unprocessable Entity):

```json
{"type":"about:blank","title":"Order rejected by pre-trade check","status":422,
 "detail":"Limit 120.00 is 19.99% from NBBO midpoint 100.0100 (collar 5%)","instance":"/api/orders","rule":"PRICE_COLLAR"}
```

```bash
# Partial fill: limit 100.02 only reaches 500 shares; 200 rest on IEXG. Then cancel the rest.
curl -s -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"clientOrderId":"T-101","accountId":"ACC-7781","symbol":"ACME","side":"BUY","quantity":700,"limitPrice":100.02}'
curl -s -X POST localhost:8080/api/orders/3/cancel     # -> "status":"CANCELLED"; again -> 409
```

## Project structure
Base package `com.dke.capital_markets.trade_execution_and_smart_order_routing`.

| Class | Package | Responsibility |
|---|---|---|
| `ParentOrder` | `domain` | Client order; enforces lifecycle transitions, applies fills, computes average price |
| `ChildOrder` | `domain` | One slice sent to one venue (`RESTING`, `FILLED`, `CANCELLED`) |
| `OrderStatus`, `Side` | `domain` | Lifecycle enum with `canMoveTo`, and buy/sell |
| `Venue` | `domain` | Execution venue (MIC code, take fee, enabled flag) |
| `ExecutionEvents` | `domain` | Event records: `OrderRouted`, `FillReported`, `OrderRejected`, `OrderCancelled` |
| `ParentOrderRepository`, `VenueRepository` | `repository` | Spring Data JPA repositories |
| `SmartOrderRouter` | `service` | Pure routing algorithm: price, then fee, never through the limit |
| `ExecutionService` | `service` | Pre-trade checks, routing, sending child orders, publishing events |
| `BusinessRuleException` | `service` | Rejection with a stable `rule` code |
| `ExecutionController`, `ExecutionDtos` | `api` | REST endpoints and request/response records |
| `ApiExceptionHandler` | `api` | Maps exceptions to `ProblemDetail` (404, 409, 422) |
| `MarketDataProvider`, `StubMarketDataProvider` | `integration` | Venue quotes and halts (stub) |
| `VenueGateway`, `StubVenueGateway` | `integration` | Sends child orders to venues (stub fills marketable orders) |
| `ExecutionEventListeners` | `integration` | Stand-in Kafka consumers: audit trail, post-trade, TCA |

## From example to production
| Here | In production |
|---|---|
| H2 in memory | PostgreSQL (Amazon RDS / Aurora); run locally with the `postgres` profile and `docker compose up -d` |
| `ApplicationEventPublisher` + `@EventListener` | Apache Kafka (Amazon MSK) topics with the same names, keyed by order id, written through a transactional outbox |
| `StubMarketDataProvider` | Direct exchange feeds and/or the SIP via a low-latency market data handler; halts from the SIP / primary listing exchange |
| `StubVenueGateway` (instant fills) | One FIX session per venue (often via a FIX engine such as QuickFIX/J); fills arrive asynchronously as `ExecutionReport` (35=8) messages |
| Synchronous route-and-fill in one HTTP call | Order accepted quickly (`ROUTED`), fills applied as execution reports arrive, client notified by events or streaming |
| Top-of-book only, one routing pass | Depth of book, venue fill probabilities, latency, rebates, odd-lot and midpoint/dark venues, re-routing of unfilled residuals |
| Fixed 5% collar, fictional fees | Collars and limits per instrument price band and per client, fee schedules loaded from venue notices; US broker-dealers must also meet SEC Rule 15c3-5 (Market Access Rule) controls |
| Runs as a single JVM | Containers on Amazon EKS, provisioned with Terraform, traced with OpenTelemetry and monitored in CloudWatch |
