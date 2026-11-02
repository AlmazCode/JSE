#!/usr/bin/env python3
"""Build the technical report from Markdown, verified results and actual Java excerpts."""

import argparse
import json
import re
from datetime import datetime, timezone
import xml.etree.ElementTree as ET
from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

from evidence import ROOT, source_digest, test_results

BODY_FONT = "Liberation Serif"
HEADING_FONT = "Liberation Sans"
CODE_FONT = "Liberation Mono"


def substitutions():
    evidence = json.loads((ROOT / "docs/verification.json").read_text(encoding="utf-8"))
    if evidence["source_sha256"] != source_digest():
        raise ValueError("Application sources changed after the recorded verification.")
    current = test_results()
    for key in ("tests", "failures", "errors", "skipped", "classes"):
        if current[key] != evidence["tests"][key]:
            raise ValueError("Current tests differ from docs/verification.json; verify again.")
    version = ET.parse(ROOT / "pom.xml").getroot().find("{*}version").text
    return {
        "VERSION": version,
        "TEST_COUNT": str(current["tests"]),
        "TEST_CLASSES": str(current["classes"]),
        "TEST_JAVA": ", ".join(evidence["tests"]["java_runtimes"]),
        "ROUTE_CYCLES": str(evidence["desktop"]["route_cycles"]),
        "PLATFORM": evidence["desktop"]["platform"],
        "DESKTOP_JAVA": evidence["desktop"]["java"],
        "NATIVE_ENTRY_NOTE": (
            "Native Robot Enter activated the arena in the recorded run."
            if evidence["desktop"]["native_robot_enter"]
            else "Native Robot Enter did not activate the arena in the recorded run; the verifier used the installed Swing action binding as a fallback."
        ),
    }


def source_excerpt(path, marker):
    lines = (ROOT / path).read_text(encoding="utf-8").splitlines()
    start = next((i for i, line in enumerate(lines) if marker in line), None)
    if start is None:
        raise ValueError(f"Missing source marker {marker!r}: {path}")
    selected = []
    depth = 0
    opened = False
    for line in lines[start:]:
        selected.append(line)
        depth += line.count("{") - line.count("}")
        opened |= "{" in line
        if (opened and depth == 0) or (not opened and line.rstrip().endswith(";")):
            break
    if not opened or depth != 0 or len(selected) > 42:
        raise ValueError(
            f"Excerpt is not a complete method of at most 42 lines: {path}:{start + 1}"
        )
    indentation = min(len(line) - len(line.lstrip()) for line in selected if line.strip())
    return "\n".join(line[indentation:] for line in selected), start + 1


def field(paragraph, instruction):
    node = OxmlElement("w:fldSimple")
    node.set(qn("w:instr"), instruction)
    paragraph._p.append(node)


def inline(paragraph, text):
    """Render the small inline subset used by this report: code, emphasis and links."""
    parts = re.split(r"(`[^`]+`|\*\*[^*]+\*\*|\[[^\]]+\]\([^)]+\))", text)
    for part in parts:
        if part.startswith("`"):
            run = paragraph.add_run(part[1:-1])
            run.font.name = CODE_FONT
            run.font.size = Pt(9)
        elif part.startswith("**"):
            paragraph.add_run(part[2:-2]).bold = True
        elif match := re.fullmatch(r"\[([^\]]+)\]\(([^)]+)\)", part):
            relation = paragraph.part.relate_to(
                match[2],
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink",
                is_external=True,
            )
            link = OxmlElement("w:hyperlink")
            link.set(qn("r:id"), relation)
            run = OxmlElement("w:r")
            properties = OxmlElement("w:rPr")
            underline = OxmlElement("w:u")
            underline.set(qn("w:val"), "single")
            properties.append(underline)
            run.append(properties)
            value = OxmlElement("w:t")
            value.text = match[1]
            run.append(value)
            link.append(run)
            paragraph._p.append(link)
        else:
            paragraph.add_run(part)


def configure(document):
    properties = document.core_properties
    properties.author = "Sabirzhanov Emil; Baktiyarova Aruzhan; Roziyeva Yasmina"
    properties.title = "Java Simple Engine — Technical Report"
    properties.subject = "Software Design Patterns"
    properties.keywords = (
        "JSE, Bridge, Decorator, Factory Method, Abstract Factory, Strategy, Observer"
    )
    properties.comments = ""
    properties.revision = 1
    properties.created = properties.modified = datetime.now(timezone.utc)
    section = document.sections[0]
    section.page_width = Cm(21)
    section.page_height = Cm(29.7)
    section.top_margin = section.bottom_margin = Cm(2.2)
    section.left_margin = section.right_margin = Cm(2.2)
    section.header_distance = section.footer_distance = Cm(1.1)
    section.different_first_page_header_footer = True

    normal = document.styles["Normal"]
    normal.font.name = BODY_FONT
    normal.font.size = Pt(11)
    normal.font.color.rgb = RGBColor(0, 0, 0)
    normal.paragraph_format.line_spacing = 1.12
    normal.paragraph_format.space_after = Pt(7)
    normal.paragraph_format.widow_control = True
    for name, size in (("Title", 24), ("Heading 1", 15), ("Heading 2", 12)):
        style = document.styles[name]
        style.font.name = HEADING_FONT
        style.font.size = Pt(size)
        style.font.color.rgb = RGBColor(0, 0, 0)
        style.font.bold = name != "Title"
        style.paragraph_format.space_before = Pt(15)
        style.paragraph_format.space_after = Pt(7)
        style.paragraph_format.keep_with_next = True
    caption = document.styles["Caption"]
    caption.font.name = HEADING_FONT
    caption.font.size = Pt(9)
    caption.font.italic = False
    caption.font.color.rgb = RGBColor.from_string("333333")
    caption.paragraph_format.space_after = Pt(9)

    language = OxmlElement("w:lang")
    language.set(qn("w:val"), "en-GB")
    normal.element.get_or_add_rPr().append(language)
    update = OxmlElement("w:updateFields")
    update.set(qn("w:val"), "true")
    document.settings.element.append(update)

    header = section.header.paragraphs[0]
    header.text = "JAVA SIMPLE ENGINE  /  TECHNICAL REPORT"
    header.runs[0].font.name = HEADING_FONT
    header.runs[0].font.size = Pt(8)
    footer = section.footer.paragraphs[0]
    footer.paragraph_format.tab_stops.add_tab_stop(Cm(16.6), WD_TAB_ALIGNMENT.RIGHT)
    footer.add_run("JSE/TR-001  ·  Rev. 1.0\t")
    field(footer, "PAGE")
    footer.add_run(" / ")
    field(footer, "NUMPAGES")
    for run in footer.runs:
        run.font.name = HEADING_FONT
        run.font.size = Pt(8)

    # Word's template theme must not override the selected fonts or add an accent border.
    for style in document.styles:
        for fonts in style.element.findall(".//" + qn("w:rFonts")):
            for attribute in ("asciiTheme", "hAnsiTheme", "eastAsiaTheme", "cstheme"):
                fonts.attrib.pop(qn("w:" + attribute), None)
    title_properties = document.styles["Title"].element.get_or_add_pPr()
    for border in title_properties.findall(qn("w:pBdr")):
        title_properties.remove(border)


def table(document, rows):
    result = document.add_table(rows=0, cols=len(rows[0]))
    result.style = "Table Grid"
    for index, row in enumerate(rows):
        cells = result.add_row().cells
        for cell, text in zip(cells, row):
            paragraph = cell.paragraphs[0]
            paragraph.paragraph_format.space_after = Pt(4)
            paragraph.paragraph_format.space_before = Pt(3)
            inline(paragraph, text)
            for run in paragraph.runs:
                run.font.size = Pt(9)
                run.font.name = HEADING_FONT
                run.bold = index == 0
            if index == 0:
                shading = OxmlElement("w:shd")
                shading.set(qn("w:fill"), "EEEEEE")
                cell._tc.get_or_add_tcPr().append(shading)
        row_properties = result.rows[-1]._tr.get_or_add_trPr()
        row_properties.append(OxmlElement("w:cantSplit"))
        if index == 0:
            row_properties.append(OxmlElement("w:tblHeader"))
    document.add_paragraph().paragraph_format.space_after = Pt(0)


def code(document, text):
    paragraph = document.add_paragraph()
    paragraph.paragraph_format.keep_together = True
    paragraph.paragraph_format.line_spacing = Pt(10.2)
    paragraph.paragraph_format.space_before = Pt(4)
    paragraph.paragraph_format.space_after = Pt(10)
    shading = OxmlElement("w:shd")
    shading.set(qn("w:fill"), "F5F5F5")
    paragraph._p.get_or_add_pPr().append(shading)
    run = paragraph.add_run(text)
    run.font.name = CODE_FONT
    run.font.size = Pt(8.2)


def render(source, destination):
    values = substitutions()
    markdown = source.read_text(encoding="utf-8")
    for key, value in values.items():
        markdown = markdown.replace("{" + key + "}", value)
    if re.search(r"\{[A-Z_]+\}", markdown):
        raise ValueError("Unresolved report substitution.")
    lines = markdown.splitlines()
    document = Document()
    configure(document)
    figure_count = listing_count = 0
    index = 0
    while index < len(lines):
        line = lines[index]
        if not line.strip():
            index += 1
            continue
        if line == "<!-- contents -->":
            document.add_page_break()
            paragraph = document.add_paragraph()
            start = OxmlElement("w:bookmarkStart")
            start.set(qn("w:id"), "1")
            start.set(qn("w:name"), "JSE_CONTENTS")
            end = OxmlElement("w:bookmarkEnd")
            end.set(qn("w:id"), "1")
            paragraph._p.extend((start, end))
            document.add_page_break()
        elif line.startswith("# "):
            title = document.add_paragraph(line[2:], style="Title")
            title.paragraph_format.space_before = Pt(35)
            title.paragraph_format.space_after = Pt(18)
        elif line.startswith("## "):
            document.add_heading(line[3:], level=1)
        elif line.startswith("### "):
            document.add_heading(line[4:], level=2)
        elif line.startswith("<!-- source:"):
            path, marker, caption = (part.strip() for part in line[12:-3].split("|", 2))
            excerpt, source_line = source_excerpt(path, marker)
            listing_count += 1
            paragraph = document.add_paragraph(
                f"Listing {listing_count}. {caption}", style="Caption"
            )
            paragraph.paragraph_format.keep_with_next = True
            code(document, excerpt)
            path_paragraph = document.add_paragraph(f"{path}:{source_line}", style="Caption")
            path_paragraph.paragraph_format.space_before = Pt(0)
        elif match := re.fullmatch(r"!\[([^\]]*)\]\(([^)]+)\)", line):
            image_path = (source.parent / match[2]).resolve()
            if not image_path.is_file():
                raise FileNotFoundError(image_path)
            figure_count += 1
            paragraph = document.add_paragraph()
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
            paragraph.paragraph_format.keep_with_next = True
            shape = paragraph.add_run().add_picture(str(image_path), width=Cm(16.5))
            maximum = Cm(17.3)
            if shape.height > maximum:
                shape.width = int(shape.width * maximum / shape.height)
                shape.height = maximum
            document.add_paragraph(f"Figure {figure_count}. {match[1]}", style="Caption")
        elif line.startswith("|"):
            rows = []
            while index < len(lines) and lines[index].startswith("|"):
                cells = [cell.strip() for cell in lines[index].strip("|").split("|")]
                if not all(re.fullmatch(r":?-+:?", cell) for cell in cells):
                    rows.append(cells)
                index += 1
            table(document, rows)
            continue
        elif line.startswith("```"):
            block = []
            index += 1
            while index < len(lines) and not lines[index].startswith("```"):
                block.append(lines[index])
                index += 1
            code(document, "\n".join(block))
        else:
            paragraph_lines = [line]
            index += 1
            while index < len(lines) and lines[index].strip():
                paragraph_lines.append(lines[index])
                index += 1
            inline(document.add_paragraph(), "\n".join(paragraph_lines))
            continue
        index += 1
    destination.parent.mkdir(parents=True, exist_ok=True)
    document.save(destination)
    print(
        json.dumps(
            {
                "output": str(destination),
                "figures": figure_count,
                "listings": listing_count,
                "evidence": values,
            }
        )
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=ROOT / "docs/report/report.md")
    parser.add_argument(
        "--output", type=Path, default=ROOT / "docs/report/JSE-Technical-Report.docx"
    )
    arguments = parser.parse_args()
    render(arguments.source, arguments.output)
