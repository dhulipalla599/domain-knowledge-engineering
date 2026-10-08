Domain: {{ domain }}
Topic: {{ topic }}

RUNNABLE EXAMPLE

A working Spring Boot project skeleton has already been created for you at `{{ example.path }}`
(Java {{ example.java_version }}, Spring Boot {{ example.spring_boot_version }}, package `{{ example.package }}`,
main class `{{ example.app_class }}`). Turn it into a small but complete, runnable implementation of this topic's
core flow - the same flow, entities, rules and events described on the page.

Code requirements:
- Packages under `{{ example.package }}`: `domain` (JPA entities, enums, domain events), `repository`,
  `service` (the business rules), `api` (REST controller, request/response records, exception handler),
  and `integration` (event listeners and adapters).
- Implement the main happy path end to end plus at least two business rules from "Business Rules and Edge Cases",
  each rejecting bad input with a clear HTTP error (use `@RestControllerAdvice` and `ProblemDetail`).
- Model the main entity's lifecycle as an enum and enforce legal state transitions in the domain class.
- Publish domain events with Spring's `ApplicationEventPublisher` and consume them with `@EventListener`
  (stand-ins for {{ stack.messaging }} topics; name them after the topics on the page).
- It must run with NO external services, accounts or API keys. Use the H2 database that is already configured.
  Anything external (identity verification, payment network, LLM, model, message broker, cloud storage) goes
  behind a Java interface with a deterministic in-memory/stub implementation, and the README explains what the
  real adapter would be.{% if domain_kind == "ai" %}
  For this AI topic: put the model behind an interface such as `LlmClient` or `EmbeddingClient`, ship a
  deterministic stub (for example keyword scoring or a hashed bag-of-words vector), and keep prompts, retrieval,
  guardrails and evaluation logic as real, testable Java code.{% endif %}{% if domain_kind == "data" %}
  For this Data Engineering topic: implement the pipeline in-process (extract from a CSV/JSON file in
  `src/main/resources/sample-data/`, transform, validate, load to H2), triggered by a REST endpoint and
  a `@Scheduled` job that is disabled by default; record run metrics in a table.{% endif %}
- Seed realistic sample data in `src/main/resources/data.sql` so the API returns something useful right away.
- Tests: at least one plain JUnit 5 unit test of the core business rule, and one `@SpringBootTest` +
  `@AutoConfigureMockMvc` test that drives the happy path and one rule violation over HTTP.
  Keep the existing `contextLoads` test.
- Only add dependencies whose versions are managed by Spring Boot (no `<version>` tags). Use Java records
  for DTOs and constructor injection everywhere. No Lombok.
- Keep it small enough to read in 15 minutes: roughly 8-15 classes.
- Replace every TODO in `{{ example.path }}/README.md` (what it shows, curl commands with expected
  responses, class table, production mapping to {{ stack.database }}, {{ stack.messaging }}, {{ stack.cloud }}).

Build it with `{{ example.build_command }}` inside `{{ example.path }}` and fix it until the build and tests pass.

Then add these two sections to the page:

## Runnable Example
One paragraph on what the example implements, then: a link to the source
([`{{ example.path }}`]({{ example.url }})), the commands to run and test it, two `curl` calls with their
expected responses, and a table: Class | Package | Responsibility.

## UML Diagrams
Diagrams of the example code exactly as written (same class, method and state names), each with one or two
sentences explaining it:
### Class Diagram
A Mermaid `classDiagram` of the controller, service, repository, entities, enums, events and listeners, with
key fields/methods and their relationships.
### Sequence Diagram (control flow)
A Mermaid `sequenceDiagram` that follows one request through the code: Client -> Controller -> Service ->
Repository / adapters -> event publisher -> listener, using `alt` for the main rule-violation branch.
### State Diagram
A Mermaid `stateDiagram-v2` of the main entity's lifecycle, matching the enum and allowed transitions.
### Activity Diagram
A Mermaid `flowchart TD` of the decision logic inside the core business-rule method.
