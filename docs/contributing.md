# How We Work

This handbook is run by two engineers, [@dhulipalla599](https://github.com/dhulipalla599) and
[@naveenks720](https://github.com/naveenks720). We both kept running into the same problem: we could build the
services, but we didn't always understand the business they served. So we started writing down each domain the
way we wish someone had explained it to us, from the plain-language idea all the way to running code.

## One topic a day

Every day one of us publishes a topic and the other reviews it. We swap roles daily, so each of us writes
half the pages and reviews the other half.

1. **Pick the topic.** The next topic comes from our backlog in `domains.yaml`. We rotate through the domains
   (Banking, Healthcare, Insurance, ...) so no area falls behind.
2. **Build the example first.** Each topic starts as a small Spring Boot project under `examples/` that
   implements the core flow, its business rules and its events. It has to build and pass its tests before we
   write about it.
3. **Write the page.** The page explains the business first (stakeholders, terms, process, rules), then maps it
   to the system: architecture, data model, APIs, events, code and cloud deployment. Every code excerpt and UML
   diagram is taken from the example, so the page and the code always agree.
4. **Open a pull request.** Pushing the topic branch opens a PR automatically. It checks that every section is
   there, that the diagrams render, that the website builds, and that the example compiles and its tests pass.

## How we review

The PR is assigned to whoever isn't writing that day.

1. Read the page against the checklist in the PR description. To try the example locally:
   `git fetch origin <branch> && git checkout <branch>`, then `cd examples/<domain>/<topic> && mvn spring-boot:run`.
2. Add something real to **Practitioner Notes**: a gotcha from a project we worked on, a correction, or
   something we'd do differently. Open *Files changed*, click the `...` menu on the file, choose *Edit file*,
   and commit to the same branch.
3. **Approve**, then **Merge pull request** using *Create a merge commit*, so the author keeps the credit.

If neither of us gets to it within 24 hours, a bot merges the PR once all checks pass. Adding the `hold` label
or requesting changes stops that.

## Code in more than one language

The runnable examples are Java and Spring Boot, but not everyone reading works in Java. Wherever it makes sense,
the code snippets on a page have tabs for **Java**, **Python** and **Node.js**. Pick a language once and every
snippet on the page switches to it. The Java tab is always the code from the runnable example; the other tabs
show the same logic written the way you'd write it in that ecosystem.

## Q&A: your questions, our review

Each domain has its own [Q&A page](qa/index.md). If you've learned something about a domain, or have a question
with a good answer, add it:

1. Open a [new Q&A issue](https://github.com/dhulipalla599/domain-knowledge-engineering/issues/new?template=qa.yml),
   pick the domain, and write the question and your answer.
2. One of us reviews it. We may tidy up the answer or ask you a follow-up in the issue.
3. When it's ready, we add the `qa-approved` label. It's published to that domain's Q&A page within a few
   minutes, with your name on it, and the issue is closed.

Only maintainers can approve. We can also add or edit entries directly in `qa/<domain>.yml` through a pull
request.

## Changing how the handbook works

| What we want | Where |
|---|---|
| A different stack (e.g. Node.js, Azure) | `stack:` in `config.yaml` (and a new template under `templates/` for runnable code) |
| Different languages on the code tabs | `code_tabs:` in `config.yaml` |
| Runnable examples off, or new Java / Spring Boot versions | `examples:` in `config.yaml` |
| Add, remove or reorder page sections | the `## ` headings in `prompts/` and `stages:` in `config.yaml` |
| New domains or topics | `domains.yaml` (and the domain list in `.github/ISSUE_TEMPLATE/qa.yml`) |
| Rotate across domains vs. finish one at a time | `selection:` in `config.yaml` |

Changes apply from the next topic. Pages already published are not rewritten.
