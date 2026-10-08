#!/usr/bin/env python3
"""Reader Q&A: one page per domain, built from qa/<domain-slug>.yml.

Readers submit a question and answer with the "Add a Q&A" issue form. A
contributor reviews it (and may edit the answer in the issue), then adds the
`qa-approved` label. The "Publish approved Q&A" workflow then runs

    python scripts/qa.py publish --issue 12 --approver <login>

which copies the entry into qa/<domain-slug>.yml through a merged pull
request, closes the issue and redeploys the site. Contributors can also add or
edit entries in qa/*.yml directly in a pull request.

build_catalog.py calls render_pages() to write docs/qa/ at build time.
"""
import argparse
import datetime as dt
import json
import pathlib
import re
import subprocess
import sys
import urllib.parse

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
QA_DIR = ROOT / "qa"
DOCS = ROOT / "docs"
FORM_FIELDS = ("Domain", "Related topic (optional)", "Question", "Answer")
APPROVED_LABEL = "qa-approved"


# ------------------------------------------------------------------ storage
class _Literal(str):
    pass


yaml.SafeDumper.add_representer(
    _Literal, lambda d, s: d.represent_scalar("tag:yaml.org,2002:str", s, style="|"))


def load_entries(slug: str) -> list[dict]:
    path = QA_DIR / f"{slug}.yml"
    if not path.exists():
        return []
    return (yaml.safe_load(path.read_text(encoding="utf-8")) or {}).get("entries") or []


def save_entries(slug: str, name: str, entries: list[dict]) -> pathlib.Path:
    def tidy(value):
        if isinstance(value, str) and "\n" in value:
            return _Literal("\n".join(line.rstrip() for line in value.strip().splitlines()) + "\n")
        return value

    path = QA_DIR / f"{slug}.yml"
    path.parent.mkdir(exist_ok=True)
    header = (f"# {name} Q&A, shown at qa/{slug}/ on the website.\n"
              "# Approved reader entries are added automatically; edit or add entries here in a pull request.\n")
    data = {"entries": [{k: tidy(v) for k, v in e.items() if v not in (None, "")} for e in entries]}
    path.write_text(header + yaml.safe_dump(data, sort_keys=False, allow_unicode=True, width=1000),
                    encoding="utf-8")
    return path


# ---------------------------------------------------------------- rendering
def safe_markdown(text: str) -> str:
    """Neutralise raw HTML, attribute lists and script links outside code."""
    out, in_fence = [], False
    for line in text.splitlines():
        if re.match(r"\s*(```|~~~)", line):
            in_fence = not in_fence
            out.append(line)
            continue
        if in_fence:
            out.append(line)
            continue
        parts = re.split(r"(`+[^`]*`+)", line)
        for i in range(0, len(parts), 2):
            p = parts[i].replace("<", "&lt;").replace("{", "&#123;")
            parts[i] = re.sub(r"(?i)\]\(\s*(javascript|data|vbscript):", "](#", p)
        out.append("".join(parts))
    return "\n".join(out)


def ask_url(repo: str, domain: str) -> str:
    query = urllib.parse.urlencode({"template": "qa.yml", "title": "[Q&A] ", "domain": domain})
    return f"https://github.com/{repo}/issues/new?{query}"


def render_pages(domains: list[dict], repo: str) -> list:
    """Write docs/qa/index.md and docs/qa/<slug>.md; return the nav entries."""
    out_dir = DOCS / "qa"
    out_dir.mkdir(exist_ok=True)
    nav, rows = ["qa/index.md"], []
    for d in domains:
        entries = load_entries(d["slug"])
        if not entries and not d.get("enabled", True):
            continue
        lines = [f"# {d['name']} Q&A", "",
                 f"Questions and answers about {d['name']}, written by readers and reviewed by the contributors.", "",
                 f"[Add a question and answer]({ask_url(repo, d['name'])}){{ .md-button }}", ""]
        if not entries:
            lines += ["_No questions yet. Be the first to add one._", ""]
        for e in entries:
            question = " ".join(safe_markdown(str(e["question"])).split())
            meta = [f"Topic: {safe_markdown(str(e['topic']))}"] if e.get("topic") else []
            if e.get("asked_by"):
                meta.append(f"added by @{e['asked_by']}")
            if e.get("approved_by"):
                meta.append(f"approved by @{e['approved_by']}")
            lines += [f"## {question}", ""]
            if meta:
                lines += [f"<small>{' · '.join(meta)}</small>", ""]
            lines += [safe_markdown(str(e["answer"]).strip()), ""]
        (out_dir / f"{d['slug']}.md").write_text("\n".join(lines), encoding="utf-8")
        nav.append({d["name"]: f"qa/{d['slug']}.md"})
        rows.append(f"| [{d['name']}]({d['slug']}.md) | {len(entries)} |")
    (out_dir / "index.md").write_text(
        "# Q&A\n\n"
        "Each domain has its own Q&A page. Anyone can add a question with an answer through a GitHub issue;\n"
        "a contributor reviews it and approves it before it is published here.\n\n"
        "| Domain | Questions |\n|---|---|\n" + "\n".join(rows) + "\n",
        encoding="utf-8")
    return nav


# --------------------------------------------------------------- publishing
def run(*cmd: str, check: bool = True) -> str:
    res = subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)
    if check and res.returncode != 0:
        raise RuntimeError(f"{' '.join(cmd[:3])} failed: {(res.stderr or res.stdout).strip()}")
    return res.stdout.strip()


def parse_form(body: str) -> dict[str, str]:
    """Split an issue-form body into {label: value}. Later fields may contain ### headings."""
    values, positions = {}, []
    start = 0
    for label in FORM_FIELDS:
        m = re.compile(rf"(?m)^### {re.escape(label)}[ \t]*\r?\n").search(body, start)
        if m:
            positions.append((label, m.start(), m.end()))
            start = m.end()
    for i, (label, _, end) in enumerate(positions):
        stop = positions[i + 1][1] if i + 1 < len(positions) else len(body)
        value = body[end:stop].strip()
        values[label] = "" if value == "_No response_" else value
    return values


def noreply_identity(login: str) -> str:
    user_id = run("gh", "api", f"users/{login}", "--jq", ".id", check=False)
    email = f"{user_id}+{login}@users.noreply.github.com" if user_id else f"{login}@users.noreply.github.com"
    return f"{login} <{email}>"


def publish(issue: int, approver: str) -> int:
    cfg = yaml.safe_load((ROOT / "config.yaml").read_text(encoding="utf-8"))
    repo = cfg["repo"]
    domains = yaml.safe_load((ROOT / "domains.yaml").read_text(encoding="utf-8"))["domains"]

    def comment(text: str) -> None:
        run("gh", "issue", "comment", str(issue), "--body", text, check=False)

    perm = run("gh", "api", f"repos/{repo}/collaborators/{approver}/permission", "--jq", ".permission",
               check=False)
    if perm not in ("admin", "maintain", "write"):
        run("gh", "issue", "edit", str(issue), "--remove-label", APPROVED_LABEL, check=False)
        comment(f"Only contributors with write access can approve Q&A entries (@{approver} has `{perm or 'none'}`).")
        return 1

    data = json.loads(run("gh", "issue", "view", str(issue), "--json", "body,author,url"))
    form = parse_form(data["body"] or "")
    domain = next((d for d in domains if d["name"].lower() == form.get("Domain", "").strip().lower()), None)
    if not domain or not form.get("Question") or not form.get("Answer"):
        comment("This issue needs a Domain, a Question and an Answer from the Q&A form before it can be "
                "published. Edit the issue, then add the `qa-approved` label again.")
        run("gh", "issue", "edit", str(issue), "--remove-label", APPROVED_LABEL, check=False)
        return 1

    entry = {
        "question": " ".join(form["Question"].split()),
        "answer": form["Answer"].replace("\r\n", "\n"),
        "topic": form.get("Related topic (optional)", "").strip(),
        "asked_by": data["author"]["login"],
        "approved_by": approver,
        "issue": issue,
        "date": dt.date.today().isoformat(),
    }
    entries = [e for e in load_entries(domain["slug"]) if e.get("issue") != issue] + [entry]
    path = save_entries(domain["slug"], domain["name"], entries)

    branch = f"qa/issue-{issue}"
    run("git", "checkout", "-B", branch)
    run("git", "add", str(path.relative_to(ROOT)))
    run("git", "-c", "user.name=github-actions[bot]",
        "-c", "user.email=41898282+github-actions[bot]@users.noreply.github.com",
        "commit", "--author", noreply_identity(approver),
        "-m", f"qa({domain['slug']}): {entry['question'][:60]}", "-m", f"Closes #{issue}")
    run("git", "push", "--force", "-u", "origin", branch)
    url = run("gh", "pr", "list", "--head", branch, "--state", "open", "--json", "url", "--jq", ".[0].url // empty")
    if not url:
        url = run("gh", "pr", "create", "--base", "main", "--head", branch,
                  "--title", f"Q&A ({domain['name']}): {entry['question'][:80]}",
                  "--body", f"Approved by @{approver} from {data['url']}.")
    merged = subprocess.run(["gh", "pr", "merge", url, "--merge", "--delete-branch"],
                            cwd=ROOT, capture_output=True, text=True)
    if merged.returncode != 0:
        comment(f"Approved. The entry is in {url}; merge it to publish.")
        return 0
    run("gh", "workflow", "run", "deploy-docs.yml", check=False)
    site = re.search(r"(?m)^site_url:\s*(\S+)", (ROOT / "mkdocs.yml").read_text(encoding="utf-8"))
    page = f"{site.group(1).rstrip('/')}/qa/{domain['slug']}/" if site else f"the {domain['name']} Q&A page"
    comment(f"Thanks! Approved by @{approver} and published to {page} (live in a few minutes).")
    run("gh", "issue", "close", str(issue), "--reason", "completed", check=False)
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest="cmd", required=True)
    p = sub.add_parser("publish", help="copy an approved Q&A issue into qa/<domain>.yml")
    p.add_argument("--issue", type=int, required=True)
    p.add_argument("--approver", required=True)
    args = ap.parse_args()
    return publish(args.issue, args.approver)


if __name__ == "__main__":
    sys.exit(main())
