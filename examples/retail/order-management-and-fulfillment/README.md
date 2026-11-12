# Order Management and Fulfillment (runnable example)

Part of **Retail** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/retail/order-management-and-fulfillment.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/retail/order-management-and-fulfillment.md)

## What this example shows
A small order management system (OMS) for "Larkspur Home", a fictional omnichannel homeware retailer:

- **Order capture** with an `Idempotency-Key` header, so a retried checkout returns the original order (200) instead of creating a second one.
- **Server-side pricing and purchase limits**: prices come from the `product` table, never from the client; `TOAST-2` is limited to 2 per order.
- **Sourcing**: `SourcingService` picks the fulfillment node (DC or ship-from-store) for each line. Nodes in the ship-to region come first, a single node that can ship the whole order beats a split, a line is never split across nodes, and inactive nodes are ignored.
- **Payment authorisation before reservation**: a declined card (`tok_declined`) never holds stock. Capture happens at shipment.
- **Inventory reservation** against available-to-promise (`on_hand - reserved`), consumed when the order ships and released when it is cancelled.
- **Order lifecycle** `CREATED -> ALLOCATED -> SHIPPED -> DELIVERED`, with `CANCELLED` allowed only before shipment, enforced in `OrderStatus.canMoveTo`.
- **Domain events** (`order.created`, `order.allocated`, `order.shipped`, `order.delivered`, `order.cancelled`) published with `ApplicationEventPublisher` and consumed by `@EventListener` methods that stand in for Kafka consumers.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/retail/order-management-and-fulfillment
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Responses below are from a fresh start (the seed data contains order `R100001`, so the first new order is `R100002`).

Place an order for the east region. The Brooklyn store is the only node that holds both items, so it ships the whole order:

```bash
curl -s -X POST localhost:8080/api/orders \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: chk-7f3a' \
  -d '{"customerId":"CUST-4410","shipToRegion":"EAST","paymentToken":"tok_visa",
       "lines":[{"sku":"KTL-200","quantity":2},{"sku":"FRY-28","quantity":1}]}'
```
```json
{"orderNumber":"R100002","status":"ALLOCATED","customerId":"CUST-4410","totalAmount":133.98,"currency":"USD",
 "lines":[{"sku":"KTL-200","quantity":2,"unitPrice":39.99,"fulfillmentNode":"STORE-114","trackingNumber":null},
          {"sku":"FRY-28","quantity":1,"unitPrice":54.00,"fulfillmentNode":"STORE-114","trackingNumber":null}]}
```
Run the same command again: you get the same order back with HTTP 200 instead of 201.

Ship it, then try to cancel it:

```bash
curl -s -X POST localhost:8080/api/orders/R100002/ship
# {"orderNumber":"R100002","status":"SHIPPED",...,"trackingNumber":"TRK-STORE-114-R100002"}...

curl -s -X POST localhost:8080/api/orders/R100002/cancel
```
```json
{"type":"about:blank","title":"Illegal order state change","status":409,
 "detail":"Order R100002 cannot move from SHIPPED to CANCELLED","instance":"/api/orders/R100002/cancel"}
```

Break the purchase limit:

```bash
curl -s -X POST localhost:8080/api/orders \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: chk-7f3b' \
  -d '{"customerId":"CUST-4411","shipToRegion":"EAST","paymentToken":"tok_visa",
       "lines":[{"sku":"TOAST-2","quantity":3}]}'
```
```json
{"type":"about:blank","title":"Order rejected","status":422,"detail":"At most 2 x TOAST-2 per order",
 "instance":"/api/orders","rule":"PURCHASE_LIMIT"}
```

Other things to try: `"paymentToken":"tok_declined"` (402 `PAYMENT_DECLINED`), `{"sku":"DOV-LTD","quantity":2}` (409 `INSUFFICIENT_STOCK`, because the Denver store is inactive), `GET /api/inventory/FRY-28` to watch reservations, and `GET /api/orders/R100001` for the seeded order.

## Project structure
| Class | Package | What it does |
|---|---|---|
| `OrderController` | `api` | REST endpoints: place, get, ship, deliver, cancel, inventory lookup |
| `OrderDtos` | `api` | Request/response records and bean validation |
| `ApiExceptionHandler` | `api` | Maps rule violations to `ProblemDetail` (404, 409, 422, 402) |
| `CustomerOrder`, `OrderLine` | `domain` | The order aggregate; lifecycle transitions live here |
| `OrderStatus` | `domain` | Lifecycle enum with `canMoveTo` |
| `Product`, `FulfillmentNode`, `InventoryPosition` | `domain` | Catalogue price and limits, ship-from locations, stock per node |
| `OrderEvents` | `domain` | Event records, each with its Kafka topic name |
| `*Repository` | `repository` | Spring Data JPA repositories |
| `OrderService` | `service` | Idempotency, limits, pricing, payment, reservation, ship, cancel |
| `SourcingService` | `service` | Pure node-selection rule (unit tested) |
| `BusinessRuleException` | `service` | Rule code + HTTP status for a rejected request |
| `PaymentGateway` / `StubPaymentGateway` | `integration` | Authorise, capture, void (stub declines `tok_declined`) |
| `CarrierGateway` / `StubCarrierGateway` | `integration` | Buys a shipping label, returns a tracking number |
| `OrderEventListeners` | `integration` | Stand-ins for the Kafka consumers; keeps an audit trail for tests |

## From example to production
| In this example | In production (PostgreSQL, Apache Kafka, AWS) |
|---|---|
| H2 in memory, `ddl-auto` | Amazon RDS / Aurora PostgreSQL, schema managed by Flyway or Liquibase. Run with `-Dspring-boot.run.profiles=postgres` and `docker compose up -d` to try PostgreSQL locally. |
| `@EventListener` in the same transaction | Transactional outbox table relayed to Apache Kafka (Amazon MSK) with a CDC tool such as Debezium, keyed by order number so events for one order stay in order. Consumers deduplicate on event id. |
| `StubPaymentGateway` | A payment service provider SDK (card authorisation, capture on shipment, void on cancel, refunds), with webhooks for asynchronous results and authorisation-expiry handling. |
| `StubCarrierGateway` | A carrier or multi-carrier label API; tracking events arrive back via webhook and drive `deliver`. |
| Sourcing reads stock from the OMS database | A dedicated inventory / availability service with near-real-time feeds from WMS and store systems, safety stock per node and capacity limits per store. |
| Optimistic locking on `InventoryPosition` | Same idea, plus retry on conflict, or atomic `UPDATE ... SET reserved = reserved + ? WHERE on_hand - reserved >= ?`. |
| One process | Separate order, inventory and fulfillment services on Amazon EKS, traced end to end with OpenTelemetry and CloudWatch. |
| Prices without tax or shipping | Tax engine and shipping-charge calculation at checkout, stored on the order as charged. |
