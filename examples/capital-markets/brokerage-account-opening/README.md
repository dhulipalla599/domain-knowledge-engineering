# Brokerage Account Opening (runnable example)

Part of **Capital Markets and Brokerage** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/capital-markets/brokerage-account-opening.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/capital-markets/brokerage-account-opening.md)

## What this example shows
A small Spring Boot service that takes a retail brokerage account application through its lifecycle:

1. **Submit** - validates eligibility (age 18+, margin accounts need liquid net worth of at least 25,000), is idempotent on the `Idempotency-Key` header, then runs identity verification and sanctions screening.
2. **Decide** - a clean application is `APPROVED`, a failed identity check is `REJECTED`, and a possible sanctions match goes to `PENDING_REVIEW` for a human.
3. **Review / activate** - a reviewer resolves `PENDING_REVIEW`; an `APPROVED` application can be activated and receives an account number.

Status changes are enforced inside the `AccountApplication` entity, and three domain events are published through Spring's `ApplicationEventPublisher` (stand-ins for Kafka topics): `brokerage.application.submitted`, `brokerage.application.decided`, `brokerage.account.activated`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/capital-markets/brokerage-account-opening
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path (HTTP 201, status `APPROVED`):

```bash
curl -s -X POST localhost:8080/api/v1/applications \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-001' \
  -d '{"applicantName":"Priya Raman","dateOfBirth":"1991-06-20","taxIdLast4":"9012","accountType":"MARGIN","liquidNetWorth":60000}'
```
```json
{"id":"2ef45f2d-...","applicantName":"Priya Raman","accountType":"MARGIN","liquidNetWorth":60000,"status":"APPROVED","decisionReason":"Automatic approval: KYC and screening passed","accountNumber":null}
```

Business-rule failure (HTTP 422, margin account with too little net worth):

```bash
curl -s -X POST localhost:8080/api/v1/applications \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-002' \
  -d '{"applicantName":"Tom Baker","dateOfBirth":"1980-01-01","taxIdLast4":"1111","accountType":"MARGIN","liquidNetWorth":5000}'
```
```json
{"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Margin accounts require liquid net worth of at least 25000","instance":"/api/v1/applications"}
```

Activate a seeded approved application:

```bash
curl -s -X POST localhost:8080/api/v1/applications/22222222-2222-2222-2222-222222222222/activate
```
```json
{"id":"22222222-2222-2222-2222-222222222222","applicantName":"Daniel Okafor","accountType":"MARGIN","liquidNetWorth":120000.00,"status":"ACTIVE","decisionReason":"Automatic approval: KYC and screening passed","accountNumber":"BRK-500001"}
```

Stub behaviour for experiments: a `taxIdLast4` of `0000` fails identity verification (`REJECTED`); an applicant name containing `Blocklisted` triggers `PENDING_REVIEW`. Replaying the same `Idempotency-Key` returns the original application with HTTP 200.

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `AccountApplication` | `domain` | JPA entity; owns the lifecycle and rejects illegal transitions |
| `ApplicationStatus` | `domain` | Lifecycle enum with allowed next states |
| `AccountType`, `AccountEvents` | `domain` | Account kinds; event records and topic names |
| `AccountApplicationRepository` | `repository` | Spring Data access, lookup by idempotency key |
| `EligibilityPolicy` | `service` | Pure age and margin net-worth rules |
| `AccountOpeningService` | `service` | Orchestrates submit, review, activate and publishes events |
| `AccountApplicationController` | `api` | REST endpoints under `/api/v1/applications` |
| `ApiExceptionHandler` | `api` | Maps rule violations to `ProblemDetail` (422, 404, 409) |
| `IdentityVerifier`, `SanctionsScreener` + stubs | `integration` | Ports and deterministic stand-ins for vendors |
| `AuditTrailListener`, `WelcomeNotificationListener` | `integration` | `@EventListener` consumers standing in for Kafka consumers |

## From example to production
| Here | Production |
|---|---|
| H2 in memory (`postgres` profile available) | Amazon RDS for PostgreSQL with Flyway migrations, not `ddl-auto` |
| Spring `ApplicationEventPublisher` | Apache Kafka topics with the same names, published through a transactional outbox |
| `StubIdentityVerifier`, `StubSanctionsScreener` | Adapters calling licensed KYC and sanctions-screening vendors, with timeouts, retries and a manual-review fallback |
| In-memory account number sequence | Database sequence or the clearing firm's account-number service |
| No authentication | OIDC/JWT on the API, role checks for the reviewer endpoint |
| Logging audit listener | Append-only audit store plus OpenTelemetry traces and CloudWatch metrics |
| Single JVM | Containers on Amazon EKS provisioned with Terraform |
