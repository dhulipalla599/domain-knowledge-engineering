#!/usr/bin/env python3
"""Fail if any commit in a range is credited to an AI assistant instead of a maintainer.

    python scripts/check_attribution.py origin/main..HEAD [--report out.md]

Flags commits whose author or committer is Claude / noreply@anthropic.com, and
commit messages with Co-Authored-By lines or session links naming Claude.
Fix a flagged branch with: git rebase -i, then reword / amend --reset-author.
"""
import argparse
import pathlib
import re
import subprocess
import sys

BAD_IDENTITY = re.compile(r"(?i)(^claude\b|@anthropic\.com)")
BAD_MESSAGE = re.compile(r"(?im)^(?:co-authored-by:.*(?:claude|anthropic)|.*claude\.ai/code/session|claude-session:).*$")
SEP = "\x1e"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("range")
    ap.add_argument("--report", default="")
    args = ap.parse_args()

    fmt = SEP.join(["%h", "%an", "%ae", "%cn", "%ce", "%B"]) + "\x1d"
    out = subprocess.run(["git", "log", "--no-merges", f"--format={fmt}", args.range],
                         check=True, capture_output=True, text=True).stdout
    problems = []
    for rec in filter(str.strip, out.split("\x1d")):
        sha, an, ae, cn, ce, body = rec.strip("\n").split(SEP, 5)
        if BAD_IDENTITY.search(an) or BAD_IDENTITY.search(ae):
            problems.append(f"`{sha}` is authored by {an} <{ae}>")
        if BAD_IDENTITY.search(cn) or BAD_IDENTITY.search(ce):
            problems.append(f"`{sha}` is committed by {cn} <{ce}>")
        for m in BAD_MESSAGE.finditer(body):
            problems.append(f"`{sha}` message contains `{m.group(0).strip()}`")

    if problems:
        report = ("### ❌ Commit attribution\n\nCommits must be credited only to the maintainers:\n\n"
                  + "\n".join(f"- {p}" for p in problems) + "\n")
    else:
        report = "### ✅ Commit attribution\n\nAll commits are credited to the maintainers.\n"
    print(report)
    if args.report:
        pathlib.Path(args.report).write_text(report, encoding="utf-8")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
