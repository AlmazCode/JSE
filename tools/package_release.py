#!/usr/bin/env python3
"""Package a clean, verified commit without adding build outputs to Git."""
import hashlib
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'release'


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    if git('status', '--porcelain'):
        raise SystemExit('Commit tracked/untracked changes before packaging.')
    source_commit = git('rev-parse', 'HEAD')
    test_files = sorted((ROOT / 'target/surefire-reports').glob('TEST-*.xml'))
    if not test_files:
        raise SystemExit('Run ./mvnw clean verify first.')
    totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for file in test_files:
        attrs = ET.parse(file).getroot().attrib
        for key in totals:
            totals[key] += int(attrs.get(key, 0))
    if totals['failures'] or totals['errors']:
        raise SystemExit('Refusing to package failing test evidence.')
    OUT.mkdir(exist_ok=True)
    artifacts = []
    for source, name in [('target/jse-demo.jar', 'jse-demo.jar'),
                         ('docs/report/JSE-Technical-Report.pdf', 'JSE-Technical-Report.pdf'),
                         ('docs/report/JSE-Technical-Report.docx', 'JSE-Technical-Report.docx')]:
        target = OUT / name
        shutil.copy2(ROOT / source, target); artifacts.append(target)
    archive = OUT / 'JSE-Source.zip'
    with archive.open('wb') as stream:
        subprocess.run(['git', 'archive', '--format=zip', '--prefix=JSE/', source_commit],
                       cwd=ROOT, stdout=stream, check=True)
    artifacts.append(archive)
    launch = OUT / 'Run-JSE.sh'
    launch.write_text('#!/bin/sh\nexec java -jar "$(dirname "$0")/jse-demo.jar" --example game "$@"\n')
    launch.chmod(0o755); artifacts.append(launch)
    launch_win = OUT / 'Run-JSE.bat'
    launch_win.write_bytes(b'@echo off\r\njava -jar "%~dp0jse-demo.jar" --example game %*\r\n')
    artifacts.append(launch_win)
    readme = OUT / 'README.txt'
    readme.write_text('JSE final integration bundle\n\n'
                      'Run: java -jar jse-demo.jar --example game\n'
                      'Requires JDK/JRE17 or newer and a desktop display.\n'
                      'The shell/batch launchers are convenience entry points.\n\n'
                      'Report: PDF for review/submission, DOCX for editing.\n'
                      'Source: extract JSE-Source.zip and run ./mvnw clean verify\n'
                      '(mvnw.cmd clean verify on Windows).\n\n'
                      'Report status: draft for team review. Check course/cover details\n'
                      'and rehearse individual pattern explanations before submission.\n'
                      'Only Linux desktop behavior has been verified.\n')
    artifacts.append(readme)
    desktop_path = ROOT / '.superpowers/sdd/2026-11-02-unify-and-report/desktop-results.json'
    manifest = {'source_commit': source_commit, 'source_branch': git('branch', '--show-current'),
                'created_utc': datetime.now(timezone.utc).isoformat(), 'verification': totals,
                'test_classes': len(test_files), 'report_status': 'draft for team review',
                'desktop': json.loads(desktop_path.read_text()) if desktop_path.exists() else None,
                'artifacts': {file.name: {'bytes': file.stat().st_size, 'sha256': digest(file)} for file in artifacts}}
    (OUT / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    print(json.dumps({'source_commit': source_commit, 'directory': str(OUT), 'artifacts': list(manifest['artifacts']),
                      'tests': totals}, indent=2))


if __name__ == '__main__':
    main()
