# Patient Registration and Master Patient Index (runnable example)

Part of **Healthcare** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/healthcare/patient-registration-and-master-patient-index.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/healthcare/patient-registration-and-master-patient-index.md)

## What this example shows
A small master patient index (MPI). A front-desk registration is scored against existing records that share the same date of birth, then lands in one of three outcomes:

- **Score 80 or more** - rejected with `409` and the existing MRN (a probable duplicate).
- **Score 50-79** - saved as `PENDING_REVIEW` and sent to a data-steward queue.
- **Below 50** - saved as `ACTIVE` with a new MRN.

Stewards then `approve` a held record as a distinct person or `merge` it into a survivor. The record lifecycle (`ACTIVE`, `PENDING_REVIEW`, `MERGED`) is enforced inside the `Patient` entity, and each step publishes a domain event named after the Kafka topic it stands in for.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/healthcare/patient-registration-and-master-patient-index
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Three patients are seeded (`MRN-00000001` Amara Okafor, `MRN-00000002` Liam Hartley, `MRN-00000003` Priya Raman).

```bash
# 1. Happy path: a new patient is registered as ACTIVE (201)
curl -s -X POST localhost:8080/api/patients -H 'Content-Type: application/json' \
  -d '{"firstName":"Noor","lastName":"Haddad","dateOfBirth":"1990-01-05","phone":"555-019-9000","postcode":"CF10 1AA"}'
# {"mrn":"MRN-AFCC267B","status":"ACTIVE","firstName":"Noor","lastName":"Haddad","dateOfBirth":"1990-01-05","survivorMrn":null}

# 2. Rule violation: same name, date of birth and phone as an existing record (409)
curl -s -X POST localhost:8080/api/patients -H 'Content-Type: application/json' \
  -d '{"firstName":"Amara","lastName":"Okafor","dateOfBirth":"1988-03-14","phone":"(555) 010-1234"}'
# {"type":"about:blank","title":"Probable duplicate patient","status":409,"detail":"Registration matches existing record MRN-00000001 (score 90)","instance":"/api/patients","existingMrn":"MRN-00000001","score":90}

# 3. Possible match: surname typo and no phone is held for review (201, PENDING_REVIEW)
curl -s -X POST localhost:8080/api/patients -H 'Content-Type: application/json' \
  -d '{"firstName":"Amara","lastName":"Okafur","dateOfBirth":"1988-03-14"}'
# {"mrn":"MRN-F8B28C65","status":"PENDING_REVIEW",...}

# 4. Steward actions (use the MRN returned above)
curl -s localhost:8080/api/review-queue
curl -s -X POST localhost:8080/api/patients/MRN-F8B28C65/merge -H 'Content-Type: application/json' \
  -d '{"survivorMrn":"MRN-00000001"}'
```

MRNs for new patients are random, so yours will differ. A future date of birth returns `422`; merging a record into itself or an already merged record returns `409`.

## Project structure
Base package: `com.dke.healthcare.patient_registration_and_master_patient_index`.

| Class | Package | Responsibility |
|---|---|---|
| `Patient`, `PatientStatus` | `domain` | JPA entity and lifecycle enum; enforces legal state transitions |
| `DomainEvent`, `PatientRegistered`, `MatchReviewRequested`, `PatientsMerged` | `domain` | Events named after the Kafka topics |
| `*Exception` classes | `domain` | Business errors mapped to HTTP statuses |
| `PatientRepository` | `repository` | Spring Data access, including the date-of-birth blocking query |
| `MatchScorer` | `service` | Deterministic 0-100 match score |
| `RegistrationService` | `service` | Register, approve, merge; publishes events |
| `PatientController`, DTO records, `ApiExceptionHandler` | `api` | REST endpoints and `ProblemDetail` errors |
| `AuditLogListener`, `ReviewTaskListener` | `integration` | In-process stand-ins for Kafka consumers |

## From example to production
| Here | Production on the configured stack |
|---|---|
| H2 in memory (`ddl-auto: create-drop`) | PostgreSQL (Amazon RDS or Aurora) with Flyway migrations; run with `-Dspring-boot.run.profiles=postgres` for a local PostgreSQL |
| `ApplicationEventPublisher` and `@EventListener` | Apache Kafka (Amazon MSK) topics with the same names, written through a transactional outbox, keyed by MRN, with idempotent consumers |
| `MatchScorer` rules | A probabilistic or ML matcher behind the same interface, with tuned thresholds and a steward review UI |
| Random MRN suffix | A database sequence or a dedicated identifier service with a check digit |
| No authentication | OIDC sign-in, role-based access and break-glass auditing on every read of a record |
| Console logging | OpenTelemetry traces and metrics exported to CloudWatch; containers on Amazon EKS |
