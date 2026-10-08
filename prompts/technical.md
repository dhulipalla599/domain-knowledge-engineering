Domain: {{ domain }}
Topic: {{ topic }}

Target stack:
- Language: {{ stack.language }}
- Backend: {{ stack.backend }}
- Frontend: {{ stack.frontend }}
- Database: {{ stack.database }}
- Messaging: {{ stack.messaging }}
- Cloud: {{ stack.cloud }}
- Compute: {{ stack.compute }}
- Infrastructure as code: {{ stack.iac }}
- Observability: {{ stack.observability }}

Write the TECHNICAL half of the page, staying consistent with the terms, actors and flows from the business half. Use exactly these sections:

## From Business to System
A table mapping each major business concept/step to the system component, data entity and event that implements it.

## Architecture
A Mermaid `flowchart LR` showing services, data stores, the event bus and external systems, followed by a short explanation of the service boundaries and why they were drawn there.

## Data Model
The core tables as {{ stack.database }} DDL (keep it to the essential 3-6 tables), plus a Mermaid `erDiagram`.

## APIs
The key REST endpoints as a table (Method | Path | Purpose), then one example request/response in JSON for the most important endpoint.

## Events
The domain events published to {{ stack.messaging }}: Event | Producer | Consumers | Key fields. Note ordering and idempotency concerns.

## Backend Implementation
Short excerpts from the runnable example in `{{ example.path }}`: the method that enforces the key business rule, and the controller endpoint that calls it. Copy them from the files you wrote; never show code here that differs from the example. Link to the full source.

## Frontend Screen
One concise {{ stack.frontend }} component for the most important user-facing screen.

## Cloud Deployment
How this runs on {{ stack.cloud }} using {{ stack.compute }}: the main managed services, a short {{ stack.iac }} snippet for the most important resource, plus security, scaling and observability ({{ stack.observability }}) notes.

## Non-Functional Requirements
Availability, latency, consistency, audit and compliance expectations specific to this topic, and how the design meets them.
