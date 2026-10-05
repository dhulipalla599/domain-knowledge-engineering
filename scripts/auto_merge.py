#!/usr/bin/env python3
"""Merge generated PRs that have waited longer than auto_merge_after_hours.

Skipped when a PR is a draft, has the `hold` label, or has changes requested.
"""
import datetime as dt
import json
import pathlib
import subprocess

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent


def gh(*args: str) -> str:
    return subprocess.run(["gh", *args], check=True, capture_output=True, text=True).stdout


def main() -> None:
    cfg = yaml.safe_load((ROOT / "config.yaml").read_text())
    limit = dt.timedelta(hours=cfg.get("auto_merge_after_hours", 24))
    now = dt.datetime.now(dt.timezone.utc)
    prs = json.loads(gh(
        "pr", "list", "--label", "auto-generated", "--state", "open", "--limit", "50",
        "--json", "number,title,createdAt,isDraft,labels,reviewDecision",
    ) or "[]")

    merged = 0
    for pr in prs:
        age = now - dt.datetime.fromisoformat(pr["createdAt"].replace("Z", "+00:00"))
        labels = {l["name"] for l in pr["labels"]}
        tag = f"#{pr['number']} {pr['title']}"
        if age < limit:
            print(f"wait   {tag} ({age.total_seconds() / 3600:.1f}h old)")
        elif pr["isDraft"] or "hold" in labels or pr["reviewDecision"] == "CHANGES_REQUESTED":
            print(f"skip   {tag} (draft, on hold, or changes requested)")
        else:
            try:
                gh("pr", "merge", str(pr["number"]), "--merge", "--delete-branch")
                print(f"merged {tag}")
                merged += 1
            except subprocess.CalledProcessError as exc:
                print(f"error  {tag}: {exc.stderr.strip()}")

    if merged:
        # Merges made with the workflow token don't trigger push workflows,
        # so start the site deployment explicitly.
        gh("workflow", "run", "deploy-docs.yml", "--ref", "main")
        print("triggered deploy-docs.yml")


if __name__ == "__main__":
    main()
