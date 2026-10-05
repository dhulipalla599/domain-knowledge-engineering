#!/usr/bin/env python3
"""Step 1 of the daily routine: decide whether to write today, and what.

Prints JSON. When "action" is "generate", it also writes .routine/brief.md,
the complete writing brief for the Claude session.

    python scripts/next_topic.py --me dhulipalla599 [--force] [--topic "Fraud Detection"]
"""
import argparse
import datetime as dt
import json
import subprocess
import sys
from zoneinfo import ZoneInfo

from jinja2 import Environment, FileSystemLoader, StrictUndefined

from common import BRANCH_PREFIX, DOCS, ROOT, front_matter, load_yaml, required_headings, slugify


def remote_topic_branches() -> list[str]:
    """claude/content-* branches on GitHub = topics waiting in a PR."""
    try:
        out = subprocess.run(["git", "ls-remote", "--heads", "origin"], cwd=ROOT,
                             check=True, capture_output=True, text=True, timeout=60).stdout
    except Exception as exc:  # noqa: BLE001
        print(f"warning: could not list remote branches: {exc}", file=sys.stderr)
        return []
    refs = [line.split("refs/heads/", 1)[1] for line in out.splitlines() if "refs/heads/" in line]
    return [r for r in refs if r.startswith(BRANCH_PREFIX)]


def parse_branch(branch: str):
    # claude/content-2026-10-06--banking--fraud-detection
    parts = branch[len(BRANCH_PREFIX):].split("--")
    return (parts[0], f"{parts[1]}/{parts[2]}") if len(parts) == 3 else (None, None)


def pick_topic(cfg, domains, taken, only_topic):
    enabled = [d for d in domains if d.get("enabled", True)]
    if only_topic:
        for d in enabled:
            for t in d["topics"]:
                if t.strip().lower() == only_topic.strip().lower():
                    return d, t
        return None

    def remaining(d):
        return [t for t in d["topics"] if f"{d['slug']}/{slugify(t)}" not in taken]

    candidates = [d for d in enabled if remaining(d)]
    if not candidates:
        return None
    if cfg.get("selection", "round_robin") == "sequential":
        d = candidates[0]
    else:  # domain with the fewest finished topics goes next
        d = min(candidates, key=lambda x: len(x["topics"]) - len(remaining(x)))
    return d, remaining(d)[0]


def skip(reason: str) -> int:
    print(json.dumps({"action": "skip", "reason": reason}, indent=2))
    return 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--me", required=True, help="GitHub username of the routine owner")
    ap.add_argument("--force", action="store_true", help="ignore the daily and open-PR limits")
    ap.add_argument("--topic", default="", help="exact topic name from domains.yaml")
    ap.add_argument("--date", default="", help="override today's date (YYYY-MM-DD), for testing")
    args = ap.parse_args()

    cfg = load_yaml("config.yaml")
    domains = load_yaml("domains.yaml")["domains"]
    tz = ZoneInfo(cfg.get("timezone", "America/New_York"))
    today = dt.date.fromisoformat(args.date) if args.date else dt.datetime.now(tz).date()

    authors = cfg.get("authors") or []
    maintainers = cfg.get("maintainers") or []
    if args.me not in authors:
        return skip(f"{args.me} is not listed under authors in config.yaml")
    todays_author = authors[today.toordinal() % len(authors)]
    if todays_author != args.me:
        return skip(f"today is {todays_author}'s day to write")

    if len(authors) > 1:
        others = [m for m in maintainers if m != args.me]
        reviewer = others[0] if others else ""
    else:
        reviewer = maintainers[today.toordinal() % len(maintainers)] if maintainers else ""

    branches = remote_topic_branches()
    pending = [parse_branch(b) for b in branches]
    published = {f"{p.parent.name}/{p.stem}": p for p in DOCS.glob("*/*.md") if p.name != "index.md"}
    made_today = any(day == today.isoformat() for day, _ in pending) or any(
        str(front_matter(p).get("generated")) == today.isoformat() for p in published.values()
    )
    if not args.force and made_today:
        return skip("a topic was already written today")
    if not args.force and len(branches) >= cfg.get("max_open_prs", 3):
        return skip(f"{len(branches)} topic PRs are waiting for review; pausing")

    taken = set(published) | {key for _, key in pending if key}
    choice = pick_topic(cfg, domains, taken, args.topic or None)
    if not choice:
        return skip("no remaining topics; add more to domains.yaml")
    domain, topic = choice
    tslug = slugify(topic)
    path = f"docs/{domain['slug']}/{tslug}.md"
    branch = f"{BRANCH_PREFIX}{today.isoformat()}--{domain['slug']}--{tslug}"

    env = Environment(loader=FileSystemLoader(str(ROOT / "prompts")), undefined=StrictUndefined)
    ctx = {"domain": domain["name"], "topic": topic, "stack": cfg["stack"],
           "audience": cfg.get("audience", "engineers"), "code_style": cfg.get("code_style", "concise")}
    parts = [env.get_template("system.md").render(**ctx)]
    for stage in cfg["stages"]:
        if stage.get("enabled", True):
            parts.append(env.get_template(stage["prompt"].split("/", 1)[1]).render(**ctx))

    skeleton = (
        "---\n"
        f"title: {json.dumps(topic)}\n"
        f"domain: {json.dumps(domain['name'])}\n"
        f"generated: '{today.isoformat()}'\n"
        f"author: {args.me}\n"
        f"reviewer: {reviewer}\n"
        "---\n\n"
        f"# {topic}\n\n"
        f"> **Domain:** {domain['name']} · **Generated:** {today:%B %d, %Y} with Claude.\n\n"
        "...all sections below, in order...\n\n"
        "## Practitioner Notes\n\n"
        "> _Reviewer: add real-world experience, corrections or gotchas here before approving._\n"
    )
    brief = (
        f"# Writing brief: {domain['name']} / {topic}\n\n"
        f"Write ONE file: `{path}`\n\n"
        "## Page skeleton (copy the front matter and header exactly)\n\n"
        "````markdown\n" + skeleton + "````\n\n"
        "## Required ## headings, in this order\n\n"
        + "\n".join(f"{i}. {h}" for i, h in enumerate(required_headings(cfg), 1))
        + "\n\n## Instructions\n\n" + "\n\n---\n\n".join(parts) + "\n"
    )
    out = ROOT / ".routine" / "brief.md"
    out.parent.mkdir(exist_ok=True)
    out.write_text(brief, encoding="utf-8")

    print(json.dumps({
        "action": "generate", "domain": domain["name"], "topic": topic,
        "path": path, "branch": branch, "reviewer": reviewer,
        "brief": ".routine/brief.md",
        "commit_message": f"docs({domain['slug']}): add {topic}",
    }, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
