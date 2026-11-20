# Insurance Eligibility Verification (runnable example)

Part of **Healthcare** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/healthcare/insurance-eligibility-verification.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/healthcare/insurance-eligibility-verification.md)

## What this example shows
A small real-time eligibility service, the kind a registration or scheduling system calls before a visit. Given a patient's coverage on file, a date of service and an X12 service type code, it asks the payer (through a stubbed clearinghouse) whether the patient is covered and stores the answer.

- **Statuses that mean different things:** `ACTIVE`, `INACTIVE` (payer answered, no coverage on that date), `REJECTED` (payer could not identify the member, AAA reject code such as `72`), and `PAYER_ERROR` (timeout, safe to retry).
- **"Active" is not enough:** coverage counts only if the payer says active *and* the date of service falls inside the plan begin/end dates.
- **Inquiry window:** dates of service more than 30 days ahead or 365 days back are rejected with `422` (real payer windows vary; this is configurable policy).
- **Real-time support:** payers that do not accept real-time inquiries are rejected with `422` and a hint to verify by phone or portal.
- **Fresh-answer reuse:** a completed `ACTIVE`/`INACTIVE` answer for the same coverage, date and service type within 24 hours is returned with `200` and `"reused": true` instead of paying for a second inquiry.
- **Lifecycle:** `PENDING -> ACTIVE | INACTIVE | REJECTED | PAYER_ERROR`, and `PAYER_ERROR -> PENDING` on retry. Retrying anything else returns `409`.
- **Events:** `eligibility.verified`, `eligibility.coverage-inactive` and `eligibility.check-failed` are published with `ApplicationEventPublisher` and consumed by `@EventListener` methods (stand-ins for Kafka topics).

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/healthcare/insurance-eligibility-verification
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Seeded coverages: `cov-1001` (active), `cov-1002` (terminated), `cov-1003` (mistyped member ID), `cov-1004` (payer offline), `cov-1005` (payer without real-time eligibility).

```bash
TODAY=$(date -u +%Y-%m-%d)
curl -i -X POST localhost:8080/api/eligibility-checks -H 'Content-Type: application/json' \
  -d "{\"coverageId\":\"cov-1001\",\"serviceDate\":\"$TODAY\",\"serviceTypeCode\":\"30\"}"
```

Expected: `201 Created` with a `Location` header and:

```json
{"id":"ad2a6fae-a388-401f-8de5-153eeb5aab2d","coverageId":"cov-1001","serviceDate":"2026-11-20","serviceTypeCode":"30","status":"ACTIVE","planBeginDate":"2026-01-01","planEndDate":"2026-12-31","copayAmount":30.00,"deductibleRemaining":450.00,"rejectReasonCode":null,"message":null,"completedAt":"2026-11-20T15:05:05.782260794Z","reused":false}
```

Send the same request again and you get `200 OK` with the same `id` and `"reused":true`.

Business-rule failure, a payer that does not support real-time eligibility:

```bash
curl -s -X POST localhost:8080/api/eligibility-checks -H 'Content-Type: application/json' \
  -d "{\"coverageId\":\"cov-1005\",\"serviceDate\":\"$TODAY\",\"serviceTypeCode\":\"30\"}"
```

Expected: `422 Unprocessable Entity`

```json
{"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Payer Smalltown Mutual Benefit does not support real-time eligibility; verify by phone or payer portal","instance":"/api/eligibility-checks"}
```

Other calls: `GET /api/eligibility-checks/{id}`, `GET /api/eligibility-checks?coverageId=cov-1001`, `POST /api/eligibility-checks/{id}/retry` (only for `PAYER_ERROR`, try `cov-1004`). The H2 console is at `/h2-console`.

## Project structure

| Class | Package | Responsibility |
|---|---|---|
| `EligibilityController` | `api` | REST endpoints; 201 for a new check, 200 for a reused answer |
| `EligibilityCheckRequest`, `EligibilityCheckResponse` | `api` | Request and response records |
| `ApiExceptionHandler` | `api` | Maps rule violations to 422, missing data to 404, illegal transitions to 409 (`ProblemDetail`) |
| `EligibilityService` | `service` | Inquiry window, real-time payer rule, fresh-answer reuse, interpreting the payer answer, retry, events |
| `EligibilityRuleException`, `NotFoundException` | `service` | Errors mapped to 422 and 404 |
| `EligibilityCheck` | `domain` | JPA entity; `transitionTo` enforces the lifecycle |
| `EligibilityStatus` | `domain` | Lifecycle enum and allowed transitions |
| `Payer`, `Coverage` | `domain` | Payer directory and the insurance a patient presented |
| `EligibilityEvents` | `domain` | Event records named after the Kafka topics |
| `PayerRepository`, `CoverageRepository`, `EligibilityCheckRepository` | `repository` | Spring Data JPA, including the fresh-answer query |
| `PayerGateway`, `StubPayerGateway` | `integration` | Clearinghouse port (270/271) and deterministic stub |
| `EligibilityEventListener` | `integration` | Logs what the worklist, financial counseling and retry consumers would do |
| `ClockConfig` | root | Single `Clock` so tests can pin "today" |

## From example to production
| Here | Production on the configured stack |
|---|---|
| H2 in memory, `ddl-auto: create-drop` | Amazon RDS for PostgreSQL with Flyway migrations; the raw 271 stored encrypted (it contains PHI) with a retention policy |
| `StubPayerGateway` | A clearinghouse adapter that builds X12 270 (HIPAA 005010X279A1) and parses 271 responses, or a payer API; timeouts, circuit breaker per payer, and payer-specific quirks |
| Payer call inside the database transaction | Save the `PENDING` check, call the payer outside the transaction, then record the answer, so a slow payer never holds a connection |
| `ApplicationEventPublisher` | Apache Kafka topics `eligibility.verified`, `eligibility.coverage-inactive`, `eligibility.check-failed`, keyed by coverage id, written through a transactional outbox |
| Manual retry endpoint | A scheduled consumer of `eligibility.check-failed` that retries `PAYER_ERROR` with backoff, plus a nightly batch re-check of tomorrow's appointments |
| Simple benefit fields | Full benefit parsing (EB segments per service type, in/out of network, coinsurance, out-of-pocket max) |
| No authentication | OIDC (for example Amazon Cognito), role-based access, audit log of every view of eligibility data |
| Local run | Container on Amazon EKS, Terraform for infrastructure, OpenTelemetry traces and metrics to CloudWatch |
