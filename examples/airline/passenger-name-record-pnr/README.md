# Passenger Name Record (PNR) (runnable example)

Part of **Airline and Travel** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/airline/passenger-name-record-pnr.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/airline/passenger-name-record-pnr.md)

## What this example shows
A small PNR service: create a booking record (passengers, flight segments, contact) that starts `ON_HOLD` with a 24-hour ticketing time limit, issue tickets, and cancel. It enforces these rules and rejects violations with RFC 9457 `ProblemDetail` responses:

- 1 to 9 passengers, names normalised to upper case and limited to letters, spaces, hyphens and apostrophes (422).
- The same passenger cannot hold two active PNRs on the same flight and date (409 duplicate booking).
- Ticketing is only possible while `ON_HOLD` and before the ticketing time limit (409).
- Only legal lifecycle transitions: `ON_HOLD -> TICKETED | CANCELLED`, `TICKETED -> CANCELLED`.

Domain events (`PnrCreated`, `PnrTicketed`, `PnrCancelled`) go through Spring's `ApplicationEventPublisher` and are consumed by `@EventListener`, standing in for the Kafka topics `pnr.created`, `pnr.ticketed` and `pnr.cancelled`.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/airline/passenger-name-record-pnr
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Two PNRs are seeded: `K7M2QP` (ON_HOLD, two passengers on SK101) and `R4T9WZ` (TICKETED).

```bash
# Happy path: create a PNR (names are normalised, locator is generated)
curl -i -X POST localhost:8080/api/pnrs -H 'Content-Type: application/json' -d '{
  "contactEmail":"mia@example.com",
  "passengers":[{"firstName":"Mia","lastName":"Chen"}],
  "segments":[{"flightNumber":"SK300","departureDate":"2027-03-01","origin":"LHR","destination":"CDG"}]}'
# 201 Created, Location: /api/pnrs/X00001
# {"recordLocator":"X00001","status":"ON_HOLD","passengers":[{"firstName":"MIA","lastName":"CHEN"}],...}

curl -X POST localhost:8080/api/pnrs/X00001/ticket
# 200 {"recordLocator":"X00001","status":"TICKETED",...,"ticketNumbers":["176-X00001-1"]}

# Rule violation: ticketing a PNR that is already ticketed
curl -i -X POST localhost:8080/api/pnrs/R4T9WZ/ticket
# 409 {"title":"Conflict","status":409,"detail":"PNR R4T9WZ is TICKETED, cannot be ticketed",...}
```

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `Pnr` | domain | Aggregate: name rules, ticketing time limit, lifecycle transitions |
| `PnrStatus` | domain | `ON_HOLD`, `TICKETED`, `CANCELLED` and the legal next states |
| `PassengerName`, `FlightSegment` | domain | Embeddable value objects |
| `PnrEvents` | domain | `PnrCreated`, `PnrTicketed`, `PnrCancelled` |
| `RuleViolationException` | domain | Business rule failure (`INVALID` or `CONFLICT`) |
| `PnrRepository` | repository | Spring Data JPA, locator lookup and duplicate-booking query |
| `PnrService` | service | Orchestrates rules, adapters and event publishing |
| `NotFoundException` | service | Unknown record locator |
| `PnrController`, DTO records | api | REST endpoints under `/api/pnrs` |
| `ApiExceptionHandler` | api | Maps failures to `ProblemDetail` (400, 404, 409, 422) |
| `RecordLocatorGenerator` / `SequentialLocatorGenerator` | integration | Locator port and deterministic stub |
| `TicketingGateway` / `StubTicketingGateway` | integration | Ticketing port and stub |
| `PnrEventListener` | integration | Stand-in for Kafka consumers |
| `ClockConfig` | integration | Injectable `Clock` |

## From example to production
- **Database:** H2 becomes Amazon RDS for PostgreSQL (the `postgres` profile and `docker-compose.yml` show the wiring); schema managed by Flyway instead of `create-drop`, and `record_locator` keeps its unique index.
- **Events:** `ApplicationEventPublisher` becomes Apache Kafka topics `pnr.created`, `pnr.ticketed` and `pnr.cancelled`, keyed by record locator for per-PNR ordering, published through a transactional outbox so the database write and the event cannot diverge.
- **Locator generation:** a collision-checked random generator that avoids look-alike characters, instead of a counter.
- **Ticketing:** `StubTicketingGateway` becomes an adapter to the airline's ticketing system or a GDS, with idempotency keys and retries.
- **Expiry:** a scheduled job (or Kafka-driven timer) cancels `ON_HOLD` PNRs past their time limit and releases inventory; here the limit is only checked when ticketing.
- **Operations:** runs on Amazon EKS with OpenTelemetry traces and CloudWatch logs and metrics; authentication, authorisation and field-level protection of passenger data are added in front of the API.
