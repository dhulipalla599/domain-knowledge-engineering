# Flight Shopping and Booking (runnable example)

Part of **Airline and Travel** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/airline/flight-shopping-and-booking.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/airline/flight-shopping-and-booking.md)

## What this example shows
A small flight shop-and-book service. A customer searches offers for a route and date, **holds** one fare for 15 minutes (taking a seat out of inventory), then **confirms** with a payment token, which authorises payment and issues a ticket (PNR and e-ticket number). Holds can be cancelled, and a sweep expires holds that were never paid.

Rules enforced, each rejecting bad input with a `ProblemDetail`:

- **Fare re-validation:** the hold fails with `422` if the price the customer saw no longer matches the offer.
- **No overselling:** a sold-out fare is rejected with `422`; `@Version` optimistic locking turns a race for the last seat into `409`.
- **Idempotent holds:** replaying the same `Idempotency-Key` returns the same booking (`200`) and takes no second seat.
- **Hold expiry:** confirming after the deadline releases the seat, persists `EXPIRED` and returns `422`.
- **Legal lifecycle only:** `HELD -> CONFIRMED -> TICKETED`, with `EXPIRED` and `CANCELLED` terminal; illegal moves return `409`.

Domain events (`flight.booking.held`, `flight.booking.ticketed`, `flight.booking.released`) are published with `ApplicationEventPublisher` and consumed by an `@EventListener`, standing in for Apache Kafka topics.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/airline/flight-shopping-and-booking
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Search (seed data has five offers; `2027-03-15` SFO to JFK returns three sellable ones, cheapest first):

```bash
curl -s "localhost:8080/api/v1/offers?origin=SFO&destination=JFK&date=2027-03-15"
```

Hold the flex fare (HTTP 201):

```bash
curl -s -X POST localhost:8080/api/v1/bookings \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-001' \
  -d '{"offerId":"aaaaaaaa-0000-0000-0000-000000000002","passengerName":"Amara Osei","passengerEmail":"amara@example.com","expectedPrice":429.00}'
```

```json
{"id":"88a61c65-0ebf-4044-83d8-1dc1eadfb1d1","offerId":"aaaaaaaa-0000-0000-0000-000000000002","passengerName":"Amara Osei","totalAmount":429.00,"currency":"USD","status":"HELD","holdExpiresAt":"2026-10-20T15:20:47.611500503Z","pnr":null,"ticketNumber":null,"statusReason":null}
```

Confirm and ticket it (use the `id` from above; the token `tok_declined` simulates a decline):

```bash
curl -s -X POST localhost:8080/api/v1/bookings/<id>/confirm \
  -H 'Content-Type: application/json' -d '{"paymentToken":"tok_visa"}'
```

```json
{"id":"88a61c65-0ebf-4044-83d8-1dc1eadfb1d1","offerId":"aaaaaaaa-0000-0000-0000-000000000002","passengerName":"Amara Osei","totalAmount":429.00,"currency":"USD","status":"TICKETED","holdExpiresAt":"2026-10-20T15:20:47.611501Z","pnr":"DK1001","ticketNumber":"0012000001001","statusReason":"Ticket issued"}
```

Rule violation, a stale fare (HTTP 422):

```bash
curl -s -X POST localhost:8080/api/v1/bookings \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-002' \
  -d '{"offerId":"aaaaaaaa-0000-0000-0000-000000000002","passengerName":"Amara Osei","passengerEmail":"amara@example.com","expectedPrice":399.00}'
```

```json
{"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Fare changed from 399.00 to 429.00 USD; search again to confirm the new price","instance":"/api/v1/bookings"}
```

## Project structure
Base package `com.dke.airline.flight_shopping_and_booking`.

| Class | Package | Responsibility |
|---|---|---|
| `Booking` | `domain` | Entity that owns the lifecycle and rejects illegal transitions |
| `BookingStatus` | `domain` | Lifecycle enum with `canMoveTo()` |
| `FlightOffer` | `domain` | Fare and seat inventory; `holdSeat()` / `releaseSeat()`; `@Version` for optimistic locking |
| `BookingEvents` | `domain` | Event records and the Kafka topic names they stand in for |
| `FlightOfferRepository`, `BookingRepository` | `repository` | Spring Data JPA queries |
| `BookingService` | `service` | Search, `hold`, `confirm`, `cancel`, `expireStaleHolds` and the business rules |
| `BusinessRuleException`, `NotFoundException` | `service` | Rule violation (422) and missing resource (404) |
| `BookingController` | `api` | REST endpoints under `/api/v1` |
| `ApiExceptionHandler` | `api` | Maps exceptions to `ProblemDetail` |
| `HoldRequest`, `ConfirmRequest`, `CancelRequest`, `OfferResponse`, `BookingResponse` | `api` | Request and response records |
| `PaymentGateway`, `StubPaymentGateway` | `integration` | Payment port and a stub that declines only `tok_declined` |
| `TicketIssuer`, `StubTicketIssuer` | `integration` | Ticketing port and a deterministic PNR generator |
| `BookingAuditListener` | `integration` | `@EventListener` consumer that records every event |
| `ClockConfig` | `integration` | Injectable `Clock` so time-based rules are testable |

## From example to production
| Here | Production on the target stack |
|---|---|
| H2 in-memory, `ddl-auto: create-drop`, `data.sql` | Amazon RDS for PostgreSQL (`--spring.profiles.active=postgres` shows the wiring), schema managed by Flyway or Liquibase |
| `ApplicationEventPublisher` and `@EventListener` | Apache Kafka (for example Amazon MSK) topics `flight.booking.held`, `flight.booking.ticketed`, `flight.booking.released`, published through a transactional outbox so the database write and the event cannot diverge |
| `StubPaymentGateway` | Adapter to a payment service provider with 3-D Secure, authorise then capture, and its own idempotency key |
| `StubTicketIssuer` | Adapter to the airline's passenger service system (PSS) or a distribution API to create the PNR and e-ticket |
| Offers seeded in SQL | Offers come from a fares and availability service, usually cached with a short TTL and re-priced at hold time |
| Seat count on the offer row | Inventory owned by the PSS or an inventory service; the count here only demonstrates the concurrency rule |
| `/admin/expire-holds` | A scheduled job (Kubernetes CronJob) or a delayed message per hold |
| Runs as one process | Containerised, deployed to Amazon EKS, traces through OpenTelemetry to CloudWatch |
