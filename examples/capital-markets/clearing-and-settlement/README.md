# Clearing and Settlement (runnable example)

Part of **Capital Markets and Brokerage** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/capital-markets/clearing-and-settlement.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/capital-markets/clearing-and-settlement.md)

## What this example shows
A small clearing and settlement core for US-style cash equities. Executed trades are captured, the settlement date is computed as T+1 business days, the trade is affirmed, and a settlement run moves shares and cash delivery-versus-payment (DvP) between depository positions. Trades the seller cannot deliver become settlement fails and are retried on the next run. Lifecycle: `PENDING_AFFIRMATION -> AFFIRMED -> SETTLED` (or `FAILED`, retried until `SETTLED`, or `CANCELLED`).

Rules enforced:

- **T+1 with a holiday calendar** - settlement date skips weekends and configured holidays (a trade on Wed 25 Nov 2026 settles Fri 27 Nov because 26 Nov is Thanksgiving).
- **Affirmation cutoff** - affirming after 9:00 pm New York time on trade date is rejected (HTTP 422, rule `AFFIRMATION_CUTOFF`) so the trade goes to the exceptions desk.
- **Trade date** - must be a business day and not in the future (HTTP 422, rule `TRADE_DATE`); buyer and seller must differ (rule `SAME_PARTY`).
- **Settlement run date** - only for a business day on or before today (HTTP 422, rule `SETTLEMENT_DATE`).
- **DvP** - shares and cash move together or not at all; a short seller produces `FAILED` with `INSUFFICIENT_SECURITIES`, a short buyer `INSUFFICIENT_CASH`.
- **Idempotency** - replaying a `tradeId` returns the original trade (HTTP 200); re-running a settlement date never settles a trade twice.
- **Lifecycle** - illegal moves (for example cancelling a settled trade) return HTTP 409.

Domain events (`TradeCaptured`, `TradeAffirmed`, `TradeSettled`, `SettlementFailed`) use Spring's `ApplicationEventPublisher`; they stand in for the Kafka topics `clearing.trade-captured.v1`, `clearing.trade-affirmed.v1`, `settlement.trade-settled.v1` and `settlement.trade-failed.v1`.

The clock is fixed at **Wed 25 Nov 2026, 09:00 New York** (`clearing.fixed-now` in `application.yml`) so the seeded data and the commands below always give the same answers. Set it to an empty value to use the real clock.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/capital-markets/clearing-and-settlement
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Seeded data: three clearing accounts (`NORTHWIND`, `BLUEHARBOR`, `REDCLIFF`) with share and USD positions, and three trades from Tue 24 Nov 2026 settling 25 Nov: `T-1001` (affirmed, deliverable), `T-1002` (affirmed, seller is short 400 GLBX) and `T-1003` (never affirmed).

```bash
# Net obligations for the settlement date (+ receive, - deliver/pay)
curl -s 'localhost:8080/api/obligations?settlementDate=2026-11-25'
# [{"accountId":"BLUEHARBOR","asset":"ACME","netQuantity":-1000},{"accountId":"BLUEHARBOR","asset":"USD","netQuantity":45100.00},
#  {"accountId":"NORTHWIND","asset":"ACME","netQuantity":1000},{"accountId":"NORTHWIND","asset":"GLBX","netQuantity":500},
#  {"accountId":"NORTHWIND","asset":"USD","netQuantity":-105100.00},{"accountId":"REDCLIFF","asset":"GLBX","netQuantity":-500},
#  {"accountId":"REDCLIFF","asset":"USD","netQuantity":60000.00}]

# Run settlement for 25 Nov: T-1001 settles DvP, T-1002 fails (seller short)
curl -s -XPOST localhost:8080/api/settlement-runs -H 'Content-Type: application/json' -d '{"settlementDate":"2026-11-25"}'
# {"settlementDate":"2026-11-25","settled":["T-1001"],"failed":["T-1002"]}

curl -s localhost:8080/api/trades/T-1002
# {"tradeId":"T-1002",...,"settlementDate":"2026-11-25","status":"FAILED","failReason":"INSUFFICIENT_SECURITIES"}

# Capture a new trade: T+1 skips Thanksgiving -> settles 2026-11-27 (201)
curl -s -XPOST localhost:8080/api/trades -H 'Content-Type: application/json' \
  -d '{"tradeId":"T-2001","buyerAccount":"REDCLIFF","sellerAccount":"BLUEHARBOR","symbol":"ACME","quantity":200,"price":45.30,"tradeDate":"2026-11-25"}'
# {"tradeId":"T-2001",...,"settlementAmount":9060.00,"tradeDate":"2026-11-25","settlementDate":"2026-11-27","status":"PENDING_AFFIRMATION","failReason":null}

# Business-rule failure: T-1003 missed the 9 pm ET affirmation cutoff -> 422
curl -s -XPOST localhost:8080/api/trades/T-1003/affirm
# {"type":"about:blank","title":"Clearing rule violated","status":422,"detail":"affirmation cutoff was 2026-11-24T21:00-05:00[America/New_York]; route trade T-1003 to the exceptions desk","instance":"/api/trades/T-1003/affirm","rule":"AFFIRMATION_CUTOFF"}
```

Other endpoints: `POST /api/trades/{tradeId}/affirm`, `POST /api/trades/{tradeId}/cancel`, `GET /api/accounts/{accountId}/positions`.

## Project structure

| Class | Package | Responsibility |
|---|---|---|
| `Trade` | `domain` | Entity; lifecycle methods `affirm`, `settle`, `fail`, `cancel`; `settlementAmount()` |
| `TradeStatus` | `domain` | Lifecycle enum with the legal transitions |
| `Position` | `domain` | Depository holding (shares or USD cash) with `covers`, `debit`, `credit` |
| `ClearingEvents` | `domain` | Event records (Kafka topic stand-ins) |
| `TradeRepository`, `PositionRepository` | `repository` | Spring Data JPA |
| `ClearingService` | `service` | Capture, affirmation cutoff, settlement run (DvP), net obligations |
| `SettlementCalendar` | `service` | Business days, T+1 settlement date, affirmation cutoff |
| `RuleViolationException`, `NotFoundException` | `service` | Business errors |
| `ClearingController`, `ApiModels` | `api` | REST endpoints and DTO records |
| `ApiExceptionHandler` | `api` | Maps errors to `ProblemDetail` |
| `DepositoryGateway`, `LedgerDepositoryGateway` | `integration` | Depository port and in-database stub |
| `ClearingEventListeners` | `integration` | Confirmation, books-and-records and fails-desk consumers (log only) |

## From example to production

| Here | Production |
|---|---|
| H2 in memory (`create-drop`) | PostgreSQL (`--spring.profiles.active=postgres`) with Flyway migrations, Amazon RDS / Aurora |
| `ApplicationEventPublisher` | Apache Kafka (Amazon MSK) topics keyed by `tradeId`, written through a transactional outbox |
| `LedgerDepositoryGateway` moves local positions | Adapter to the clearing broker / custodian and the CSD (DTC for US equities) using ISO 15022 or ISO 20022 settlement messages, with asynchronous settlement confirmations |
| Gross DvP trade by trade; netting is a report only | The CCP (NSCC CNS for US equities) nets obligations and the depository settles net positions; partial settlement and recycling of fails |
| Holiday list in `application.yml` | Market calendar reference-data service per market and currency |
| Late affirmations rejected | Exception queue and manual workflow; late-affirmed trades may still settle but lose straight-through processing |
| Fixed demo clock | Real clock; scheduled settlement batches per depository cycle |
| No authentication | OAuth2/OIDC at the gateway, maker-checker for cancels, audit log |
| Local JVM | Container on Amazon EKS, OpenTelemetry traces and metrics to CloudWatch |
