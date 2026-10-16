# Product Catalog and Pricing (runnable example)

Part of **Retail** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/retail/product-catalog-and-pricing.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/retail/product-catalog-and-pricing.md)

## What this example shows
A small pricing service. A merchandiser asks to change a product's price. The service enforces two rules: the price must keep at least a 10% margin over cost (otherwise HTTP 422), and a move of more than 20% in either direction waits for a pricing manager's approval. Smaller moves apply immediately. A second change for a SKU that is already pending is rejected with 409. Every outcome publishes a domain event, using Spring events as stand-ins for Kafka topics.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/retail/product-catalog-and-pricing
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Apply a small change (79.99 to 84.00 is about 5%, so it is applied at once):

```bash
curl -s -XPOST localhost:8080/api/price-changes -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1001","newPrice":84.00,"reason":"Seasonal adjustment"}'
# {"id":1,"sku":"SKU-1001","oldPrice":79.99,"newPrice":84.00,"status":"APPLIED","reason":"Seasonal adjustment"}
```

Break the margin rule (cost 9.80, floor 10.78):

```bash
curl -s -XPOST localhost:8080/api/price-changes -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1004","newPrice":10.00,"reason":"Clearance"}'
# 422 {"title":"Unprocessable Entity","status":422,"detail":"Price 10.00 is below the margin floor 10.78 for SKU-1004",...}
```

A large change waits for approval, then is approved:

```bash
curl -s -XPOST localhost:8080/api/price-changes -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1002","newPrice":12.00,"reason":"Promo"}'
# {"id":2,...,"status":"PENDING_APPROVAL",...}
curl -s -XPOST localhost:8080/api/price-changes/2/approve
# {"id":2,...,"status":"APPLIED",...}
```

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `Product` | domain | Sellable item with cost and current price |
| `PriceChange` | domain | Change request; `moveTo` enforces legal lifecycle transitions |
| `PriceChangeStatus` | domain | `PENDING_APPROVAL`, `APPLIED`, `REJECTED` |
| `PricingEvents` | domain | Event records named after Kafka topics |
| `ProductRepository`, `PriceChangeRepository` | repository | Spring Data JPA |
| `PricingPolicy` | service | Margin floor and approval threshold rules |
| `PricingService` | service | Request, approve, reject; publishes events |
| `PricingException` | service | Business-rule errors |
| `PricingController`, DTO records | api | REST endpoints |
| `ApiExceptionHandler` | api | Maps errors to `ProblemDetail` |
| `PricingEventListeners`, `PriceIndexPort`, `InMemoryPriceIndex` | integration | Event consumers and a stubbed search index |

## From example to production
- **Database:** H2 becomes PostgreSQL (profile `postgres`), with Flyway migrations and optimistic locking on `Product`.
- **Events:** Spring events become Apache Kafka topics (`retail.price-change.requested`, `retail.price-change.rejected`, `retail.product-price.updated`) published through a transactional outbox; consumers must be idempotent.
- **Search index:** `InMemoryPriceIndex` becomes an OpenSearch adapter behind `PriceIndexPort`.
- **Security and cloud:** add authentication and roles (merchandiser vs pricing manager), run on Amazon EKS with Amazon RDS for PostgreSQL and Amazon MSK, and export traces with OpenTelemetry to CloudWatch.
