# Quote and Rating (runnable example)

Part of **Insurance** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/insurance/quote-and-rating.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/insurance/quote-and-rating.md)

## What this example shows
A personal-auto quote is created, rated and bound. `POST /api/quotes` runs eligibility rules, looks up a state base rate, asks a (stubbed) insurance-score vendor for a score, computes a premium with `BigDecimal`, and stores the quote as `RATED` with a 30-day validity. `POST /api/quotes/{id}/bind` binds it. Two business rules reject bad input with `ProblemDetail` responses:

- **Eligibility:** drivers under 18, or with more than 3 prior claims, are declined with `422`.
- **Price validity and lifecycle:** an expired quote cannot be bound (`409`, and the quote becomes `EXPIRED`); a quote can only be bound from `RATED` (`409` on a second bind).

Lifecycle: `DRAFT -> RATED -> BOUND`, or `RATED -> EXPIRED`. Illegal moves are rejected inside the `Quote` entity. `QuoteRated` and `QuoteBound` events are published with Spring's `ApplicationEventPublisher` and consumed by `@EventListener` methods (stand-ins for the Kafka topics `insurance.quote.rated` and `insurance.quote.bound`).

The rate factors in `RatingEngine` are illustrative, not a filed rate plan.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/insurance/quote-and-rating
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path: rate a new quote (Texas, sedan, no claims).

```bash
curl -s -X POST localhost:8080/api/quotes -H 'Content-Type: application/json' \
  -d '{"driverName":"Priya Nair","driverAge":29,"state":"TX","vehicleType":"SEDAN","priorClaims":0}'
```
```json
{"id":"565d6596-924d-4b52-861f-c6a1b66fd308","driverName":"Priya Nair","state":"TX","status":"RATED","premium":1320.00,"validUntil":"2026-11-12T15:08:34.818685256Z","boundAt":null}
```
(`201 Created`; id, premium band and dates vary: the stub score depends on the name.)

Bind the seeded quote:

```bash
curl -s -X POST localhost:8080/api/quotes/11111111-1111-1111-1111-111111111111/bind
```
```json
{"id":"11111111-1111-1111-1111-111111111111","driverName":"Maria Alvarez","state":"TX","status":"BOUND","premium":1320.00,"validUntil":"2026-11-12T15:08:34.105760Z","boundAt":"2026-10-13T15:08:34.938302100Z"}
```

Rule violation: an under-age driver is declined.

```bash
curl -s -X POST localhost:8080/api/quotes -H 'Content-Type: application/json' \
  -d '{"driverName":"Tim Young","driverAge":17,"state":"TX","vehicleType":"SEDAN","priorClaims":0}'
```
```json
{"type":"about:blank","title":"Unprocessable Entity","status":422,"detail":"Driver must be at least 18 years old","instance":"/api/quotes"}
```

Binding the same quote again returns `409` with `"detail":"Quote 1111... is BOUND; cannot move to BOUND"`.

## Project structure
Base package `com.dke.insurance.quote_and_rating`.

| Class | Package | Responsibility |
|---|---|---|
| `QuoteAndRatingApplication` | root | Spring Boot entry point |
| `Quote` | `domain` | Aggregate; enforces legal lifecycle transitions |
| `QuoteStatus` | `domain` | `DRAFT`, `RATED`, `BOUND`, `EXPIRED` and allowed moves |
| `VehicleType` | `domain` | Vehicle class and its rating factor |
| `BaseRate` | `domain` | Base premium per state (stand-in for a rate table) |
| `QuoteEvents` | `domain` | `QuoteRated` and `QuoteBound` events |
| `QuoteRuleException` | `domain` | Business-rule violation with an HTTP status |
| `QuoteRepository`, `BaseRateRepository` | `repository` | Spring Data JPA repositories |
| `RatingEngine` | `service` | Pure premium calculation |
| `QuoteService` | `service` | Eligibility, validity and bind rules; publishes events |
| `ClockConfig` | `service` | Injectable `Clock` |
| `QuoteController`, `CreateQuoteRequest`, `QuoteResponse` | `api` | REST endpoints and DTO records |
| `ApiExceptionHandler` | `api` | Maps exceptions to `ProblemDetail` |
| `CreditScoreProvider`, `StubCreditScoreProvider` | `integration` | Score vendor port and deterministic stub |
| `PolicyIssuanceGateway`, `InMemoryPolicyIssuanceGateway` | `integration` | Policy system port and idempotent stub |
| `QuoteEventListener` | `integration` | Consumes the domain events |

## From example to production
| Here | Production on the configured stack |
|---|---|
| H2 in-memory, `ddl-auto` | PostgreSQL (Amazon RDS or Aurora) with Flyway migrations; run with `-Dspring-boot.run.profiles=postgres` locally |
| `ApplicationEventPublisher` | Apache Kafka topics `insurance.quote.rated` and `insurance.quote.bound`, published through a transactional outbox, keyed by quote id |
| `StubCreditScoreProvider` | HTTPS adapter to a licensed score vendor with timeouts, retries and a stored score reference for audit |
| `InMemoryPolicyIssuanceGateway` | Kafka consumer calling the policy administration system, de-duplicating on quote id |
| `BaseRate` table and hard-coded factors | Versioned, effective-dated rate tables approved by actuarial and filed with the regulator |
| Lazy expiry on bind | Same check plus a scheduled sweep to expire stale quotes |
| Console logging | OpenTelemetry traces and metrics exported to Amazon CloudWatch; service on Amazon EKS, infrastructure in Terraform |
