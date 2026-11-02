#!/usr/bin/env python3
"""Update report contents in LibreOffice and export a tagged PDF.

Run with LibreOffice's Python/UNO installation (python3-uno on Debian/Ubuntu).
The Java build does not depend on this publishing tool.
"""

import argparse
import subprocess
import tempfile
import time
from pathlib import Path

import uno
from com.sun.star.beans import PropertyValue
from com.sun.star.connection import NoConnectException

ROOT = Path(__file__).resolve().parents[1]


def property_value(name, value):
    property_node = PropertyValue()
    property_node.Name = name
    property_node.Value = value
    return property_node


def export(source, destination):
    with tempfile.TemporaryDirectory(prefix="jse-report-") as profile:
        pipe_name = Path(profile).name
        process = subprocess.Popen(
            [
                "libreoffice",
                "-env:UserInstallation=" + Path(profile).as_uri(),
                "--headless",
                "--nologo",
                "--nodefault",
                "--nofirststartwizard",
                f"--accept=pipe,name={pipe_name};urp;StarOffice.ServiceManager",
            ],
            stdout=subprocess.DEVNULL,
        )
        document = None
        desktop = None
        try:
            context = uno.getComponentContext()
            resolver = context.ServiceManager.createInstanceWithContext(
                "com.sun.star.bridge.UnoUrlResolver", context
            )
            deadline = time.monotonic() + 20
            while True:
                try:
                    remote = resolver.resolve(
                        f"uno:pipe,name={pipe_name};urp;StarOffice.ComponentContext"
                    )
                    break
                except NoConnectException:
                    if process.poll() is not None or time.monotonic() > deadline:
                        raise RuntimeError("LibreOffice did not start its report export service.")
                    time.sleep(0.2)
            desktop = remote.ServiceManager.createInstanceWithContext(
                "com.sun.star.frame.Desktop", remote
            )
            document = desktop.loadComponentFromURL(
                source.resolve().as_uri(),
                "_blank",
                0,
                (property_value("Hidden", True), property_value("UpdateDocMode", 3)),
            )
            if document is None:
                raise RuntimeError(f"LibreOffice could not open {source}")
            if document.getDocumentIndexes().getCount() == 0:
                bookmark = document.getBookmarks().getByName("JSE_CONTENTS")
                contents = document.createInstance("com.sun.star.text.ContentIndex")
                contents.Title = "Contents"
                contents.CreateFromOutline = True
                contents.CreateFromMarks = False
                contents.Level = 1
                document.Text.insertTextContent(bookmark.getAnchor(), contents, False)
            document.refresh()
            indexes = document.getDocumentIndexes()
            for _ in range(2):
                for index in range(indexes.getCount()):
                    indexes.getByIndex(index).update()
                document.refresh()
            document.store()
            document.storeToURL(
                destination.resolve().as_uri(),
                (
                    property_value("FilterName", "writer_pdf_Export"),
                    property_value("Overwrite", True),
                    property_value(
                        "FilterData",
                        (
                            property_value("UseTaggedPDF", True),
                            property_value("ExportBookmarks", True),
                            property_value("EmbedStandardFonts", True),
                        ),
                    ),
                ),
            )
            print(destination)
        finally:
            if document is not None:
                document.close(True)
            if desktop is not None:
                desktop.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source", type=Path, default=ROOT / "docs/report/JSE-Technical-Report.docx"
    )
    parser.add_argument("--output", type=Path)
    arguments = parser.parse_args()
    export(arguments.source, arguments.output or arguments.source.with_suffix(".pdf"))
