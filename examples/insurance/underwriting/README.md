# Underwriting (runnable example)

Part of **Insurance** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/insurance/underwriting.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/insurance/underwriting.md)

## What this example shows
A home-insurance application is scored by a pure rules engine and then auto-approved, auto-declined, or referred to a human underwriter. It demonstrates:

- a state machine (`SUBMITTED → APPROVED | REFERRED | DECLINED`, `REFERRED → APPROVED | DECLINED`) enforced inside the entity;
- an idempotent submission (`Idempotency-Key` header) so a retried request never creates a second application;
- an underwriter **authority limit**: approving a case above your signing limit is rejected with HTTP 422;
- domain events named after Kafka topics (`underwriting.application-submitted`, `underwriting.decision-made`), published with Spring's `ApplicationEventPublisher`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/insurance/underwriting
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Seed data: application 1 approved, 2 referred (Priya Nair, 900,000), 3 declined. Underwriters: `uw-junior` (limit 500,000), `uw-senior` (limit 2,000,000).

```bash
# Happy path: a clean, modest risk is approved automatically (HTTP 201)
curl -s -XPOST localhost:8080/api/applications -H 'Idempotency-Key: demo-1' \
  -H 'Content-Type: application/json' \
  -d '{"applicantName":"Jo Park","sumInsured":250000,"yearBuilt":2012,"floodZone":false}'
# {"id":4,"applicantName":"Jo Park","sumInsured":250000,"riskScore":10,"status":"APPROVED","decisionReason":"Within automatic acceptance appetite","decidedBy":"auto-rules"}

# Rule violation: a junior underwriter approves above their authority (HTTP 422)
curl -s -XPOST localhost:8080/api/applications/2/decision -H 'Content-Type: application/json' \
  -d '{"underwriterId":"uw-junior","approve":true,"reason":"ok"}'
# {"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Sum insured 900000.00 exceeds authority limit 500000 of uw-junior; escalate to a senior underwriter","instance":"/api/applications/2/decision"}

# The senior underwriter approves (HTTP 200); deciding again returns 409 Conflict
curl -s -XPOST localhost:8080/api/applications/2/decision -H 'Content-Type: application/json' \
  -d '{"underwriterId":"uw-senior","approve":true,"reason":"flood cover added"}'
```

Other endpoints: `GET /api/applications/{id}`, `GET /api/applications/referrals`, `GET /api/applications/events` (events the in-process "broker" has published). H2 console: `http://localhost:8080/h2-console`.

## Project structure
| Class | Package | What it does |
|---|---|---|
| `PolicyApplication` | `domain` | JPA entity; `moveTo` is the only way to change status |
| `ApplicationStatus` | `domain` | Lifecycle enum and legal transitions |
| `IllegalTransitionException` | `domain` | Thrown for an illegal move (HTTP 409) |
| `UnderwritingEvent` | `domain` | Sealed event type: `ApplicationSubmitted`, `DecisionMade` |
| `PolicyApplicationRepository` | `repository` | Spring Data JPA queries |
| `RiskScorer` | `service` | Pure scoring and accept/refer/decline rules |
| `UnderwritingService` | `service` | Orchestrates submit and decide, enforces authority, publishes events |
| `RuleViolationException` | `service` | Carries an HTTP status for a broken business rule |
| `UnderwritingController` | `api` | REST endpoints |
| `ApiExceptionHandler` | `api` | Maps exceptions to `ProblemDetail` |
| `SubmitApplicationRequest`, `DecisionRequest`, `ApplicationResponse` | `api` | DTO records |
| `ClaimsHistoryClient`, `StubClaimsHistoryClient` | `integration` | Port and stub for the claims bureau |
| `AuthorityDirectory`, `StubAuthorityDirectory` | `integration` | Port and stub for underwriter authority limits |
| `UnderwritingEventListener` | `integration` | Stand-in for Kafka publishing |

## From example to production
| Here | Production (PostgreSQL, Apache Kafka, AWS on Amazon EKS) |
|---|---|
| H2 in-memory, `ddl-auto: create-drop`, `data.sql` | PostgreSQL (Amazon RDS) with Flyway migrations; profile `postgres` shows the wiring |
| `@EventListener` in the same JVM | Transactional outbox table relayed to Apache Kafka (Amazon MSK), keyed by application id; idempotent consumers |
| `StubClaimsHistoryClient` | HTTP adapter to a claims-history bureau with timeouts, retries and a circuit breaker |
| `StubAuthorityDirectory` | Lookup against the company IAM/HR system, with Amazon Cognito or OIDC supplying the underwriter identity |
| `underwriterId` in the request body | Taken from the authenticated JWT, never from the payload |
| Idempotency by unique key in the same table | Same, plus key expiry and request-hash comparison |
| Logs only | OpenTelemetry traces and metrics exported to CloudWatch; immutable audit trail of every decision |
