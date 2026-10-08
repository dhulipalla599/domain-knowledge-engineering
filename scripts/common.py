"""Shared helpers for the routine and the GitHub workflows."""
import pathlib
import re

import yaml

ROOT = pathlib.Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
# Topic branch: <prefix>YYYY-MM-DD--<domain-slug>--<topic-slug>
TOPIC_BRANCH = re.compile(r"(\d{4}-\d{2}-\d{2})--([a-z0-9-]+?)--([a-z0-9-]+)$")


def load_yaml(name: str) -> dict:
    return yaml.safe_load((ROOT / name).read_text(encoding="utf-8"))


def slugify(text: str) -> str:
    text = text.lower().replace("&", " and ")
    return re.sub(r"[^a-z0-9]+", "-", text).strip("-")


def java_identifier(slug: str) -> str:
    """'hl7-and-fhir-interoperability' -> 'hl7_and_fhir_interoperability'."""
    ident = re.sub(r"[^a-z0-9]+", "_", slug.lower()).strip("_")
    return ident if ident and not ident[0].isdigit() else f"t_{ident}"


def class_name(text: str) -> str:
    """'Customer Onboarding and KYC' -> 'CustomerOnboardingAndKycApplication'."""
    words = re.findall(r"[A-Za-z0-9]+", text)
    name = "".join(w[:1].upper() + w[1:].lower() for w in words) or "Example"
    if name[0].isdigit():
        name = "T" + name
    return name[:60] + "Application"


def front_matter(path: pathlib.Path) -> dict:
    m = re.match(r"---\n(.*?)\n---\n", path.read_text(encoding="utf-8"), re.S)
    return (yaml.safe_load(m.group(1)) or {}) if m else {}


def enabled_stages(cfg: dict) -> list[dict]:
    examples_on = (cfg.get("examples") or {}).get("enabled", False)
    return [s for s in cfg["stages"]
            if s.get("enabled", True) and (s["id"] != "code" or examples_on)]


def required_headings(cfg: dict) -> list[str]:
    """The ## headings each page must contain, taken from the enabled prompts."""
    heads = []
    for stage in enabled_stages(cfg):
        text = (ROOT / stage["prompt"]).read_text(encoding="utf-8")
        heads += re.findall(r"(?m)^## (.+?)\s*$", text)
    return heads + ["Practitioner Notes"]


def parse_topic_branch(branch: str, prefix: str):
    """Return (date, 'domain/topic') for a topic branch, else (None, None).

    Accepts '<prefix>...' and the fallback '<app>/<prefix>...' used when a push to <prefix> is rejected.
    """
    for p in (prefix, f"claude/{prefix}"):
        if branch.startswith(p):
            m = TOPIC_BRANCH.match(branch[len(p):])
            if m:
                return m.group(1), f"{m.group(2)}/{m.group(3)}"
    return None, None
