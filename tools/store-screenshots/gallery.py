#!/usr/bin/env python3
"""Writes out/index.html showing every built locale."""
import os
ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, 'out')
locales = sorted(d for d in os.listdir(OUT) if os.path.isdir(os.path.join(OUT, d)))
rows = []
for loc in locales:
    shots = ''.join(f'<a href="{loc}/{f}" target="_blank"><img loading="lazy" src="{loc}/{f}"></a>'
                    for f in sorted(os.listdir(os.path.join(OUT, loc))) if f.endswith('.png') and not f.startswith('08'))
    rows.append(f'<section id="{loc}"><h2>{loc}</h2><div class="row">{shots}</div>'
                f'<img class="fg" loading="lazy" src="{loc}/08-feature-graphic.png"></section>')
nav = ''.join(f'<a href="#{l}">{l}</a>' for l in locales)
open(os.path.join(OUT, 'index.html'), 'w').write(f'''<!doctype html><meta charset="utf-8"><title>Scrolless store art</title>
<style>body{{font:14px system-ui;background:#eceae2;margin:0;padding:20px}} nav{{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:14px}}
nav a{{background:#fff;padding:2px 8px;border-radius:9px;color:#163f2a;text-decoration:none}} h2{{margin:18px 0 8px}}
.row{{display:grid;grid-template-columns:repeat(7,1fr);gap:10px}} .row img{{width:100%;border-radius:8px;box-shadow:0 1px 4px #0002}}
.fg{{width:min(100%,640px);margin-top:10px;border-radius:8px;box-shadow:0 1px 4px #0002}}</style>
<h1>Scrolless store art ({len(locales)} locales)</h1><nav>{nav}</nav>{''.join(rows)}''')
print(os.path.join(OUT, 'index.html'))
