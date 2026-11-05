# Electronic Health Records (EHR) (runnable example)

Part of **Healthcare** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/healthcare/electronic-health-records-ehr.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/healthcare/electronic-health-records-ehr.md)

## What this example shows
A small clinical-documentation core: a clinician drafts a note, signs it, and later corrects it with an addendum. Reading a chart is allowed only with a treatment relationship, or with a recorded "break-glass" reason that lands in the audit trail.

- Lifecycle `DRAFT -> SIGNED -> AMENDED -> ENTERED_IN_ERROR`, enforced inside `ClinicalNote`.
- Rule 1: signed notes are immutable; corrections are addenda (HTTP 409 on edit).
- Rule 2: chart access needs a care-team relationship or a break-glass reason (HTTP 403), and every read is audited.
- Events `ehr.note.signed`, `ehr.note.amended`, `ehr.chart.accessed` use Spring's `ApplicationEventPublisher` as a stand-in for Kafka topics.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/healthcare/electronic-health-records-ehr
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path (seeded patient `pat-amara`, clinician `dr-lee` is on the care team):

```bash
curl -s -X POST localhost:8080/api/patients/pat-amara/notes -H 'Content-Type: application/json' \
  -d '{"authorId":"dr-lee","text":"BP 150/95. Start home monitoring."}'
# {"id":1,"patientId":"pat-amara","authorId":"dr-lee","text":"BP 150/95. Start home monitoring.","status":"DRAFT","addenda":[]}

curl -s -X POST localhost:8080/api/notes/1/sign -H 'Content-Type: application/json' -d '{"authorId":"dr-lee"}'
# {"id":1,...,"status":"SIGNED","addenda":[]}
```

Rule violation (`dr-lee` is not on the care team of `pat-priya`, no break-glass reason):

```bash
curl -s "localhost:8080/api/patients/pat-priya/chart?clinicianId=dr-lee"
# {"status":403,"title":"Forbidden","detail":"dr-lee has no treatment relationship with pat-priya; supply a break-glass reason for emergency access",...}

curl -s "localhost:8080/api/patients/pat-priya/chart?clinicianId=dr-lee&breakGlassReason=Unconscious%20in%20ED"   # 200, audited
curl -s localhost:8080/api/patients/pat-priya/audit
```

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `ClinicalNote` | `domain` | Entity and lifecycle rules (`edit`, `sign`, `addAddendum`, `markEnteredInError`) |
| `NoteStatus` | `domain` | Lifecycle enum |
| `Patient`, `AuditEntry` | `domain` | Patient record; append-only access log |
| `EhrEvents` | `domain` | Event records named after Kafka topics |
| `*Repository` (3) | `repository` | Spring Data JPA repositories |
| `EhrService` | `service` | Access rule, authorship check, audit, event publishing |
| `EhrController`, `ApiExceptionHandler` | `api` | REST endpoints; maps exceptions to `ProblemDetail` |
| `CareTeamDirectory`, `StubCareTeamDirectory` | `integration` | Port and in-memory stub for care-team lookups |
| `EhrEventListener` | `integration` | Consumes events (FHIR export, privacy alert stand-ins) |

## From example to production
- H2 becomes PostgreSQL (the `postgres` profile and `docker-compose.yml` are included); use Flyway migrations, and revoke `UPDATE`/`DELETE` on the audit table.
- `ApplicationEventPublisher` becomes Apache Kafka topics via a transactional outbox, keyed by patient id for per-patient ordering.
- `StubCareTeamDirectory` becomes an adapter over the hospital's ADT/scheduling feed; identity comes from OIDC tokens (for example Amazon Cognito), not request bodies.
- Runs on Amazon EKS with Terraform-managed RDS, MSK and KMS; OpenTelemetry traces go to CloudWatch.
