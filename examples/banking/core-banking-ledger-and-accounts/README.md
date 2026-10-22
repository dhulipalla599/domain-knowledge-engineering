# Core Banking Ledger and Accounts (runnable example)

Part of **Banking** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/banking/core-banking-ledger-and-accounts.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/banking/core-banking-ledger-and-accounts.md)

## What this example shows
A small double-entry ledger for a fictional retail bank (Northwind Bank). A transfer becomes one balanced journal entry
(a debit line and a credit line) that updates both account balances atomically. It demonstrates:

- **Idempotent posting**: retrying with the same `Idempotency-Key` returns the original entry instead of moving money twice.
- **Overdraft rule**: a debit may not take the balance below the account's overdraft limit (`INSUFFICIENT_FUNDS`).
- **Account state rule**: frozen or closed accounts cannot transact (`ACCOUNT_NOT_ACTIVE`); currencies must match; a fraud stub can hold large transfers.
- **Immutable history**: a posted entry is never edited; `POST /transfers/{id}/reversal` posts the opposite lines.
- **Lifecycles** enforced in the domain: `EntryStatus` (PENDING, POSTED, REVERSED) and `AccountStatus` (ACTIVE, FROZEN, CLOSED).
- **Events** (`ledger.entry.posted`, `ledger.entry.reversed`, `ledger.account.status-changed`) published with Spring's `ApplicationEventPublisher` as stand-ins for Kafka topics.

All money is stored as whole minor units (pence), never as floating point.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/banking/core-banking-ledger-and-accounts
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Seeded accounts: `ACC-1001` (GBP 1,000.00), `ACC-1002` (GBP 250.00, overdraft 500.00), `ACC-1003` (frozen), `ACC-1004` (GBP 90,000.00), `ACC-2001` (EUR).

Happy path, moving GBP 25.00:

```bash
curl -s -X POST localhost:8080/api/v1/transfers -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-1' \
  -d '{"fromAccount":"ACC-1001","toAccount":"ACC-1002","amountMinor":2500,"currency":"GBP","reference":"Rent share"}'
```

```json
{"entryId":"a492cc11-118d-46cf-bd2c-803e967c1ddb","status":"POSTED","amountMinor":2500,"currency":"GBP","reference":"Rent share","reversalOf":null,"createdAt":"2026-10-21T15:06:35.678827520Z"}
```

Rule violation, overspending (HTTP 422):

```bash
curl -s -X POST localhost:8080/api/v1/transfers -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-2' \
  -d '{"fromAccount":"ACC-1001","toAccount":"ACC-1002","amountMinor":999999,"currency":"GBP"}'
```

```json
{"type":"about:blank","title":"Business rule violated","status":422,"detail":"Account ACC-1001 has 97500 available, needs 999999","instance":"/api/v1/transfers","code":"INSUFFICIENT_FUNDS"}
```

Other calls: `GET /api/v1/accounts/ACC-1001`, `GET /api/v1/accounts/ACC-1002/statement`,
`POST /api/v1/transfers/{entryId}/reversal`, `POST /api/v1/accounts/ACC-1001/status` with `{"status":"FROZEN"}`.

## Project structure
Base package `com.dke.banking.core_banking_ledger_and_accounts`.

| Class | Package | Responsibility |
|---|---|---|
| `LedgerController` | `api` | REST endpoints for transfers, reversals, balances, statements and account status |
| `LedgerDtos` | `api` | Request and response records |
| `ApiExceptionHandler` | `api` | Maps rule violations to HTTP 422 `ProblemDetail`, missing items to 404, duplicate keys to 409 |
| `LedgerService` | `service` | Transfer, reversal and status-change rules; transaction boundary; publishes events |
| `Account` | `domain` | Balance, overdraft rule, account lifecycle |
| `JournalEntry` | `domain` | One balanced posting with unique idempotency key and `EntryStatus` lifecycle |
| `LedgerLine` | `domain` | Append-only debit or credit line |
| `LedgerEvents` | `domain` | `EntryPosted`, `EntryReversed`, `AccountStatusChanged` |
| `AccountRepository` etc. | `repository` | Spring Data JPA; `findByNumberForUpdate` takes a row lock |
| `FraudScreeningClient` | `integration` | Port for the fraud engine; `StubFraudScreeningClient` holds transfers above 50,000.00 |
| `AuditTrailListener`, `LowBalanceAlertListener` | `integration` | Stand-ins for Kafka consumers |

## From example to production
| Here | Production |
|---|---|
| H2 in memory, `ddl-auto: create-drop` | PostgreSQL (Amazon RDS or Aurora) with Flyway migrations, a `CHECK` on balances where policy requires it, and revoked `UPDATE`/`DELETE` on `ledger_lines` (run with `-Dspring-boot.run.profiles=postgres`) |
| `ApplicationEventPublisher` and `@EventListener`, synchronous and in-process | Apache Kafka topics with the same names, keyed by account number, written through a transactional outbox so an event is never lost or sent for a rolled-back posting; consumers must be idempotent |
| `StubFraudScreeningClient` | HTTP or gRPC adapter to the fraud service with a timeout and a defined fail-open or fail-closed policy |
| Pessimistic row locks in one database | Same idea per shard; cross-shard or cross-system transfers need a saga or a clearing account |
| Plain logging | OpenTelemetry traces and metrics to CloudWatch; audit events retained per regulatory policy |
| Runs as one JVM | Container on Amazon EKS behind an ALB, secrets in AWS Secrets Manager, infrastructure in Terraform |
