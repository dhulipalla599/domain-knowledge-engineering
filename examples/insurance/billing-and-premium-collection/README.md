# Billing and Premium Collection (runnable example)

Part of **Insurance** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/insurance/billing-and-premium-collection.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/insurance/billing-and-premium-collection.md)

## What this example shows
A premium installment billing service. Each policy's premium is split into **installments** that move through
`BILLED → PARTIALLY_PAID → PAID`, or `OVERDUE → DEFAULTED` when the policyholder does not pay before the grace period ends.

It implements:

- **Payment collection** against an installment through a payment gateway port (stubbed in memory).
- **Idempotency**: every payment needs an `Idempotency-Key` header. A retry with the same key returns the original
  payment (HTTP 200, `"replayed": true`) and never charges twice; reusing a key for a different amount is rejected (422).
- **No overpayment**: a payment larger than the outstanding premium is rejected (422).
- **Lifecycle enforcement**: paying a `PAID` or `DEFAULTED` installment is rejected (409). Declined cards return 402.
- **Overdue run**: a daily job marks past-due installments `OVERDUE` (grace period starts) and past-grace ones `DEFAULTED`.
- **Domain events** named after the Kafka topics: `premium.payment.received`, `premium.payment.failed`,
  `premium.installment.overdue`, `premium.installment.defaulted`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/insurance/billing-and-premium-collection
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Seed data (see `src/main/resources/data.sql`): installment 1 is billed (100.00), 2 is overdue (85.50),
3 is paid, 4 is billed (64.20).

```bash
# 1. Pay installment 1 in full -> 201 Created
curl -s -X POST localhost:8080/api/installments/1/payments \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: 7f3c9a10-pay-0001' \
  -d '{"amount":100.00,"paymentToken":"tok_visa"}'
```

```json
{"paymentId":2,"installmentId":1,"policyNumber":"HM-2026-900001","amount":100.00,"gatewayReference":"PSP-655849d3","receivedAt":"2026-11-25T15:05:21.091187721Z","installmentStatus":"PAID","outstanding":0.00,"replayed":false}
```

Send the same request again: you get **200 OK** with the same `paymentId` and `"replayed":true`. No second charge.

```bash
# 2. Overpay installment 2 (outstanding 85.50) -> 422 Unprocessable Entity
curl -s -X POST localhost:8080/api/installments/2/payments \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: 7f3c9a10-pay-0002' \
  -d '{"amount":100.00,"paymentToken":"tok_visa"}'
```

```json
{"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Payment 100.00 exceeds outstanding premium 85.50","instance":"/api/installments/2/payments"}
```

Other calls:

```bash
curl -s 'localhost:8080/api/installments?policyNumber=HM-2026-900001'      # list installments
curl -s -X POST 'localhost:8080/api/billing-runs/overdue?asOf=2026-12-10'  # {"asOf":"2026-12-10","markedOverdue":1,"markedDefaulted":1}
# paymentToken "tok_insufficient_funds" makes the stub gateway decline -> 402
```

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `Installment` | `domain` | Entity: amount due/paid, due date, grace end; enforces legal status transitions |
| `InstallmentStatus` | `domain` | Lifecycle enum and `canMoveTo` rules |
| `Payment` | `domain` | Captured payment with a unique idempotency key |
| `BillingEvent` | `domain` | Sealed interface of domain events, one per Kafka topic |
| `IllegalTransitionException` | `domain` | Illegal lifecycle move (mapped to 409) |
| `InstallmentRepository`, `PaymentRepository` | `repository` | Spring Data JPA repositories |
| `BillingService` | `service` | `recordPayment` (idempotency, payable, no overpayment, gateway) and `runOverdueCheck` |
| `BillingRuleException` | `service` | Business-rule failure carrying an HTTP status |
| `BillingController` | `api` | REST endpoints |
| `PaymentRequest`, `PaymentResponse`, `InstallmentResponse` | `api` | Request/response records |
| `ApiExceptionHandler` | `api` | Maps exceptions to RFC 9457 `ProblemDetail` |
| `PaymentGateway`, `StubPaymentGateway` | `integration` | Payment provider port and deterministic stub |
| `BillingEventListener` | `integration` | Stand-in for Kafka consumers (ledger, notifications, policy admin) |

## From example to production
| In this example | In production (PostgreSQL, Apache Kafka, AWS EKS) |
|---|---|
| H2 in memory, `ddl-auto` schema | PostgreSQL (Amazon RDS/Aurora) with Flyway migrations; unique index on `payment.idempotency_key`; run with the `postgres` profile |
| `ApplicationEventPublisher` + `@EventListener` | Transactional outbox table relayed to Apache Kafka (Amazon MSK) topics keyed by policy number |
| `StubPaymentGateway` | Adapter for a real payment service provider (card tokenisation, direct debit mandates such as SEPA or ACH), passing the idempotency key through and reconciling via provider webhooks/settlement files |
| `POST /api/billing-runs/overdue` called by hand | Scheduled Kubernetes CronJob (or EventBridge Scheduler) per billing date, with grace periods configured per product and jurisdiction |
| Log line in the listener | Consumers that post to the general ledger, send receipts/reminders, and ask policy administration to lapse or cancel for non-payment |
| No auth | OAuth2/OIDC at the API gateway; card data never touches this service (PCI DSS scope reduction via tokens) |
