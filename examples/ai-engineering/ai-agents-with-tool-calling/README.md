# AI Agents with Tool Calling (runnable example)

Part of **AI and LLM Engineering** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/ai-engineering/ai-agents-with-tool-calling.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/ai-engineering/ai-agents-with-tool-calling.md)

## What this example shows
A claims-assistant **agent loop** for a fictional insurer. The model (a deterministic stub behind the `LlmClient` interface) proposes tool calls; the service validates each one against an allow-list, executes it, feeds the result back, and stops with a final answer. It demonstrates:

- an **allow-list** of tools (`lookup_claim`, `schedule_callback`, `issue_payment`); unknown tools are recorded as `DENIED`
- a **human approval gate** for the money-moving tool, with a four-eyes rule (the requester cannot approve)
- a **step limit** (4 tool calls) that fails runaway loops
- argument validation inside the tools, because model output is untrusted
- a run lifecycle enforced in the domain class: `RUNNING -> AWAITING_APPROVAL -> RUNNING -> COMPLETED | FAILED`
- domain events named after Kafka topics, consumed by an audit listener

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/ai-engineering/ai-agents-with-tool-calling
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path, two read-only tools, no approval needed:

```bash
curl -s -X POST localhost:8080/api/agent-runs -H 'Content-Type: application/json' \
  -d '{"userId":"ana-agent","goal":"What is the status of CLM-1001 and book a callback"}'
```
Expected: HTTP 201, `"status":"COMPLETED"`, two tool calls, and a `finalAnswer` containing `Claim CLM-1001 for Ana Torres is UNDER_REVIEW`.

Payment pauses for approval:

```bash
curl -s -X POST localhost:8080/api/agent-runs -H 'Content-Type: application/json' \
  -d '{"userId":"ana-agent","goal":"Pay 250 USD on CLM-1002"}'
```
Expected: `"status":"AWAITING_APPROVAL"` and one `issue_payment` call with status `PENDING_APPROVAL`.

Rule violation, the requester approves their own run:

```bash
curl -s -X POST localhost:8080/api/agent-runs/2/approval -H 'Content-Type: application/json' \
  -d '{"approver":"ana-agent","approved":true}'
```
Expected: HTTP 403 ProblemDetail, `"detail":"The requester cannot approve their own agent run"`. Repeat with `"approver":"supervisor-1"` and the run becomes `COMPLETED` with `Payment of 250 queued for CLM-1002`.

Other rules: `"goal":"Delete CLM-1003"` gives a `DENIED` tool call; `"goal":"Check CLM-1001 forever"` ends `FAILED` after 4 calls; approving a finished run gives 409; a blank goal gives 400.

## Project structure
| Class | Package | Responsibility |
|---|---|---|
| `AgentRun`, `RunStatus` | domain | Run entity and lifecycle with legal transitions |
| `ToolCall`, `ToolCallStatus` | domain | One tool invocation and its outcome |
| `Claim` | domain | Seeded claim data the tools read |
| `AgentEvents` | domain | Events named after Kafka topics |
| `AgentRunRepository`, `ToolCallRepository`, `ClaimRepository` | repository | Spring Data JPA access |
| `AgentService` | service | The agent loop and all rules |
| `Tool`, `LlmClient`, `ToolRegistry` | service | Ports and the tool allow-list |
| `AgentRunController`, DTO records, `ApiExceptionHandler` | api | REST endpoints and `ProblemDetail` errors |
| `KeywordPlannerLlmClient` | integration | Deterministic stub model |
| `LookupClaimTool`, `ScheduleCallbackTool`, `IssuePaymentTool`, `ClaimArguments` | integration | Tool adapters and argument validation |
| `AuditTrailListener` | integration | `@EventListener` stand-in for Kafka consumers |

## From example to production
| Here | Production on the configured stack |
|---|---|
| H2 in memory (`postgres` profile available) | PostgreSQL (Amazon RDS or Aurora) with Flyway migrations |
| `ApplicationEventPublisher` and `@EventListener` | Apache Kafka topics `agent.run.started`, `agent.toolcall.executed`, `agent.approval.requested`, `agent.run.finished`, published through a transactional outbox |
| `KeywordPlannerLlmClient` | An adapter calling a hosted model through an LLM gateway, with timeouts, retries, and prompt and tool-schema versioning |
| Tools return canned text | Real claims and payments services called with service credentials scoped per tool and idempotency keys |
| Requester-differs check | Identity from OIDC/JWT claims and role-based approval policy |
| Log lines | OpenTelemetry traces per run and step, CloudWatch metrics and logs, deployed on Amazon EKS with Terraform |
