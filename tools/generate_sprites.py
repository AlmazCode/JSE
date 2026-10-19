#!/usr/bin/env python3
"""Rebuild JSE's original 32px sprites using only Python's standard library."""
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/sprites'
PALETTES = {
    'forest/player': {'o': '#142E23', 'g': '#258554', 'G': '#60C779', 's': '#F1C9A5',
                      'b': '#7B4B32', 'w': '#F6F0D1'},
    'forest/enemy': {'o': '#402C39', 'r': '#C94554', 'R': '#FF7D77', 'w': '#FFF0CE',
                     's': '#D8B18D', 'b': '#755343'},
    'forest/pickup': {'o': '#594322', 'g': '#46945B', 'G': '#8BD17E', 'a': '#EDAD39',
                      'A': '#FFDA73', 'w': '#FFF3B8'},
    'space/player': {'o': '#24304E', 'g': '#7C94C4', 'G': '#DBE8FA', 's': '#274968',
                     'b': '#49658B', 'w': '#65E1EC'},
    'space/enemy': {'o': '#3E294F', 'r': '#9B62B8', 'R': '#D6A0EE', 'w': '#67EBEC',
                    's': '#6A467F', 'b': '#EF6897'},
    'space/pickup': {'o': '#204361', 'g': '#3E7BA8', 'G': '#65CADF', 'a': '#3CA5C4',
                     'A': '#88F2EC', 'w': '#E4FFFD'},
}
PATTERNS = {
    'forest/player': [
        '      oooo', '    ooGGGGo', '   oGGGGGGGo', '  ooooooooooo',
        '    ossssso', '    osososo', '     ossso', '   oogggggoo',
        '  osogGgggoso', '  osoGggggoso', '   ooggGggoo', '    ogggggo',
        '    obbobbo', '    obbobbo',
        '   obbbobbbo', '   ooooooooo'],
    'forest/enemy': [
        '', '      oooo', '    ooRRRRoo', '   oRRwwRRRRo',
        '  oRRRwwRRwwRo', ' oRwwRRRRRwwRRo', ' oRwwRRwwRRRRRo', ' oooooooooooooo',
        '    osssssso', '    osossoso', '    osssssso', '     osbbso',
        '    oossssoo', '   obbbbbbbbo', '   oooooooooo', ''],
    'forest/pickup': [
        '', '       oggo', '      ogGGgo', '     ogGGgo',
        '      oggo', '     oooooo', '    oAAAAAAo', '   oAAwwAAAao',
        '   oAAwAAAAao', '   oAAAAAAaao', '   oAAAAAaaao', '    oAaaaaao',
        '     oaaaao', '      oooo', '', ''],
    'space/player': [
        '     oooooo', '    oGGGGGGo', '   oGssssssGo', '   oGswssssGo',
        '   oGsswwwsGo', '   oGssssssGo', '    oGGGGGGo', '  oooggggggooo',
        '  oGogGwwGgoGo', '  oGogGGGGgoGo', '  oGoggggggoGo', '   ooGGGGGGoo',
        '    oGGooGGo', '    oGGooGGo', '   obbboobbbo', '   oooooooooo'],
    'space/enemy': [
        '      obbo', '       oo', '    oooooooo', '   oRRRRRRRRo',
        '   oRwwRRwwRo', '   oRwwRRwwRo', '   oRRRssRRRo', '    osssssso',
        '  oosRRRRRRsoo', '  oRoRRbbRRoRo', '  oRoRRRRRRoRo', '   ooRRRRRRoo',
        '    oRRooRRo', '    oRRooRRo',
        '   osssoossso', '   oooooooooo'],
    'space/pickup': [
        '', '       oo', '      oAAo', '     oAwAAo',
        '    oAAwAAAo', '   ogAAwAAAAo', '  oggAAwAAAAAo', '  oggAAwAAAAAo',
        '   ogAAwAAAao', '    ogAAAAao', '     ogAAao', '      oAAo',
        '       oo', '', '', ''],
}

def chunk(kind, data):
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))

def generate(name, rows):
    assert len(rows) == 16 and all(len(row) <= 16 for row in rows), name
    palette = {key: bytes.fromhex(value[1:]) + b'\xff' for key, value in PALETTES[name].items()}
    pixels = []
    for row in rows:
        row = row.ljust(16)
        expanded = b''.join(palette.get(char, b'\0\0\0\0') * 2 for char in row)
        pixels.extend([b'\0' + expanded] * 2)
    data = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 32, 32, 8, 6, 0, 0, 0))
    data += chunk(b'IDAT', zlib.compress(b''.join(pixels), 9)) + chunk(b'IEND', b'')
    path = ROOT / f'{name}.png'
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)

if __name__ == '__main__':
    for name, rows in PATTERNS.items():
        generate(name, rows)
