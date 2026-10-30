# Warehouse Management (runnable example)

Part of **Logistics** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/logistics/warehouse-management.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/logistics/warehouse-management.md)

## What this example shows
The inbound half of a warehouse: an advance shipping notice (ASN) becomes a `Receipt`, the dock counts the goods (**receive**), and the goods are stored in a bin (**put away**), which updates stock and tells the ERP. Rules enforced: over-receipt above 10% is rejected (422), put-away needs a counted receipt (409), a bin must have free capacity (409), and hazardous goods only go into certified bins (422). Domain events are published with `ApplicationEventPublisher` and consumed with `@EventListener`, standing in for Kafka topics.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/logistics/warehouse-management
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Seed data: receipts 1-3 (EXPECTED), bins `A-01-01` (380 free), `A-01-02` (10 free), `H-01-01` (hazmat certified).

```bash
# Happy path: count 200 headphones, then store them in A-01-01
curl -s -X POST localhost:8080/api/receipts/1/receive -H 'Content-Type: application/json' -d '{"countedQty":200}'
# {"id":1,"asnNumber":"ASN-1001","sku":"SKU-HEADPHONES","expectedQty":200,"receivedQty":200,"status":"RECEIVED","binCode":null}
curl -s -X POST localhost:8080/api/receipts/1/putaway -H 'Content-Type: application/json' -d '{"binCode":"A-01-01"}'
# {"id":1,...,"status":"PUT_AWAY","binCode":"A-01-01"}

# Rule violation: 80 counted against an ASN of 50 (max accepted is 55)
curl -s -X POST localhost:8080/api/receipts/3/receive -H 'Content-Type: application/json' -d '{"countedQty":80}'
# {"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Over-receipt: counted 80 but at most 55 accepted for ASN ASN-1003","instance":"/api/receipts/3/receive"}
```

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `Receipt`, `ReceiptStatus` | domain | Inbound receipt, lifecycle and over-receipt rule |
| `Bin` | domain | Capacity and hazmat certification rule |
| `StockLevel` | domain | On-hand quantity per SKU and bin |
| `WarehouseEvents` | domain | `GoodsReceived`, `GoodsPutAway` events |
| `WarehouseException` | domain | Business-rule violation with a `Kind` |
| `*Repository` (3) | repository | Spring Data JPA access |
| `WarehouseService` | service | Orchestrates receive and put-away, publishes events |
| `WarehouseController`, DTO records, `ApiExceptionHandler` | api | REST endpoints and `ProblemDetail` errors |
| `WarehouseEventListeners`, `ErpNotifier`, `InMemoryErpNotifier` | integration | Event consumers and the ERP port with an in-memory stub |

## From example to production
- **Database:** H2 becomes PostgreSQL (`--spring.profiles.active=postgres`), with Flyway migrations instead of `create-drop` and optimistic locking (`@Version`) on `Bin` so concurrent put-aways cannot overbook it.
- **Events:** `ApplicationEventPublisher` becomes Apache Kafka topics `warehouse.goods-received` and `warehouse.goods-put-away`, published through a transactional outbox, keyed by receipt id, with idempotent consumers.
- **ERP:** `InMemoryErpNotifier` is replaced by an adapter calling the real ERP or consuming the Kafka topic.
- **AWS:** runs on Amazon EKS with Amazon RDS for PostgreSQL and Amazon MSK; OpenTelemetry traces and CloudWatch metrics and logs.
