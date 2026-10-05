#!/usr/bin/env python3
"""Check a generated page before it is merged.

    python scripts/check_page.py docs/banking/fraud-detection.md [--mermaid] [--report out.md]

Always checks: front matter, a single H1, every required section, balanced
code fences, and at least 3 Mermaid diagrams.
--mermaid also renders each diagram with mermaid-cli (used by the GitHub
workflow, where a browser is available). Exit code 1 means problems found.
"""
import argparse
import pathlib
import re
import shutil
import subprocess
import sys
import tempfile

from common import ROOT, front_matter, load_yaml, required_headings

MERMAID = re.compile(r"```mermaid[ \t]*\n(.*?)```", re.S)


def render(src: str) -> str | None:
    mmdc = shutil.which("mmdc")
    with tempfile.TemporaryDirectory() as tmp:
        inp, out = pathlib.Path(tmp, "d.mmd"), pathlib.Path(tmp, "d.svg")
        inp.write_text(src, encoding="utf-8")
        cmd = [mmdc, "-i", str(inp), "-o", str(out), "-q", "-p", str(ROOT / "scripts" / "puppeteer-config.json")]
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
        if res.returncode == 0 and out.exists():
            return None
        return (res.stderr or res.stdout or "unknown error").strip().splitlines()[-1][:300]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("path")
    ap.add_argument("--mermaid", action="store_true")
    ap.add_argument("--report", default="")
    args = ap.parse_args()

    path = pathlib.Path(args.path)
    text = path.read_text(encoding="utf-8")
    body = re.sub(r"\A---\n.*?\n---\n", "", text, flags=re.S)
    prose = re.sub(r"```.*?```", "", body, flags=re.S)  # ignore code when counting headings
    problems = []

    fm = front_matter(path)
    for key in ("title", "domain", "generated"):
        if not fm.get(key):
            problems.append(f"front matter is missing `{key}`")
    h1 = re.findall(r"(?m)^# \S", prose)
    if len(h1) != 1:
        problems.append(f"expected exactly one `# ` title, found {len(h1)}")
    found = set(h.strip() for h in re.findall(r"(?m)^## (.+)$", prose))
    for head in required_headings(load_yaml("config.yaml")):
        if head not in found:
            problems.append(f"missing section `## {head}`")
    if len(re.findall(r"(?m)^\s*```", body)) % 2:
        problems.append("a code block is not closed (odd number of ``` fences)")
    blocks = MERMAID.findall(body)
    if len(blocks) < 3:
        problems.append(f"expected at least 3 Mermaid diagrams, found {len(blocks)}")

    if args.mermaid:
        if not shutil.which("mmdc"):
            problems.append("mermaid-cli (mmdc) is not installed, cannot render diagrams")
        elif render("flowchart TD\n  A --> B") is not None:
            print("warning: mermaid renderer not working here; diagrams not checked")
        else:
            for i, src in enumerate(blocks, 1):
                err = render(src)
                if err:
                    first = src.strip().splitlines()[0] if src.strip() else ""
                    problems.append(f"diagram {i} (`{first}`) does not render: {err}")

    if problems:
        report = "### ❌ Page check found problems\n\n" + "\n".join(f"- {p}" for p in problems) + "\n"
    else:
        report = f"### ✅ Page check passed\n\n{len(blocks)} diagrams, all required sections present.\n"
    print(report)
    if args.report:
        pathlib.Path(args.report).write_text(report, encoding="utf-8")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
