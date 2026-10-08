# Domain Knowledge for Full Stack Engineers

Business domains explained simply, then mapped to real system design and **runnable code**.
Every topic comes with a use case diagram, UML diagrams of the code's control flow, and a small Spring Boot project you can run with one command.
A new topic is written by a Claude routine on schedule, reviewed through a pull request, and published to the website.

**Website:** https://dhulipalla599.github.io/domain-knowledge-engineering/

Maintainers: [@dhulipalla599](https://github.com/dhulipalla599) · [@naveenks720](https://github.com/naveenks720)

## How it works

```mermaid
flowchart LR
    A["Claude routine<br/>(schedule)"] --> B["next_topic.py<br/>picks topic, scaffolds example"]
    B --> C["Claude writes runnable code<br/>+ page with UML"]
    C --> D["Push feature/DATE--domain--topic"]
    D --> E["GitHub Action opens PR<br/>renders diagrams, builds and tests code"]
    E --> F{"Reviewed within 24h?"}
    F -- "Approve + merge" --> G["main"]
    F -- "No" --> H["Bot auto-merges"] --> G
    G --> I["Deploy MkDocs site<br/>GitHub Pages"]
```

No API key is used: the writing runs as a Claude Code routine on a Claude Pro/Max subscription, and everything else runs on free GitHub Actions.

| File / folder | Purpose |
|---|---|
| `ROUTINE.md` | The steps the Claude routine follows on every run |
| `ROUTINE_PROMPT.md` | The short prompt saved in the routine (points at `ROUTINE.md`) |
| `config.yaml` | Tech stack, page sections, runnable-code settings, authors, branches, auto-merge rules |
| `domains.yaml` | Topic backlog, in order |
| `prompts/` | What each part of the page must contain |
| `templates/java-spring-boot/` | Skeleton copied for each topic's runnable example |
| `examples/<domain>/<topic>/` | Runnable Spring Boot example for each topic (`mvn spring-boot:run`) |
| `scripts/next_topic.py` | Picks today's topic, scaffolds the example, writes the brief for Claude |
| `scripts/check_page.py` | Checks sections, diagrams (use case, class, sequence, state, ER), and builds the example |
| `scripts/auto_merge.py` | Merges PRs nobody handled within 24 hours |
| `scripts/build_catalog.py` | Builds the catalog page and site navigation |
| `docs/` | Published pages, one folder per domain |
| `.github/workflows/` | Open PR on push (with checks), example builds, hourly auto-merge, website deployment |

First-time setup: [SETUP.md](SETUP.md). Review process: [docs/contributing.md](docs/contributing.md).

## Run any example

```bash
git clone https://github.com/dhulipalla599/domain-knowledge-engineering
cd domain-knowledge-engineering/examples/banking/customer-onboarding-and-kyc
mvn spring-boot:run      # needs Java 21 + Maven; no database or API keys
```
