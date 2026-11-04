# Subscriber Onboarding (runnable example)

Part of **Telecom** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/telecom/subscriber-onboarding.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/telecom/subscriber-onboarding.md)

## What this example shows
Onboarding a new mobile subscriber: an applicant submits ID details, a plan and the ICCID printed on a SIM card. The service checks eligibility, verifies identity through a port, reserves a phone number (MSISDN) from inventory, binds it to the SIM and activates the subscription. It demonstrates:

- an `Idempotency-Key` header so a retried request never creates a second subscription;
- rules that fail with a clear `ProblemDetail`: minimum age (422), maximum active lines per ID (422), SIM already assigned (409), failed identity check (422);
- a lifecycle enum (`SUBMITTED`, `VERIFIED`, `ACTIVE`, `REJECTED`, `CANCELLED`) whose legal transitions are enforced in the `Subscription` entity;
- domain events (`subscription.submitted`, `subscription.activated`, `subscription.rejected`) published with `ApplicationEventPublisher` and consumed by `@EventListener` beans, standing in for Apache Kafka topics;
- optimistic locking (`@Version`) on SIMs and numbers so two requests cannot claim the same resource.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/telecom/subscriber-onboarding
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Activate a line (SIM `8944000000000000001` is seeded as available):

```bash
curl -s -X POST localhost:8080/api/v1/subscriptions \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"fullName":"Lena Park","dateOfBirth":"1994-03-09","idDocumentNumber":"ID-550123","plan":"PREPAID_BASIC","iccid":"8944000000000000001"}'
```

Expected `201 Created` (the id and timestamp will differ; repeating the call with the same key returns the same subscription):

```json
{"id":"0b6f1c52-8f55-4c0e-9a57-6a3d1e0b7c11","status":"ACTIVE","plan":"PREPAID_BASIC","iccid":"8944000000000000001","msisdn":"+15550100002","activatedAt":"2026-10-19T15:12:03.114Z"}
```

Try the already-assigned SIM `8944000000000000099`:

```bash
curl -s -X POST localhost:8080/api/v1/subscriptions \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-2' \
  -d '{"fullName":"Lena Park","dateOfBirth":"1994-03-09","idDocumentNumber":"ID-550123","plan":"PREPAID_BASIC","iccid":"8944000000000000099"}'
```

Expected `409 Conflict`:

```json
{"type":"about:blank","title":"SIM already assigned","status":409,"detail":"SIM 8944000000000000099 is already in use","instance":"/api/v1/subscriptions"}
```

A document number starting with `FAKE` fails the stub identity check with `422`. Read the seeded subscription with `GET /api/v1/subscriptions/11111111-1111-1111-1111-111111111111`.

## Project structure
| Class | Package | What it does |
|---|---|---|
| `SubscriptionController` | `api` | REST endpoints: onboard, get, cancel |
| `OnboardingRequest`, `SubscriptionResponse` | `api` | Request/response records |
| `OnboardingExceptionHandler`, `OnboardingException` | `api` | Map rule violations to `ProblemDetail` |
| `OnboardingService` | `service` | Business rules and orchestration |
| `Subscription`, `SubscriptionStatus` | `domain` | Main entity and lifecycle with legal transitions |
| `SimCard`, `PhoneNumber`, `Plan` | `domain` | SIM and number inventory, plan catalogue |
| `OnboardingEvent` | `domain` | Sealed event types named after Kafka topics |
| `SubscriptionRepository`, `SimCardRepository`, `PhoneNumberRepository` | `repository` | Spring Data JPA |
| `IdentityVerifier`, `StubIdentityVerifier` | `integration` | KYC port and deterministic stub |
| `ProvisioningListener`, `NotificationListener` | `integration` | Event consumers (network provisioning/billing, SMS) |

## From example to production
- **Database:** H2 becomes PostgreSQL (`--spring.profiles.active=postgres`, e.g. Amazon RDS or Aurora) with Flyway migrations instead of `create-drop`, a unique index on `idempotency_key`, and number allocation using `SELECT ... FOR UPDATE SKIP LOCKED`.
- **Events:** `ApplicationEventPublisher` becomes Apache Kafka (Amazon MSK) topics keyed by subscription id, written through a transactional outbox so the database commit and the event cannot diverge. Consumers must be idempotent.
- **Identity verification:** the stub becomes an adapter to a national ID registry or KYC vendor, with timeouts, retries and a manual-review path.
- **Provisioning and billing:** the listeners become separate services calling the HSS/HLR and the billing platform.
- **Runtime:** containers on Amazon EKS, secrets in AWS Secrets Manager, OpenTelemetry traces and logs to CloudWatch.
