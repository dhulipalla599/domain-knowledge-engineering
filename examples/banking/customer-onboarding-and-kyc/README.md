# Customer Onboarding and KYC (runnable example)

Part of **Banking** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/banking/customer-onboarding-and-kyc.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/banking/customer-onboarding-and-kyc.md)

## What this example shows

A small onboarding service for the fictional Northwind Bank:

- An applicant submits an application with an `Idempotency-Key`, so a retried request never creates a second customer.
- Applicants under 18 are refused with a `422` problem response.
- Every application is screened against a (fictional) sanctions watchlist and given a risk rating.
- `OnboardingDecisionPolicy` decides: confirmed match → reject, possible match or high risk → analyst review, otherwise approve.
- Applicants only ever see `IN_PROGRESS`, `APPROVED` or `DECLINED`, never that a review exists (the "tipping off" rule).
- An analyst clears or confirms a review with a mandatory reason.
- Approval publishes `ApplicationApproved`; after the transaction commits, a listener opens a **restricted** account, idempotently.

## Run it

Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/banking/customer-onboarding-and-kyc
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the unit and API tests
```

## Try it

Happy path: a low-risk applicant is approved and gets a restricted account.

```bash
curl -s -X POST localhost:8080/api/v1/applications \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"legalName":"Priya Raman","dateOfBirth":"1991-04-12","nationality":"IN","occupation":"Software engineer"}'
# 202 {"applicationId":"…","status":"APPROVED","nextStep":"COMPLETE_WELCOME_STEPS","accountNumber":"NWB…"}
```

Run the same command again: you get the same `applicationId` back.

Business rule: a possible sanctions match waits for an analyst, and the applicant is not told why.

```bash
curl -s -X POST localhost:8080/api/v1/applications \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-2' \
  -d '{"legalName":"Viktor Petrov","dateOfBirth":"1988-02-20","nationality":"GB","occupation":"Teacher"}'
# 202 {"applicationId":"<id>","status":"IN_PROGRESS","nextStep":"WAIT_FOR_DECISION","accountNumber":null}

curl -s localhost:8080/api/v1/review-cases          # analyst queue (includes two seeded cases)

curl -s -X POST localhost:8080/api/v1/review-cases/<id>/decision \
  -H 'Content-Type: application/json' \
  -d '{"cleared":true,"reason":"Different date of birth from the listed person"}'
# 200 {"applicationId":"<id>","status":"APPROVED",…}
```

Business rule: minors are refused.

```bash
curl -s -X POST localhost:8080/api/v1/applications \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-3' \
  -d '{"legalName":"Sam Young","dateOfBirth":"2015-06-01","nationality":"GB","occupation":"Student"}'
# 422 {"title":"Business rule violated","code":"UNDER_MINIMUM_AGE",…}
```

Browse the tables at http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:banking-customer-onboarding-and-kyc`, user `sa`, no password).

## Project structure

| Class | Package | Responsibility |
|---|---|---|
| `ApplicationController` | `api` | REST endpoints for applicants and analysts |
| `ApplicationDtos` | `api` | Request/response records; customer view vs analyst view |
| `ApiExceptionHandler` | `api` | Maps rule violations to `422`, not found to `404`, illegal transitions to `409` |
| `OnboardingService` | `service` | Orchestrates submit → screen → rate → decide, and analyst review |
| `OnboardingDecisionPolicy` | `service` | **The key business rule**: approve, review or reject |
| `RiskRatingPolicy` | `service` | Simplified risk rating from country and occupation |
| `OnboardingApplication` | `domain` | Aggregate root; enforces legal status transitions |
| `ApplicationStatus` | `domain` | Lifecycle states, allowed transitions, customer-facing wording |
| `CustomerAccount` | `domain` | Account opened after approval, starts `RESTRICTED` |
| `OnboardingEvents` | `domain` | `ApplicationSubmitted`, `ScreeningCompleted`, `ApplicationApproved`, `ApplicationRejected` |
| `SanctionsScreeningClient` / `StubSanctionsScreeningClient` | `integration` | Port to the screening vendor and a deterministic stub |
| `AccountOpeningListener` | `integration` | Opens the account after commit; idempotent |
| `AuditTrailListener` | `integration` | Logs every decision with its inputs |

## From example to production

| In this example | In production (configured stack) |
|---|---|
| H2 in-memory database | PostgreSQL (try it: `docker compose up -d` then `mvn spring-boot:run -Dspring-boot.run.profiles=postgres`), schema managed by Flyway or Liquibase |
| Spring `ApplicationEventPublisher` + `@TransactionalEventListener` | Transactional outbox table relayed to Apache Kafka topics keyed by `applicationId` |
| `StubSanctionsScreeningClient` with a two-name watchlist | Screening vendor API, daily list updates and re-screening of existing customers |
| Synchronous screening inside the request | Asynchronous steps with timeouts and resumable state; the API returns `202` and the client polls |
| Two hard-coded risk rules | Versioned, documented risk model owned by compliance; rule version stored with every decision |
| Log-based audit | Append-only audit store with retention rules |
| `mvn spring-boot:run` | Container on Amazon EKS (see `Dockerfile`), deployed with Terraform, traced with OpenTelemetry |
