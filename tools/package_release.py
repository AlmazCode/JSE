#!/usr/bin/env python3
"""Build and verify a submission bundle from the exact clean Git commit."""

import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET
import zipfile
from datetime import datetime, timezone
from pathlib import Path

from evidence import ROOT, current_evidence, source_digest, test_results

OUT = ROOT / "release"


def git(*arguments):
    return subprocess.check_output(["git", *arguments], cwd=ROOT, text=True).strip()


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify_inputs():
    if git("status", "--porcelain"):
        raise ValueError("Commit the verified source and documentation before packaging.")
    recorded = json.loads((ROOT / "docs/verification.json").read_text(encoding="utf-8"))
    if recorded != current_evidence():
        raise ValueError(
            "Current tests, desktop evidence or sources differ from the recorded release."
        )
    version = ET.parse(ROOT / "pom.xml").getroot().find("{*}version").text
    report = ROOT / "docs/report/JSE-Technical-Report.docx"
    with zipfile.ZipFile(report) as archive:
        body = ET.fromstring(archive.read("word/document.xml"))
        text = " ".join(body.itertext())
    for expected in (f"Release {version}", f"{recorded['tests']['tests']} tests"):
        if expected not in text:
            raise ValueError(f"Report does not identify the current release: {expected}")
    if "Draft for team review" in text:
        raise ValueError("The report is still marked as a draft.")
    pdf = ROOT / "docs/report/JSE-Technical-Report.pdf"
    if not pdf.read_bytes().startswith(b"%PDF-"):
        raise ValueError("The PDF report is missing or invalid.")
    with zipfile.ZipFile(ROOT / "target/jse-demo.jar") as archive:
        manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8")
        if f"Implementation-Version: {version}" not in manifest:
            raise ValueError("Packaged JAR version does not match pom.xml.")
        for name in archive.namelist():
            if name.endswith(".class") and int.from_bytes(archive.read(name)[6:8], "big") != 61:
                raise ValueError(f"Unexpected bytecode target: {name}")
    return version, recorded


def verify_source_archive(archive, recorded):
    """Build the archive itself, not another checkout that may contain extra local files."""
    with tempfile.TemporaryDirectory(prefix="jse-submission-source-") as directory:
        root = Path(directory)
        with zipfile.ZipFile(archive) as source:
            source.extractall(root)
        source_root = root / "JSE"
        if source_digest(source_root) != recorded["source_sha256"]:
            raise ValueError("Archived sources differ from verified application sources.")
        wrapper = source_root / ("mvnw.cmd" if os.name == "nt" else "mvnw")
        if os.name != "nt":
            wrapper.chmod(0o755)
        command = [str(wrapper), "--batch-mode", "--no-transfer-progress", "clean", "verify"]
        print("Verifying the extracted source archive...", flush=True)
        log = root / "build.log"
        with log.open("w", encoding="utf-8") as output:
            result = subprocess.run(
                command, cwd=source_root, stdout=output, stderr=subprocess.STDOUT, timeout=180
            )
        if result.returncode:
            raise RuntimeError(
                "Source archive build failed:\n" + log.read_text(encoding="utf-8")[-6000:]
            )
        archive_tests = test_results(source_root)
        for key in ("tests", "failures", "errors", "skipped", "classes"):
            if archive_tests[key] != recorded["tests"][key]:
                raise ValueError("Extracted source test results differ from the recorded release.")
        java_home = os.environ.get("JAVA_HOME")
        java = (
            str(Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java"))
            if java_home
            else "java"
        )
        subprocess.run(
            [
                java,
                "-Djava.awt.headless=true",
                "-jar",
                str(source_root / "target/jse-demo.jar"),
                "--help",
            ],
            cwd=source_root,
            check=True,
            capture_output=True,
            timeout=15,
        )
        return {
            "command": "./mvnw --batch-mode --no-transfer-progress clean verify",
            "exit_code": 0,
            **archive_tests,
        }


def main():
    version, recorded = verify_inputs()
    commit = git("rev-parse", "HEAD")
    OUT.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="jse-release-") as directory:
        stage = Path(directory)
        for source in (
            "target/jse-demo.jar",
            "docs/report/JSE-Technical-Report.pdf",
            "docs/report/JSE-Technical-Report.docx",
        ):
            shutil.copy2(ROOT / source, stage / Path(source).name)
        archive = stage / "JSE-Source.zip"
        with archive.open("wb") as output:
            subprocess.run(
                ["git", "archive", "--format=zip", "--prefix=JSE/", commit],
                cwd=ROOT,
                stdout=output,
                check=True,
            )
        archive_verification = verify_source_archive(archive, recorded)
        launcher = stage / "Run-JSE.sh"
        launcher.write_text(
            '#!/bin/sh\nexec java -jar "$(dirname "$0")/jse-demo.jar" --example game "$@"\n',
            encoding="utf-8",
        )
        launcher.chmod(0o755)
        (stage / "Run-JSE.bat").write_bytes(
            b'@echo off\r\njava -jar "%~dp0jse-demo.jar" --example game %*\r\n'
        )
        (stage / "README.txt").write_text(
            f"JAVA SIMPLE ENGINE — RELEASE {version}\n\n"
            "START\n"
            "Run Run-JSE.sh / Run-JSE.bat, or:\n"
            "  java -jar jse-demo.jar --example game\n"
            "Requires Java 17 or newer and a graphical desktop.\n\n"
            "Menu: Enter Arena, H Showcase, Escape close.\n"
            "Move with WASD/arrows. P pauses; R resets; T changes theme.\n"
            "F1 changes renderer; F2/F3 toggle outline/health bars.\n"
            "Tab selects an enemy; B changes behavior; L toggles the log.\n"
            "Escape returns to the menu.\n\n"
            "DOCUMENTS\n"
            "JSE-Technical-Report.pdf is the final technical report.\n"
            "The DOCX contains the same report in editable form.\n\n"
            "SOURCE\n"
            "Extract JSE-Source.zip, then run ./mvnw clean verify\n"
            "(mvnw.cmd clean verify on Windows). JDK 17+ is required.\n"
            "The first build downloads Maven and build dependencies.\n"
            "The extracted source archive has been independently built and tested.\n\n"
            "VERIFICATION\n"
            f"{recorded['tests']['tests']} tests; zero failures, errors or skips.\n"
            f"Desktop: {recorded['desktop']['platform']}, Java {recorded['desktop']['java']}; "
            f"{recorded['desktop']['route_cycles']} navigation cycles.\n"
            "Physical keyboard coverage is partial; other desktop platforms were not tested.\n"
            "manifest.json records the source commit, evidence and SHA-256 checksums.\n",
            encoding="utf-8",
        )
        artifacts = sorted(stage.iterdir())
        manifest = {
            "release": version,
            "source_commit": commit,
            "source_branch": git("branch", "--show-current"),
            "created_utc": datetime.now(timezone.utc).isoformat(),
            "verification": recorded,
            "source_archive_verification": archive_verification,
            "artifacts": {
                path.name: {"bytes": path.stat().st_size, "sha256": sha256(path)}
                for path in artifacts
            },
        }
        manifest_path = stage / "manifest.json"
        manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
        bundle = stage / "JSE-Submission.zip"
        with zipfile.ZipFile(
            bundle, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9
        ) as output:
            for path in [*artifacts, manifest_path]:
                output.write(path, arcname="JSE-Submission/" + path.name)
        with zipfile.ZipFile(bundle) as output:
            if output.testzip() is not None:
                raise ValueError("Submission ZIP integrity check failed.")
        for path in stage.iterdir():
            shutil.copy2(path, OUT / path.name)
        print(
            json.dumps(
                {
                    "release": version,
                    "commit": commit,
                    "bundle": str(OUT / bundle.name),
                    "source_archive_tests": archive_verification,
                },
                indent=2,
            )
        )


if __name__ == "__main__":
    main()
