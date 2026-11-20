# Card Authorization and Settlement (runnable example)

Part of **Banking** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/banking/card-authorization-and-settlement.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/banking/card-authorization-and-settlement.md)

## What this example shows
The issuer side of a card purchase, from the moment the network asks "approve or decline?" to the moment the
clearing record turns the hold into a posted statement line:

- `POST /api/authorizations` decides **approve (`00`) or decline** (`62` restricted card, `59` suspected fraud,
  `51` insufficient funds) and places a **hold** that reduces open-to-buy. A decline is a normal business answer
  (HTTP 200), not an error.
- **Idempotency on the network reference**: a network retry returns the original answer and never holds twice;
  reusing the reference for a different request is HTTP 409.
- `POST /api/authorizations/{id}/reversal` releases the hold; `POST /api/authorizations/{id}/clearing` posts the
  final amount. Clearing above the authorized amount is only allowed within a per-MCC tolerance
  (20% for restaurants, MCC 5812, to cover tips; illustrative value); otherwise HTTP 422.
- The `AuthorizationStatus` lifecycle (`APPROVED -> REVERSED | CLEARED`, `DECLINED` final) is enforced in the
  entity; clearing a reversed hold is HTTP 409.
- Events are published with `ApplicationEventPublisher` under the Kafka topic names used on the concept page
  (`card.authorization.approved`, `card.authorization.declined`, `card.authorization.reversed`,
  `card.transaction.cleared`).

Seed data: `tok_4111_0001` (Priya Raman, limit 5000, open hold of 80.00 at a bistro), `tok_4111_0002`
(Diego Alvarez, only 50.00 open-to-buy) and `tok_4111_0003` (Mei Tanaka, `BLOCKED`).

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/banking/card-authorization-and-settlement
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path: authorize a restaurant bill, then clear it with a tip.

```bash
curl -s -X POST localhost:8080/api/authorizations -H 'Content-Type: application/json' \
  -d '{"networkReference":"NET-20261119-000042","cardToken":"tok_4111_0001","merchantName":"Blue Fern Bistro","mcc":"5812","amount":64.30,"currency":"USD"}'
```

```json
{"id":"c3d04900-9fa9-436a-a9e6-75ae0b077f45","networkReference":"NET-20261119-000042","cardToken":"tok_4111_0001","merchantName":"Blue Fern Bistro","mcc":"5812","amount":64.30,"currency":"USD","status":"APPROVED","responseCode":"00","approvalCode":"725201","clearedAmount":null}
```

```bash
curl -s -X POST localhost:8080/api/authorizations/<id>/clearing -H 'Content-Type: application/json' -d '{"amount":75.00}'
```

```json
{"id":"c3d04900-9fa9-436a-a9e6-75ae0b077f45","networkReference":"NET-20261119-000042","cardToken":"tok_4111_0001","merchantName":"Blue Fern Bistro","mcc":"5812","amount":64.30,"currency":"USD","status":"CLEARED","responseCode":"00","approvalCode":"725201","clearedAmount":75.00}
```

Rule violation: the seeded 80.00 bistro hold cleared at 120.00 is above the 20% tolerance (HTTP 422).

```bash
curl -s -X POST localhost:8080/api/authorizations/5b1f2c3e-0000-4000-8000-000000000001/clearing \
  -H 'Content-Type: application/json' -d '{"amount":120.00}'
```

```json
{"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Clearing amount 120.00 exceeds authorized 80.00 plus tolerance for MCC 5812","instance":"/api/authorizations/5b1f2c3e-0000-4000-8000-000000000001/clearing"}
```

Decline (not an error): `tok_4111_0002` asking for 75.00 returns HTTP 200 with `"status":"DECLINED","responseCode":"51"`.
Check open-to-buy with `curl -s localhost:8080/api/cards/tok_4111_0001` (`availableCredit` 3720.00 at start,
3645.00 after the cleared 75.00). The `id` and `approvalCode` values differ per run.

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `CardAuthorizationAndSettlementApplication` | root | Spring Boot entry point |
| `CardAccount` | domain | Credit limit, posted balance, pending holds, open-to-buy; optimistic locking |
| `CardStatus` | domain | `ACTIVE` / `BLOCKED` |
| `Authorization` | domain | Authorization + hold; enforces lifecycle transitions |
| `AuthorizationStatus` | domain | Lifecycle enum and allowed next states |
| `CardEvent` | domain | Sealed event records named after Kafka topics |
| `CardAccountRepository`, `AuthorizationRepository` | repository | Spring Data JPA access |
| `AuthorizationService` | service | Approve/decline, hold, reverse, clear; idempotency and tolerance rules |
| `CardRuleException` | service | Rule violation with an HTTP status |
| `AuthorizationController`, `AuthorizationRequest`, `ClearingRequest`, `AuthorizationResponse`, `CardResponse` | api | REST endpoints and DTO records |
| `ApiExceptionHandler` | api | Maps errors to `ProblemDetail` |
| `FraudScorer`, `StubFraudScorer` | integration | Fraud-scoring port and deterministic stub |
| `CardEventListeners` | integration | `@EventListener` stand-ins for Kafka consumers |

## From example to production
| Here | Production on the configured stack |
|---|---|
| JSON over HTTP from a test client | ISO 8583 (or the network's API) over a dedicated, mutually authenticated link to Visa/Mastercard/other schemes, usually through an authorization switch |
| H2 in memory (`data.sql` seed) | PostgreSQL (`postgres` profile), schema via Flyway; holds and balances often also kept in a low-latency store |
| Clearing via `POST .../clearing` | Batch clearing files (for example Mastercard IPM, Visa Base II) parsed by a settlement job, matched to holds, with unmatched records sent to exceptions |
| `ApplicationEventPublisher` | Apache Kafka topics `card.*`, published through a transactional outbox, keyed by card token for ordering |
| `StubFraudScorer` | A real-time ML scoring service with a strict latency budget and a fallback rule set |
| No hold expiry | A scheduled job expiring stale holds after the scheme/issuer window (varies by network and MCC) |
| No stand-in processing | Network or processor stand-in (STIP) rules when the issuer host is unavailable, reconciled afterwards |
| Card token as a plain string | Tokens issued by a PCI DSS-scoped vault; PANs never reach this service |
| Single process | Services on Amazon EKS, secrets in AWS Secrets Manager / KMS, traces via OpenTelemetry to CloudWatch |

