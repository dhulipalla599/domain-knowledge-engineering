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
Short excerpts from the runnable example in `{{ example.path }}`: the method that enforces the key business rule, and the controller endpoint that calls it. Copy them from the files you wrote; never show code here that differs from the example. Link to the full source.{% if code_tabs|length > 1 %}
Show every excerpt as content tabs, one per language, in this order: {% for t in code_tabs %}{{ t.label }}{% if not loop.last %}, {% endif %}{% endfor %}.
The {{ code_tabs[0].label }} tab is the exact code from the example. The other tabs show the same logic, names and rules
written idiomatically for that ecosystem (e.g. FastAPI + SQLAlchemy for Python, Express or NestJS for Node.js). Start the section with one quoted line saying the {{ code_tabs[0].label }} tab is the runnable example's code and naming the
framework used on each other tab. Use exactly this syntax (blank lines around each tab, code indented four spaces):

{% for t in code_tabs %}=== "{{ t.label }}"

    ```{{ t.fence }}
    ...
    ```

{% endfor %}{% endif %}

## Frontend Screen
One concise {{ stack.frontend }} component for the most important user-facing screen.

## Cloud Deployment
How this runs on {{ stack.cloud }} using {{ stack.compute }}: the main managed services, a short {{ stack.iac }} snippet for the most important resource, plus security, scaling and observability ({{ stack.observability }}) notes.

## Non-Functional Requirements
Availability, latency, consistency, audit and compliance expectations specific to this topic, and how the design meets them.
