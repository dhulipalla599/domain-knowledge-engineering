# Shipment Booking (runnable example)

Part of **Logistics** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/logistics/shipment-booking.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/logistics/shipment-booking.md)

## What this example shows
A shipper books space on a carrier's rate quote. The service enforces three business rules (quote not expired, weight within the quote limit, dangerous goods only on quotes that accept them), makes the request idempotent through an `Idempotency-Key` header, asks the carrier for a slot through a stubbed gateway, and publishes domain events named after the Kafka topics used in production. Booking lifecycle: `PENDING -> CONFIRMED -> DISPATCHED`, with `CANCELLED` reachable from `PENDING` and `CONFIRMED`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/logistics/shipment-booking
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Sample quotes are seeded: `Q-1001` (valid, 18,000 kg, no dangerous goods), `Q-1002` (valid, accepts dangerous goods), `Q-1003` (expired), `Q-1004` (carrier offline).

Happy path:

```bash
curl -s -X POST localhost:8080/api/bookings \
  -H 'Idempotency-Key: demo-1' -H 'Content-Type: application/json' \
  -d '{"customerId":"C-77","quoteId":"Q-1001","weightKg":12000}'
```

```json
{"id":"b83cc043-17ad-4481-8335-43793d6405c1","reference":"BK-F7DAB0C0","status":"CONFIRMED","quoteId":"Q-1001","weightKg":12000.0,"price":2450.00,"currency":"USD","carrierConfirmation":"NOR-BK-F7DAB0C0"}
```

Repeating the same call with the same `Idempotency-Key` returns the same booking and publishes no new events.

Rule violation (expired quote):

```bash
curl -s -X POST localhost:8080/api/bookings \
  -H 'Idempotency-Key: demo-2' -H 'Content-Type: application/json' \
  -d '{"customerId":"C-77","quoteId":"Q-1003","weightKg":1000}'
```

```json
{"type":"about:blank","title":"Gone","status":410,"detail":"Quote Q-1003 expired at 2020-01-01T00:00:00Z","instance":"/api/bookings"}
```

Other rejections: weight over the limit or dangerous goods on `Q-1001` return `422`; `Q-1004` returns `503`; an unknown quote returns `404`; cancelling a dispatched or already cancelled booking returns `409`.

## Project structure
| Class | Package | What it does |
|---|---|---|
| `ShipmentBookingApplication` | root | Spring Boot entry point |
| `Booking` | `domain` | Aggregate; enforces legal status transitions |
| `BookingStatus` | `domain` | Lifecycle enum with allowed next states |
| `RateQuote` | `domain` | Carrier offer with price, limits and expiry |
| `BookingEvent` | `domain` | Sealed event types (`BookingRequested`, `BookingConfirmed`, `BookingCancelled`) |
| `BookingRepository`, `RateQuoteRepository` | `repository` | Spring Data JPA repositories |
| `BookingService` | `service` | Business rules, idempotency, event publishing |
| `BookingRuleException`, `ClockConfig` | `service` | Rule errors with HTTP status; injectable clock |
| `BookingController`, `BookingRequest`, `BookingResponse` | `api` | REST endpoints and DTO records |
| `ApiExceptionHandler` | `api` | Maps errors to `ProblemDetail` |
| `CarrierGateway`, `StubCarrierGateway`, `CarrierUnavailableException` | `integration` | Carrier port and deterministic stub |
| `BookingEventListener` | `integration` | `@EventListener` standing in for Kafka consumers |

## From example to production
- **Database:** H2 becomes PostgreSQL (the `postgres` profile and `docker-compose.yml` are included); schema managed by Flyway or Liquibase instead of `create-drop`; a unique index on `idempotency_key` guards against concurrent duplicates.
- **Messaging:** `ApplicationEventPublisher` becomes Apache Kafka topics (`shipment.booking.requested`, `.confirmed`, `.cancelled`) published through a transactional outbox, keyed by booking id.
- **Carrier adapter:** `StubCarrierGateway` becomes a REST or EDI client with timeouts, retries and a circuit breaker; confirmation may arrive asynchronously.
- **AWS:** runs on Amazon EKS with RDS for PostgreSQL and Amazon MSK; secrets in AWS Secrets Manager; traces and metrics through OpenTelemetry to CloudWatch.
