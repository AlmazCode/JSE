"""Read verification results and identify the application sources they describe."""

import hashlib
import json
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def source_digest(root=ROOT):
    """Hash paths as well as contents; ignore generated documentation and build output."""
    paths = [root / "pom.xml"]
    for directory in ("src", "config"):
        paths.extend(path for path in (root / directory).rglob("*") if path.is_file())
    digest = hashlib.sha256()
    for path in sorted(paths):
        digest.update(path.relative_to(root).as_posix().encode("utf-8") + b"\0")
        digest.update(path.read_bytes() + b"\0")
    return digest.hexdigest()


def test_results(root=ROOT):
    files = sorted((root / "target/surefire-reports").glob("TEST-*.xml"))
    if not files:
        raise ValueError("Run ./mvnw clean verify first.")
    totals = dict(tests=0, failures=0, errors=0, skipped=0)
    runtimes = set()
    for path in files:
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, "0"))
        for property_node in suite.findall("properties/property"):
            if property_node.get("name") == "java.runtime.version":
                runtimes.add(property_node.get("value"))
    if totals["failures"] or totals["errors"] or totals["skipped"]:
        raise ValueError("The submission requires a passing suite with no skipped tests.")
    return {**totals, "classes": len(files), "java_runtimes": sorted(runtimes)}


def current_evidence():
    desktop_path = ROOT / "target/verification/desktop.json"
    if not desktop_path.is_file():
        raise ValueError(
            "Run tools/DesktopVerification.java on a desktop before recording evidence."
        )
    desktop = json.loads(desktop_path.read_text(encoding="utf-8"))
    if not desktop.get("success"):
        raise ValueError("Desktop verification did not succeed.")
    jar_hash = hashlib.sha256((ROOT / "target/jse-demo.jar").read_bytes()).hexdigest()
    if desktop.get("jar_sha256") != jar_hash:
        raise ValueError("JAR changed after desktop verification; run the desktop verifier again.")
    return {"source_sha256": source_digest(), "tests": test_results(), "desktop": desktop}


if __name__ == "__main__":
    destination = ROOT / "docs/verification.json"
    destination.write_text(json.dumps(current_evidence(), indent=2) + "\n", encoding="utf-8")
    print(destination.relative_to(ROOT))
