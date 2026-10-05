"""Shared helpers for the routine and the GitHub workflows."""
import pathlib
import re

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
BRANCH_PREFIX = "claude/content-"


def load_yaml(name: str) -> dict:
    return yaml.safe_load((ROOT / name).read_text(encoding="utf-8"))


def slugify(text: str) -> str:
    text = text.lower().replace("&", " and ")
    return re.sub(r"[^a-z0-9]+", "-", text).strip("-")


def front_matter(path: pathlib.Path) -> dict:
    m = re.match(r"---\n(.*?)\n---\n", path.read_text(encoding="utf-8"), re.S)
    return (yaml.safe_load(m.group(1)) or {}) if m else {}


def required_headings(cfg: dict) -> list[str]:
    """The ## headings each page must contain, taken from the enabled prompts."""
    heads = []
    for stage in cfg["stages"]:
        if stage.get("enabled", True):
            text = (ROOT / stage["prompt"]).read_text(encoding="utf-8")
            heads += re.findall(r"(?m)^## (.+?)\s*$", text)
    return heads + ["Practitioner Notes"]
