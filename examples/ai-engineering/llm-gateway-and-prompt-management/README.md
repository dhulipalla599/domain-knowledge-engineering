# LLM Gateway and Prompt Management (runnable example)

Part of **AI and LLM Engineering** in Domain Knowledge for Full Stack Engineers.
Concept page: [docs/ai-engineering/llm-gateway-and-prompt-management.md](https://github.com/dhulipalla599/domain-knowledge-engineering/blob/main/docs/ai-engineering/llm-gateway-and-prompt-management.md)

## What this example shows
A small LLM gateway for a fictional insurer, Harbor Mutual. Teams call named, versioned prompts through one endpoint. The gateway:

- renders the single ACTIVE version of a prompt and rejects missing variables (400);
- masks e-mail addresses and card numbers before the text reaches a model (`PiiRedactor`);
- enforces a per-team monthly token budget (429) using a row lock;
- fails over from the primary model provider to a fallback one;
- is idempotent when the `Idempotency-Key` header is repeated;
- manages the prompt lifecycle `DRAFT -> APPROVED -> ACTIVE -> RETIRED` with a four-eyes approval rule (403) and illegal transitions rejected (409);
- publishes `PromptActivated` and `LlmCallCompleted` events (stand-ins for Kafka topics `prompt.version.activated` and `llm.call.completed`).

The model is a deterministic stub (`StubLlmClient`), so no API key or network is needed.

## Run it
Requires Java 21 and Maven 3.9+. No database, broker or API key is needed.

```bash
cd examples/ai-engineering/llm-gateway-and-prompt-management
mvn spring-boot:run          # starts on http://localhost:8080
mvn verify                   # runs the tests
```

## Try it
Happy path (seed data: team `claims-team`, prompt `claims-summary` v1 ACTIVE):

```bash
curl -s -X POST localhost:8080/api/gateway/completions -H 'Content-Type: application/json' \
  -d '{"teamId":"claims-team","promptName":"claims-summary","maxTokens":60,"variables":{"notes":"Hail damage to roof, contact jo@example.com"}}'
```
```json
{"callId":1,"provider":"primary-stub","promptName":"claims-summary","promptVersion":1,"output":"[primary-stub] Summarise this insurance claim for an adjuster. Claim notes: Hail damage to","promptTokens":25,"completionTokens":23,"redactions":1}
```

Rule failure: `marketing-team` only has 50 tokens left:

```bash
curl -s -X POST localhost:8080/api/gateway/completions -H 'Content-Type: application/json' \
  -d '{"teamId":"marketing-team","promptName":"claims-summary","variables":{"notes":"Campaign"}}'
```
```json
{"type":"about:blank","title":"Token budget exceeded","status":429,"detail":"Team marketing-team has 50 tokens left but this call may need 274","instance":"/api/gateway/completions"}
```

Other things to try: put `[simulate-outage]` in `notes` to see failover to `fallback-stub`; `GET /api/budgets/claims-team`; `POST /api/prompts` then `/approve` and `/activate`.

## Project structure
| Class | Package | What it does |
|---|---|---|
| `GatewayController` | `api` | REST endpoints for completions, prompts and budgets |
| `ApiModels` | `api` | Request/response records |
| `ApiExceptionHandler` | `api` | Maps errors to `ProblemDetail` responses |
| `GatewayService` | `service` | Core flow: prompt lookup, redaction, budget, failover, event |
| `PromptService` | `service` | Draft, approve (four-eyes) and activate prompts |
| `PiiRedactor` | `service` | Masks e-mails and card numbers |
| `BusinessRuleException` | `service` | Rule violation carrying an HTTP status |
| `PromptVersion`, `PromptStatus` | `domain` | Versioned template and its lifecycle |
| `TeamBudget`, `GatewayCall` | `domain` | Token budget and the call record |
| `PromptActivated`, `LlmCallCompleted` | `domain` | Domain events |
| `LlmClient`, `StubLlmClient`, `LlmClientConfig` | `integration` | Model port, stub, and provider order |
| `AuditTrailListener` | `integration` | Event consumer: audit trail and spend per team |

## From example to production
| Here | Production on the target stack |
|---|---|
| H2 in memory (`postgres` profile available) | PostgreSQL (Amazon RDS or Aurora) with Flyway migrations |
| Spring `ApplicationEventPublisher` | Apache Kafka topics (Amazon MSK), published via a transactional outbox |
| `StubLlmClient` | Adapters for real providers such as Amazon Bedrock, with timeouts, retries and circuit breakers |
| 4-characters-per-token estimate | The provider's real tokenizer and usage numbers |
| Regex PII masking | A dedicated detection service (for example Amazon Comprehend) plus allow-lists |
| Budget in a table | Same, plus per-minute rate limits and alerts in CloudWatch |
| No authentication | OIDC/JWT at the gateway; team identity taken from the token, not the body |
| Single process | Stateless pods on Amazon EKS, OpenTelemetry traces and metrics to CloudWatch |
