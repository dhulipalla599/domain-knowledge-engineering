# Number Porting (runnable example)

Part of **Telecom** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/telecom/number-porting.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/telecom/number-porting.md)

## What this example shows
A small Spring Boot 3 service for **mobile number porting**: a subscriber moves their number from a donor operator to the recipient, Northwind Mobile. It shows idempotent submission, a one-port-in-flight rule, a business-day port-date rule, donor validation behind an interface, an enforced status lifecycle, and domain events published with Spring's `ApplicationEventPublisher` (stand-ins for Kafka topics such as `port.request.scheduled`).

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/telecom/number-porting
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Submit a port request (the stub donor knows `+15550101001` / Ada Lovelace):

```bash
curl -s -X POST localhost:8080/api/port-requests -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"msisdn":"+15550101001","subscriberName":"Ada Lovelace","donorOperator":"Contoso Telecom","requestedPortDate":"2026-11-03"}'
```

```json
{"id":4,"msisdn":"+15550101001","subscriberName":"Ada Lovelace","donorOperator":"Contoso Telecom","recipientOperator":"Northwind Mobile","requestedPortDate":"2026-11-03","status":"SCHEDULED","rejectionReason":null}
```

A Saturday port date is refused (the date rule is evaluated against today's real date, so pick a weekend day in the future):

```bash
curl -s -X POST localhost:8080/api/port-requests -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-2' \
  -d '{"msisdn":"+15550101002","subscriberName":"Grace Hopper","donorOperator":"Contoso Telecom","requestedPortDate":"2026-11-07"}'
```

```json
{"type":"about:blank","title":"INVALID_PORT_DATE","status":422,"detail":"Port date 2026-11-07 is not a business day","instance":"/api/port-requests"}
```

Cancelling a completed port (seeded request 2) is an illegal transition:

```bash
curl -s -X POST localhost:8080/api/port-requests/2/cancel
# 409 {"title":"ILLEGAL_STATE_TRANSITION","detail":"Port request 2 cannot move from COMPLETED to CANCELLED",...}
```

Other endpoints: `GET /api/port-requests`, `GET /api/port-requests/{id}`, `POST /api/port-requests/{id}/complete` (409 `TOO_EARLY` before the port date).
Donor stub: `+15550101003` (Alan Turing) is contract-locked and is recorded as `REJECTED`.

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `PortingController` | api | REST endpoints |
| `SubmitPortRequest`, `PortRequestResponse` | api | Request/response records |
| `ApiExceptionHandler` | api | Maps rule violations to `ProblemDetail` |
| `PortingService` | service | Submit, complete, cancel; enforces the rules |
| `PortDatePolicy` | service | Business-day and lead-time rule |
| `PortRuleViolation`, `PortRequestNotFound`, `ClockConfig` | service | Errors and injectable clock |
| `PortRequest`, `PortStatus` | domain | Entity and lifecycle with legal transitions |
| `PortEvents` | domain | Event records named after Kafka topics |
| `PortRequestRepository` | repository | Spring Data JPA |
| `DonorGateway` / `StubDonorGateway` | integration | Donor validation port and deterministic stub |
| `RoutingGateway` / `InMemoryRoutingGateway` | integration | Routing update port and stub |
| `PortEventListener` | integration | `@EventListener` consumers |

## From example to production
- **Database:** H2 becomes PostgreSQL (run with `-Dspring-boot.run.profiles=postgres`) with Flyway migrations instead of `create-drop`.
- **Events:** `ApplicationEventPublisher` becomes Apache Kafka topics via a transactional outbox, keyed by MSISDN, with idempotent consumers.
- **Donor and routing:** the stubs become adapters to the national porting hub or operator APIs and the number portability database; timeouts, retries and circuit breakers are added.
- **Platform:** containerised on Amazon EKS with RDS PostgreSQL and MSK, Terraform-managed, traced with OpenTelemetry into CloudWatch.
- **Not modelled:** per-country timing rules, authentication, donor-side port-out, and customer-facing notifications.
