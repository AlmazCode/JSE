#!/usr/bin/env python3
"""Render the reviewed report content and current source/test evidence to an editable DOCX."""
import argparse
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET
from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor

ROOT = Path(__file__).resolve().parents[1]


def evidence():
    files = sorted((ROOT / 'target/surefire-reports').glob('TEST-*.xml'))
    if not files:
        raise SystemExit('Run ./mvnw clean verify before building the report.')
    total = errors = failures = skipped = 0
    for file in files:
        attrs = ET.parse(file).getroot().attrib
        total += int(attrs['tests']); errors += int(attrs.get('errors', 0))
        failures += int(attrs.get('failures', 0)); skipped += int(attrs.get('skipped', 0))
    if errors or failures:
        raise SystemExit('Report requires a passing test suite.')
    return {'TEST_COUNT': str(total), 'TEST_CLASSES': str(len(files)), 'SKIPPED': str(skipped)}


def source_excerpt(path, marker, max_lines=28):
    lines = (ROOT / path).read_text().splitlines()
    start = next((i for i, line in enumerate(lines) if marker in line), None)
    if start is None:
        raise ValueError(f'Missing source marker {marker!r} in {path}')
    depth = 0; opened = False; selected = []
    for line in lines[start:]:
        selected.append(line)
        depth += line.count('{') - line.count('}')
        opened |= '{' in line
        if opened and depth == 0:
            break
        if not opened and line.rstrip().endswith(';'):
            break
    if len(selected) > max_lines:
        raise ValueError(f'Excerpt exceeds {max_lines} lines: {path} {marker}')
    indent = min(len(line) - len(line.lstrip()) for line in selected if line.strip())
    return '\n'.join(line[indent:] for line in selected), start + 1


def shade(cell, fill):
    node = OxmlElement('w:shd'); node.set(qn('w:fill'), fill)
    cell._tc.get_or_add_tcPr().append(node)


def field(paragraph, instruction):
    node = OxmlElement('w:fldSimple'); node.set(qn('w:instr'), instruction)
    paragraph._p.append(node)


def hyperlink(paragraph, text, url):
    relation = paragraph.part.relate_to(url, 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink', is_external=True)
    link = OxmlElement('w:hyperlink'); link.set(qn('r:id'), relation)
    run = OxmlElement('w:r'); properties = OxmlElement('w:rPr')
    color = OxmlElement('w:color'); color.set(qn('w:val'), '174A74'); properties.append(color)
    underline = OxmlElement('w:u'); underline.set(qn('w:val'), 'single'); properties.append(underline)
    run.append(properties); value = OxmlElement('w:t'); value.text = text; run.append(value)
    link.append(run); paragraph._p.append(link)


def render(data, output):
    values = evidence()
    def text(value):
        for key, content in values.items(): value = value.replace('{' + key + '}', content)
        return value
    document = Document()
    document.core_properties.author = 'JSE project team'
    document.core_properties.title = 'Java Simple Engine - Technical Report'
    document.core_properties.subject = 'Software Design Patterns: six coordinated patterns'
    document.core_properties.keywords = 'Bridge, Decorator, Factory Method, Abstract Factory, Strategy, Observer'
    section = document.sections[0]
    section.page_width = Inches(8.27); section.page_height = Inches(11.69)
    section.top_margin = Inches(0.68); section.bottom_margin = Inches(0.65)
    section.left_margin = section.right_margin = Inches(0.72)
    section.header_distance = section.footer_distance = Inches(0.28)
    section.different_first_page_header_footer = True
    normal = document.styles['Normal']; normal.font.name = 'Calibri'; normal.font.size = Pt(10.5)
    normal.paragraph_format.space_after = Pt(6); normal.paragraph_format.line_spacing = 1.08
    for name, size in [('Title', 30), ('Heading 1', 18), ('Heading 2', 12), ('Heading 3', 11)]:
        style = document.styles[name]; style.font.name = 'Calibri'; style.font.size = Pt(size)
        style.font.color.rgb = RGBColor.from_string('173F5F')
    document.styles['Caption'].font.size = Pt(9)
    document.styles['Caption'].font.color.rgb = RGBColor.from_string('4E5965')
    header = section.header.paragraphs[0]; header.text = 'JSE  /  SOFTWARE DESIGN PATTERNS'
    header.runs[0].font.size = Pt(8); header.runs[0].font.color.rgb = RGBColor.from_string('596675')
    footer = section.footer.paragraphs[0]; footer.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    footer.add_run('Technical report  |  '); field(footer, 'PAGE')
    for run in footer.runs: run.font.size = Pt(8)
    cover = document.add_paragraph(); cover.paragraph_format.space_before = Pt(90)
    run = cover.add_run('JAVA SIMPLE ENGINE'); run.bold = True; run.font.size = Pt(30); run.font.color.rgb = RGBColor.from_string('173F5F')
    document.add_paragraph(data['subtitle'], style='Subtitle')
    document.add_paragraph('Software Design Patterns\nTeam technical report', style='Heading 2')
    document.add_paragraph('\n'.join(data['contributors']))
    document.add_paragraph('2 November 2026\nVersion 0.1.0 — consolidated implementation\nDraft for team review')
    summary = document.add_paragraph(data['cover_summary']); summary.paragraph_format.space_before = Pt(22)
    figure_number = listing_number = 0
    for page in data['pages']:
        document.add_page_break(); document.add_heading(page['title'], level=1)
        for block in page['blocks']:
            kind = block['type']
            if kind == 'p': document.add_paragraph(text(block['text']))
            elif kind == 'h': document.add_heading(text(block['text']), level=2)
            elif kind == 'bullets':
                for entry in block['items']: document.add_paragraph(text(entry), style='List Bullet')
            elif kind == 'steps':
                for i, entry in enumerate(block['items'], 1): document.add_paragraph(f'{i}. {text(entry)}')
            elif kind == 'table':
                table = document.add_table(rows=1, cols=len(block['headers'])); table.style = 'Light Shading Accent 1'
                for i, title in enumerate(block['headers']): table.rows[0].cells[i].text = title
                repeat = OxmlElement('w:tblHeader'); table.rows[0]._tr.get_or_add_trPr().append(repeat)
                for row in block['rows']:
                    for cell, value in zip(table.add_row().cells, row): cell.text = text(str(value))
                for row in table.rows:
                    for cell in row.cells:
                        for paragraph in cell.paragraphs:
                            paragraph.paragraph_format.space_after = Pt(4)
                            for run in paragraph.runs: run.font.size = Pt(9)
                document.add_paragraph().paragraph_format.space_after = Pt(0)
            elif kind == 'code':
                listing_number += 1
                source, line = source_excerpt(block['path'], block['marker'], block.get('max_lines', 28))
                document.add_paragraph(f'Listing {listing_number}. {block["caption"]} ({block["path"]}:{line})', style='Caption')
                table = document.add_table(rows=1, cols=1); cell = table.cell(0, 0); shade(cell, 'F0F3F6')
                p = cell.paragraphs[0]; p.paragraph_format.space_after = Pt(3); p.paragraph_format.space_before = Pt(3)
                run = p.add_run(source); run.font.name = 'Courier New'; run.font.size = Pt(8)
                document.add_paragraph().paragraph_format.space_after = Pt(0)
            elif kind == 'literal':
                p = document.add_paragraph(); run = p.add_run(block['text']); run.font.name = 'Courier New'; run.font.size = Pt(9)
            elif kind == 'image':
                figure_number += 1
                path = ROOT / block['path']
                if not path.is_file(): raise FileNotFoundError(path)
                paragraph = document.add_paragraph(); paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                shape = paragraph.add_run().add_picture(str(path), width=Inches(block.get('width', 6.45)))
                max_height = Inches(block.get('max_height', 7.5))
                if shape.height > max_height:
                    shape.width = int(shape.width * max_height / shape.height); shape.height = max_height
                caption = document.add_paragraph(f'Figure {figure_number}. {block["caption"]}', style='Caption')
                caption.paragraph_format.keep_with_next = False
            elif kind == 'ref':
                paragraph = document.add_paragraph(text(block['text']))
                if block.get('url'): hyperlink(paragraph, block.get('label', block['url']), block['url'])
            else: raise ValueError(kind)
    output.parent.mkdir(parents=True, exist_ok=True); document.save(output)
    print(json.dumps({'output': str(output), 'test_evidence': values, 'planned_pages': len(data['pages']) + 1,
                      'figures': figure_number, 'source_listings': listing_number}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--output', type=Path, default=ROOT / 'docs/report/JSE-Technical-Report.docx')
    args = parser.parse_args(); data = json.loads((ROOT / 'docs/report/report-content.json').read_text())
    render(data, args.output)
