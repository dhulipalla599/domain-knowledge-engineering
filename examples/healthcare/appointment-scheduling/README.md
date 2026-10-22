# Appointment Scheduling (runnable example)

Part of **Healthcare** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/healthcare/appointment-scheduling.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/healthcare/appointment-scheduling.md)

## What this example shows
A small clinic booking service. A patient books a fixed-length slot with a provider, and the service enforces the rules that most often break naive schedulers:

- **No double booking:** an active appointment for the same provider that overlaps the slot is rejected with `409 Conflict`.
- **Booking rules:** at least one hour of lead time, start times aligned to the provider's slot length, and verified insurance coverage (`422 Unprocessable Entity`).
- **Idempotent retries:** repeating a request with the same `Idempotency-Key` returns the original appointment (`200`) instead of creating a second one.
- **Lifecycle:** `BOOKED -> CHECKED_IN -> COMPLETED`, or `BOOKED -> CANCELLED / NO_SHOW`. Illegal transitions return `409`.
- **Events:** `appointment.booked`, `appointment.cancelled` and `appointment.checked-in` are published with Spring's `ApplicationEventPublisher` and consumed by `@EventListener` methods (stand-ins for Kafka topics).

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/healthcare/appointment-scheduling
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Book Dr. Chen for a slot three days from now (the start time must be on a 30-minute boundary):

```bash
START=$(date -u -d '+3 days' +%Y-%m-%dT09:00:00Z)   # on macOS: date -u -v+3d +%Y-%m-%dT09:00:00Z
curl -i -X POST localhost:8080/api/appointments \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d "{\"patientId\":\"pat-amara\",\"providerId\":\"prov-chen\",\"startTime\":\"$START\",\"reason\":\"Annual checkup\"}"
```

Expected: `201 Created` with a `Location` header and a body like:

```json
{"id":"03ca631f-c76e-4f8b-8beb-b6c3972c5d95","patientId":"pat-amara","providerId":"prov-chen","startTime":"2026-10-25T09:00:00Z","endTime":"2026-10-25T09:30:00Z","status":"BOOKED","reason":"Annual checkup"}
```

Now try to book the same slot for a different patient (business-rule failure):

```bash
curl -s -X POST localhost:8080/api/appointments -H 'Content-Type: application/json' \
  -d "{\"patientId\":\"pat-jonas\",\"providerId\":\"prov-chen\",\"startTime\":\"$START\"}"
```

Expected: `409 Conflict`

```json
{"type":"about:blank","title":"Conflict","status":409,"detail":"Provider prov-chen is already booked at 2026-10-25T09:00:00Z","instance":"/api/appointments"}
```

Other calls: `POST /api/appointments/{id}/check-in`, `POST /api/appointments/{id}/cancel`, `GET /api/appointments/{id}`, `GET /api/appointments?providerId=prov-chen`. The patient `pat-lapsed` has lapsed coverage and is rejected with `422`. The H2 console is at `/h2-console`.

## Project structure

| Class | Package | Responsibility |
|---|---|---|
| `AppointmentController` | `api` | REST endpoints, maps the `Idempotency-Key` header, returns 201 or 200 |
| `BookAppointmentRequest`, `AppointmentResponse` | `api` | Request and response records |
| `ApiExceptionHandler` | `api` | Turns rule violations into `ProblemDetail` responses |
| `AppointmentService` | `service` | Booking rules, idempotency, cancellation and check-in, event publishing |
| `BookingRuleException`, `SlotConflictException`, `NotFoundException` | `service` | Errors mapped to 422, 409 and 404 |
| `Appointment`, `Provider`, `Patient` | `domain` | JPA entities; `Appointment.transitionTo` enforces the lifecycle |
| `AppointmentStatus` | `domain` | Lifecycle enum and allowed transitions |
| `AppointmentEvents` | `domain` | Event records named after the Kafka topics |
| `AppointmentRepository`, `ProviderRepository`, `PatientRepository` | `repository` | Spring Data JPA, including the overlap query |
| `EligibilityChecker`, `StubEligibilityChecker` | `integration` | Insurance port and deterministic stub |
| `AppointmentEventListener` | `integration` | Logs what the reminder, waitlist and dashboard consumers would do |

## From example to production
| Here | Production on the configured stack |
|---|---|
| H2 in memory, `ddl-auto: create-drop` | Amazon RDS for PostgreSQL with Flyway migrations. Add a `tstzrange` exclusion constraint (`EXCLUDE USING gist`) on provider and time so the database itself prevents overlaps, because the check-then-insert in the service can race under concurrency |
| `ApplicationEventPublisher` | Apache Kafka topics `appointment.booked`, `appointment.cancelled`, `appointment.checked-in`, keyed by provider id for per-provider ordering, written through a transactional outbox |
| `StubEligibilityChecker` | An adapter to the payer's real-time eligibility service (X12 270/271 or a FHIR-based API), with timeouts and a circuit breaker |
| Single UTC clock | Provider time zones stored per clinic and displayed in local time; recurring availability templates and holidays |
| No authentication | OIDC (for example Amazon Cognito) with role-based access, audit logging of every read and change of patient data |
| Local run | Container on Amazon EKS, Terraform for infrastructure, OpenTelemetry traces and logs to CloudWatch |
