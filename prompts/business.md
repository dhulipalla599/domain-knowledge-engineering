Domain: {{ domain }}
Topic: {{ topic }}

Write the BUSINESS half of the handbook page for this topic, with exactly these sections:

## Explain It Like I'm Five
A short, warm analogy a child would understand (4-6 sentences). Then one sentence connecting the analogy to the real business.

## Business Overview
What this capability is, why the business cares (revenue, risk, regulation, customer experience), and where it sits in the wider {{ domain }} value chain.

## Real-World Example
A concrete walk-through with a fictional customer and company, following one case from start to finish.

## Stakeholders
A table: Stakeholder | What they need | What they fear.

## Key Terms
A table of 8-12 domain terms an engineer will hear in meetings: Term | Plain-English meaning.

## Process Flow
A short narrative of the end-to-end process, then:
1. A Mermaid `flowchart TD` of the main process, including the important failure/exception paths.
2. A Mermaid `sequenceDiagram` showing the main actors and systems for the happy path.

## Business Rules and Edge Cases
The rules that most often break naive implementations (timing, money, compliance, idempotency, partial failure).
