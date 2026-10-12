#!/usr/bin/env python3
"""Checks that every character in the copy exists in the fonts build.py uses."""
import os, sys, unicodedata
from fontTools.ttLib import TTFont
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import build
cmaps = {name: set(TTFont(os.path.join(HERE, 'fonts', f + '.ttf')).getBestCmap()) for name, f in build.FONTS}
missing = {}
for loc, lines in build.COPY.items():
    text = ''.join(lines) + 'Scrolless'
    stack = set(build.font_names(loc, 'Display') + build.font_names(loc, 'Body'))
    for ch in set(text):
        if ch.isspace() or unicodedata.category(ch) in ('Cf',):
            continue
        if not any(ord(ch) in cmaps[f] for f in stack):
            missing.setdefault(loc, set()).add(ch)
for loc, chars in sorted(missing.items()):
    print(loc, ' '.join(f'{c} U+{ord(c):04X}' for c in sorted(chars)))
print('missing glyphs in', len(missing), 'locales')
sys.exit(1 if missing else 0)
