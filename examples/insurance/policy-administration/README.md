# Policy Administration (runnable example)

Part of **Insurance** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/insurance/policy-administration.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/insurance/policy-administration.md)

## What this example shows
A small policy-administration core: issue a policy from a bound quote, endorse it mid-term (pro-rata premium delta), cancel it (pro-rata refund) and expire it. It demonstrates:

- a `PolicyStatus` lifecycle (`ACTIVE` -> `CANCELLED` / `EXPIRED`) with transitions enforced in the `Policy` domain class;
- business rules in `PolicyService`: one policy per quote (409), endorsement date must fall inside the term (422), declined payment (402), no second cancellation (409);
- domain events published with `ApplicationEventPublisher` and consumed with `@EventListener`, standing in for Kafka topics `policy.issued`, `policy.endorsed`, `policy.cancelled`, `policy.expired`;
- errors returned as `ProblemDetail` from a `@RestControllerAdvice`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/insurance/policy-administration
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Two policies are seeded (ids 1 and 2). Policy 1 is active with an annual premium of 1,200.00.

Endorse policy 1 (raise the sum insured on 1 July 2026; 184 of 365 days remain):

```bash
curl -s -X POST localhost:8080/api/policies/1/endorsements -H 'Content-Type: application/json' \
  -d '{"newSumInsured":500000,"effectiveDate":"2026-07-01","reason":"Home extension"}'
```

```json
{"id":1,"policyNumber":"HM-2026-900001","quoteId":"Q-1001","holderName":"Priya Nair","sumInsured":500000,"annualPremium":1500.00,"inception":"2026-01-01","expiry":"2027-01-01","status":"ACTIVE","refundDue":null,"endorsements":[{"effectiveDate":"2026-07-01","newSumInsured":500000,"premiumDelta":151.23,"reason":"Home extension"}]}
```

Business-rule failure (effective date after the term ends) returns HTTP 422:

```bash
curl -s -X POST localhost:8080/api/policies/1/endorsements -H 'Content-Type: application/json' \
  -d '{"newSumInsured":500000,"effectiveDate":"2028-01-01","reason":"Late"}'
```

```json
{"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Effective date 2028-01-01 is outside the policy term 2026-01-01 to 2027-01-01","instance":"/api/policies/1/endorsements"}
```

Issue a new policy (returns 201; repeating the same `quoteId` returns 409):

```bash
curl -s -X POST localhost:8080/api/policies -H 'Content-Type: application/json' \
  -d '{"quoteId":"Q-2001","holderName":"Ana Silva","sumInsured":300000,"rate":0.003,"inception":"2026-12-01"}'
```

Other endpoints: `GET /api/policies`, `GET /api/policies/{id}`, `POST /api/policies/{id}/cancellation` and `POST /api/policies/{id}/expiry` (body `{"effectiveDate":"..."}`).

## Project structure
| Class | Package | What it does |
|---|---|---|
| `Policy` | `domain` | Aggregate: terms, premium, status, endorsement history, enforces legal transitions |
| `PolicyStatus` | `domain` | Lifecycle enum and allowed moves |
| `Endorsement` | `domain` | Record of a mid-term change and its premium delta |
| `PolicyEvent` | `domain` | Sealed event types, each naming its Kafka topic |
| `IllegalTransitionException` | `domain` | Raised on an illegal state change (HTTP 409) |
| `PolicyRepository` | `repository` | Spring Data JPA access |
| `PolicyService` | `service` | Business rules: issue, endorse, cancel, expire |
| `ProRata` | `service` | Daily pro-rata premium arithmetic |
| `RuleViolationException` | `service` | Rule failure carrying an HTTP status |
| `PolicyController` | `api` | REST endpoints |
| `ApiExceptionHandler` | `api` | Maps exceptions to `ProblemDetail` |
| `PaymentGateway` / `StubPaymentGateway` | `integration` | Payment port and deterministic stub |
| `PolicyEventListener` | `integration` | Stand-in for Kafka consumers |

## From example to production
| Here | Production on the configured stack |
|---|---|
| H2 in memory (`ddl-auto: create-drop`) | PostgreSQL (Amazon RDS or Aurora) with Flyway migrations; the `postgres` profile shows the datasource |
| `ApplicationEventPublisher` + `@EventListener` | Apache Kafka topics (`policy.issued` and so on) keyed by policy id, published through a transactional outbox so a rollback never emits an event |
| `StubPaymentGateway` | A real payment or billing adapter with idempotency keys |
| Refund based on the current annual premium | Billing tracks premium actually paid per period, so refunds reconcile with the ledger |
| No authentication | OIDC/JWT with role checks (underwriting, service agent, auditor) |
| Local process | Container on Amazon EKS with OpenTelemetry traces and CloudWatch logs and metrics |
