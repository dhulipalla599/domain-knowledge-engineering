#!/usr/bin/env python3
"""Check a generated page (and its runnable example) before it is merged.

    python scripts/check_page.py docs/banking/fraud-detection.md [--mermaid] [--build] [--report out.md]

Always checks: front matter, a single H1, every required section, balanced
code fences, a code tab per language in `code_tabs` (Backend Implementation), the Mermaid diagrams the page must have (use case, UML class,
sequence, state, ER), and - when the page names an `example:` folder - that
the example exists, has tests, and has no TODOs left in its README.
--mermaid renders each diagram with mermaid-cli (used in GitHub Actions).
--build   runs the example's Maven build and tests.
Exit code 1 means problems were found.
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
REQUIRED_KINDS = {  # diagram type -> minimum count
    "flowchart": 3,       # use case, process flow, architecture, activity
    "sequenceDiagram": 2, # business happy path + code control flow
    "classDiagram": 1,
    "stateDiagram": 1,
    "erDiagram": 1,
}


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


def diagram_kind(src: str) -> str:
    first = next((l.strip() for l in src.splitlines() if l.strip() and not l.strip().startswith("%%")), "")
    word = first.split()[0] if first else ""
    if word in ("graph", "flowchart"):
        return "flowchart"
    return word.split("-")[0]  # stateDiagram-v2 -> stateDiagram


def check_example(folder: pathlib.Path, build: bool, build_command: str, problems: list[str]) -> str:
    if not folder.is_dir():
        problems.append(f"example folder `{folder.relative_to(ROOT)}` does not exist")
        return ""
    for need in ("pom.xml", "README.md"):
        if not (folder / need).is_file():
            problems.append(f"example is missing `{need}`")
    main_java = list((folder / "src/main/java").rglob("*.java"))
    tests = list((folder / "src/test/java").rglob("*Test*.java"))
    if len(main_java) < 4:
        problems.append(f"example has only {len(main_java)} Java classes; expected a real implementation")
    if len(tests) < 2:
        problems.append(f"example has {len(tests)} test classes; expected unit + API tests")
    readme = folder / "README.md"
    if readme.is_file() and "TODO" in readme.read_text(encoding="utf-8"):
        problems.append("example README.md still contains TODO sections")
    placeholder = re.compile(r"__[A-Z_]+__")
    leftovers = [str(p.relative_to(folder)) for p in folder.rglob("*")
                 if "target" not in p.parts and (placeholder.search(str(p.relative_to(folder)))
                     or (p.is_file() and p.suffix in (".java", ".xml", ".yml", ".md")
                         and placeholder.search(p.read_text(encoding="utf-8", errors="ignore"))))]
    if leftovers:
        problems.append(f"template placeholders left in: {leftovers[:3]}")

    if not build:
        return f"{len(main_java)} classes, {len(tests)} test classes (build not run)"
    if not shutil.which(build_command.split()[0]):
        problems.append(f"`{build_command.split()[0]}` is not installed, cannot build the example")
        return ""
    res = subprocess.run(build_command.split(), cwd=folder, capture_output=True, text=True, timeout=1500)
    if res.returncode != 0:
        tail = "\n".join((res.stdout + res.stderr).strip().splitlines()[-25:])
        problems.append(f"example build/tests failed (`{build_command}`):\n\n```text\n{tail}\n```")
        return ""
    runs = re.findall(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+)", res.stdout)
    total = runs[-1][0] if runs else "?"
    return f"{len(main_java)} classes, build passed, {total} tests passed"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("path")
    ap.add_argument("--mermaid", action="store_true")
    ap.add_argument("--build", action="store_true")
    ap.add_argument("--report", default="")
    args = ap.parse_args()

    cfg = load_yaml("config.yaml")
    path = pathlib.Path(args.path).resolve()
    text = path.read_text(encoding="utf-8")
    body = re.sub(r"\A---\n.*?\n---\n", "", text, flags=re.S)
    prose = re.sub(r"```.*?```", "", body, flags=re.S)  # ignore code when counting headings
    problems: list[str] = []

    fm = front_matter(path)
    for key in ("title", "domain", "generated"):
        if not fm.get(key):
            problems.append(f"front matter is missing `{key}`")
    h1 = re.findall(r"(?m)^# \S", prose)
    if len(h1) != 1:
        problems.append(f"expected exactly one `# ` title, found {len(h1)}")
    found = set(h.strip() for h in re.findall(r"(?m)^## (.+)$", prose))
    for head in required_headings(cfg):
        if head not in found:
            problems.append(f"missing section `## {head}`")
    if len(re.findall(r"(?m)^\s*```", body)) % 2:
        problems.append("a code block is not closed (odd number of ``` fences)")

    tabs = [t["label"] for t in cfg.get("code_tabs") or []]
    backend = re.search(r"(?ms)^## Backend Implementation\s*$(.*?)(?=^## )", body)
    if len(tabs) > 1 and backend:
        missing = [t for t in tabs if f'=== "{t}"' not in backend.group(1)]
        if missing:
            problems.append(f"`## Backend Implementation` has no code tab for: {', '.join(missing)}")

    blocks = MERMAID.findall(body)
    kinds: dict[str, int] = {}
    for src in blocks:
        k = diagram_kind(src)
        kinds[k] = kinds.get(k, 0) + 1
    examples_on = (cfg.get("examples") or {}).get("enabled", False)
    for kind, minimum in REQUIRED_KINDS.items():
        if kind in ("classDiagram", "stateDiagram") and not examples_on:
            continue
        if kinds.get(kind, 0) < minimum:
            problems.append(f"expected at least {minimum} `{kind}` diagram(s), found {kinds.get(kind, 0)}")

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

    example_summary = ""
    if fm.get("example"):
        build_command = (cfg.get("examples") or {}).get("build_command", "mvn -B -ntp verify")
        example_summary = check_example(ROOT / fm["example"], args.build, build_command, problems)
    elif examples_on:
        problems.append("front matter has no `example:` folder, but examples are enabled in config.yaml")

    if problems:
        report = "### ❌ Page check found problems\n\n" + "\n".join(f"- {p}" for p in problems) + "\n"
    else:
        report = (f"### ✅ Page check passed\n\n{len(blocks)} diagrams, all required sections present."
                  + (f" Example: {example_summary}." if example_summary else "") + "\n")
    print(report)
    if args.report:
        pathlib.Path(args.report).write_text(report, encoding="utf-8")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
