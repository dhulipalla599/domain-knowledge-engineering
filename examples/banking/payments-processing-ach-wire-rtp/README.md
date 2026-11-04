# Payments Processing (ACH, Wire, RTP) (runnable example)

Part of **Banking** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/banking/payments-processing-ach-wire-rtp.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/banking/payments-processing-ach-wire-rtp.md)

## What this example shows
A customer payment order sent over one of three rails (ACH, Wire, RTP):

- Idempotent submission (`Idempotency-Key` header): same key and instruction returns the same payment, same key with a different instruction is `409`.
- Rail limits, sanctions screening and an available-balance check, each rejected with an RFC 9457 `ProblemDetail`.
- Funds are *held* first, then settled (or released if the network refuses).
- RTP and Wire settle immediately; ACH stays `SUBMITTED` until `/settle`, and can later be `RETURNED`.
- Lifecycle `RECEIVED -> VALIDATED -> SUBMITTED -> SETTLED -> RETURNED` (or `REJECTED`) enforced in the domain.
- Domain events published with `ApplicationEventPublisher` and consumed with `@EventListener`, named after Kafka topics (`payments.submitted`, `payments.settled`, `payments.rejected`, `payments.returned`).

Limits (ACH 250,000, Wire 10,000,000, RTP 100,000) are illustrative bank policy, not network rules.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/banking/payments-processing-ach-wire-rtp
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path (RTP settles immediately):

```bash
curl -s -X POST localhost:8080/api/payments -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-1' \
  -d '{"fromAccount":"ACC-1001","rail":"RTP","beneficiaryName":"Harbor Steel Supply","beneficiaryRouting":"021000021","beneficiaryAccount":"8800123456","amount":250.00}'
```

```json
{"id":"31467041-7838-4e80-b12a-ed0640f4fe4a","fromAccount":"ACC-1001","rail":"RTP","beneficiaryName":"Harbor Steel Supply","amount":250.00,"status":"SETTLED","failureReason":null,"createdAt":"2026-11-04T15:04:56.294Z"}
```

Rule violation (over the RTP limit, HTTP 422):

```bash
curl -s -X POST localhost:8080/api/payments -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-2' \
  -d '{"fromAccount":"ACC-1001","rail":"RTP","beneficiaryName":"Harbor Steel Supply","beneficiaryRouting":"021000021","beneficiaryAccount":"8800123456","amount":100000.01}'
```

```json
{"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Amount exceeds the RTP limit of 100000.00","instance":"/api/payments"}
```

Check the balance: `curl -s localhost:8080/api/accounts/ACC-1001` returns balance `49750.00` after the first call.
A beneficiary name containing `BLOCKED` fails sanctions screening; routing number `000000000` is refused by the stub network (payment ends `REJECTED`, hold released).

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `PaymentsProcessingAchWireRtpApplication` | root | Spring Boot entry point |
| `PaymentOrder` | domain | Payment entity; enforces lifecycle transitions |
| `PaymentStatus` | domain | Lifecycle enum and allowed next states |
| `Rail` | domain | ACH / WIRE / RTP with limit and settlement behaviour |
| `Account` | domain | Balance, held funds, optimistic locking |
| `PaymentEvent` | domain | Sealed event types named after Kafka topics |
| `AccountRepository`, `PaymentOrderRepository` | repository | Spring Data JPA access |
| `PaymentService` | service | Business rules and orchestration |
| `PaymentRuleException` | service | Rule violation with an HTTP status |
| `PaymentController`, `PaymentRequest`, `PaymentResponse`, `AccountResponse` | api | REST endpoints and DTO records |
| `ApiExceptionHandler` | api | Maps errors to `ProblemDetail` |
| `SanctionsScreener`, `StubSanctionsScreener` | integration | Screening port and deterministic stub |
| `PaymentNetwork`, `StubPaymentNetwork` | integration | Network port and deterministic stub |
| `PaymentEventListeners` | integration | `@EventListener` stand-ins for Kafka consumers |

## From example to production
| Here | Production on the configured stack |
|---|---|
| H2 in memory (`data.sql` seed) | PostgreSQL (`--spring.profiles.active=postgres`), schema via Flyway migrations |
| `ApplicationEventPublisher` | Apache Kafka topics `payments.*`, published through a transactional outbox so the DB write and event cannot diverge |
| `StubPaymentNetwork` | Adapters to the ACH operator, Fedwire and RTP, usually via a bank connectivity provider, with asynchronous status callbacks |
| `StubSanctionsScreener` | A screening service with fuzzy name matching and case management |
| Single process | Services on Amazon EKS, secrets in AWS Secrets Manager, traces via OpenTelemetry to CloudWatch |
| No authentication | OAuth2 / mTLS, per-customer limits, dual approval for large wires |
