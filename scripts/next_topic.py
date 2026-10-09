#!/usr/bin/env python3
"""Step 1 of the daily routine: decide whether to write today, and what.

Prints JSON. When "action" is "generate", it also
  - writes .routine/brief.md, the complete writing brief for the routine, and
  - creates the runnable example skeleton under examples/<domain>/<topic>/.

    python scripts/next_topic.py --me dhulipalla599 [--force] [--topic "Fraud Detection"]
"""
import argparse
import datetime as dt
import json
import subprocess
import sys
from zoneinfo import ZoneInfo

from jinja2 import Environment, FileSystemLoader, StrictUndefined

from common import (DOCS, ROOT, class_name, enabled_stages, front_matter, java_identifier,
                    load_yaml, parse_topic_branch, required_headings, slugify)


def remote_heads() -> dict[str, str]:
    """{branch: sha} for every branch on GitHub."""
    try:
        out = subprocess.run(["git", "ls-remote", "--heads", "origin"], cwd=ROOT,
                             check=True, capture_output=True, text=True, timeout=60).stdout
    except Exception as exc:  # noqa: BLE001
        print(f"warning: could not list remote branches: {exc}", file=sys.stderr)
        return {}
    heads = {}
    for line in out.splitlines():
        sha, _, ref = line.partition("\t")
        if ref.startswith("refs/heads/"):
            heads[ref[len("refs/heads/"):]] = sha
    return heads


def pending_topics(cfg: dict) -> list[tuple[str, str, str]]:
    """Topic branches waiting for review: [(branch, date, 'domain/topic')].

    Branches that point at the same commit as main hold no work and are ignored.
    """
    heads = remote_heads()
    main_sha = heads.get("main")
    result = []
    for branch, sha in heads.items():
        day, key = parse_topic_branch(branch, cfg.get("branch_prefix", "feature/"))
        if key and sha != main_sha:
            result.append((branch, day, key))
    return result


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
    else:  # domain with the fewest finished topics goes next; ties keep file order
        d = min(candidates, key=lambda x: len(x["topics"]) - len(remaining(x)))
    return d, remaining(d)[0]


def scaffold_example(ex: dict, replacements: dict[str, str]) -> None:
    """Copy the template into ex['path'], filling in the __PLACEHOLDERS__."""
    src = ROOT / ex["template"]
    dest = ROOT / ex["path"]
    if dest.exists():
        return  # keep work from an earlier attempt
    for f in sorted(p for p in src.rglob("*") if p.is_file()):
        rel = str(f.relative_to(src))
        for key, value in replacements.items():
            rel = rel.replace(key, value)
        target = dest / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        text = f.read_text(encoding="utf-8")
        for key, value in replacements.items():
            text = text.replace(key, value)
        target.write_text(text, encoding="utf-8")


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
    prefix = cfg.get("branch_prefix", "feature/")

    authors = cfg.get("authors") or []
    maintainers = cfg.get("maintainers") or []
    if args.me not in authors:
        return skip(f"{args.me} is not listed under authors in config.yaml")
    todays_author = authors[today.toordinal() % len(authors)]
    if todays_author != args.me:
        return skip(f"today is {todays_author}'s day to write")

    # Who today's commit is credited to: alternates daily through commit_authors,
    # otherwise the routine owner. The reviewer is always someone else.
    commit_authors = cfg.get("commit_authors") or []
    commit_author = commit_authors[today.toordinal() % len(commit_authors)] if commit_authors else args.me
    others = [m for m in maintainers if m != commit_author]
    if others:
        reviewer = others[today.toordinal() % len(others)]
    else:
        reviewer = maintainers[today.toordinal() % len(maintainers)] if maintainers else ""

    pending = pending_topics(cfg)
    published = {f"{p.parent.name}/{p.stem}": p for p in DOCS.glob("*/*.md")
                 if p.name != "index.md" and p.parent.name != "qa"}
    made_today = any(day == today.isoformat() for _, day, _ in pending) or any(
        str(front_matter(p).get("generated")) == today.isoformat() for p in published.values()
    )
    if not args.force and made_today:
        return skip("a topic was already written today")
    if not args.force and len(pending) >= cfg.get("max_open_prs", 3):
        return skip(f"{len(pending)} topic PRs are waiting for review; pausing")

    taken = set(published) | {key for _, _, key in pending}
    choice = pick_topic(cfg, domains, taken, args.topic or None)
    if not choice:
        return skip("no remaining topics; add more to domains.yaml")
    domain, topic = choice
    tslug = slugify(topic)
    path = f"docs/{domain['slug']}/{tslug}.md"
    branch = f"{prefix}{today.isoformat()}--{domain['slug']}--{tslug}"
    repo = cfg.get("repo", "")
    files_to_commit = [path]

    # ---------------------------------------------------------------- example
    ex_cfg = cfg.get("examples") or {}
    example = {}
    if ex_cfg.get("enabled"):
        ex_path = f"{ex_cfg.get('folder', 'examples')}/{domain['slug']}/{tslug}"
        package = ".".join([ex_cfg.get("group_id", "com.example"),
                            java_identifier(domain["slug"]), java_identifier(tslug)])
        example = {
            "path": ex_path,
            "url": f"https://github.com/{repo}/tree/main/{ex_path}" if repo else ex_path,
            "template": ex_cfg.get("template", "templates/java-spring-boot"),
            "package": package,
            "app_class": class_name(topic),
            "java_version": str(ex_cfg.get("java_version", "21")),
            "spring_boot_version": str(ex_cfg.get("spring_boot_version", "3.5.5")),
            "build_command": ex_cfg.get("build_command", "mvn -B -ntp verify"),
        }
        page_url = f"https://github.com/{repo}/blob/main/{path}" if repo else f"../../../{path}"
        scaffold_example(example, {
            "__PACKAGE_PATH__": package.replace(".", "/"),
            "__PACKAGE__": package,
            "__APP_CLASS__": example["app_class"],
            "__GROUP_ID__": package.rsplit(".", 1)[0],
            "__ARTIFACT_ID__": f"{domain['slug']}-{tslug}"[:80],
            "__TITLE__": topic,
            "__DOMAIN__": domain["name"],
            "__JAVA_VERSION__": example["java_version"],
            "__SPRING_BOOT_VERSION__": example["spring_boot_version"],
            "__EXAMPLE_PATH__": ex_path,
            "__PAGE_PATH__": path,
            "__PAGE_URL__": page_url,
        })
        files_to_commit.append(ex_path)

    # ------------------------------------------------------------------ brief
    env = Environment(loader=FileSystemLoader(str(ROOT / "prompts")), undefined=StrictUndefined)
    ctx = {"domain": domain["name"], "topic": topic, "stack": cfg["stack"],
           "domain_kind": domain.get("kind", "business"), "example": example or {"path": "", "url": ""},
           "audience": cfg.get("audience", "engineers"), "code_style": cfg.get("code_style", "concise"),
           "code_tabs": cfg.get("code_tabs") or []}
    parts = [env.get_template("system.md").render(**ctx)]
    for stage in enabled_stages(cfg):
        parts.append(env.get_template(stage["prompt"].split("/", 1)[1]).render(**ctx))

    example_line = (f"> **Runnable code:** [`{example['path']}`]({example['url']})\n\n" if example else "")
    skeleton = (
        "---\n"
        f"title: {json.dumps(topic)}\n"
        f"domain: {json.dumps(domain['name'])}\n"
        f"generated: '{today.isoformat()}'\n"
        f"author: {commit_author}\n"
        f"reviewer: {reviewer}\n"
        + (f"example: {example['path']}\n" if example else "")
        + "---\n\n"
        f"# {topic}\n\n"
        f"> **Domain:** {domain['name']} · **Author:** [@{commit_author}](https://github.com/{commit_author})"
        f" · **Published:** {today:%B %d, %Y}\n\n"
        + example_line
        + "...all sections below, in order...\n\n"
        "## Practitioner Notes\n\n"
        "> _Reviewer: add real-world experience, corrections or gotchas here before approving._\n"
    )
    order = (
        "1. Build the runnable example first (instructions in the RUNNABLE EXAMPLE part below) "
        "and get its build and tests passing.\n"
        "2. Then write the page, so the code excerpts and UML diagrams match the real code.\n"
        if example else "1. Write the page.\n"
    )
    brief = (
        f"# Writing brief: {domain['name']} / {topic}\n\n"
        f"Write the page `{path}`"
        + (f" and the runnable example in `{example['path']}/`" if example else "") + ".\n\n"
        "## Order of work\n\n" + order + "\n"
        "## Page skeleton (copy the front matter and header exactly)\n\n"
        "````markdown\n" + skeleton + "````\n\n"
        "## Required ## headings, in this order\n\n"
        + "\n".join(f"{i}. {h}" for i, h in enumerate(required_headings(cfg), 1))
        + "\n\n## Instructions\n\n" + "\n\n---\n\n".join(parts) + "\n"
    )
    out = ROOT / ".routine" / "brief.md"
    out.parent.mkdir(exist_ok=True)
    out.write_text(brief, encoding="utf-8")

    identity = (cfg.get("git_identities") or {}).get(commit_author, "")
    name, _, email = identity.partition(" <")
    print(json.dumps({
        "action": "generate", "domain": domain["name"], "topic": topic,
        "path": path, "example_path": example.get("path", ""),
        "build_command": example.get("build_command", ""),
        "files_to_commit": files_to_commit,
        "branch": branch,
        "commit_author": commit_author, "reviewer": reviewer, "brief": ".routine/brief.md",
        "commit_message": f"docs({domain['slug']}): add {topic}"
                          + (" with runnable example" if example else ""),
        "git_author_name": name.strip(), "git_author_email": email.rstrip(">").strip(),
    }, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
