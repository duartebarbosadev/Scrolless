#!/usr/bin/env python3
"""Copies out/<locale>/ into fastlane/metadata/android/<locale>/images/ with Play's file names."""
import os, shutil
ROOT = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(ROOT, '..', '..'))
OUT = os.path.join(ROOT, 'out')
for loc in sorted(os.listdir(OUT)):
    src = os.path.join(OUT, loc)
    if not os.path.isdir(src):
        continue
    images = os.path.join(REPO, 'fastlane', 'metadata', 'android', loc, 'images')
    shots = os.path.join(images, 'phoneScreenshots')
    os.makedirs(shots, exist_ok=True)
    for f in os.listdir(shots):
        if f.endswith('.png'):
            os.remove(os.path.join(shots, f))
    for f in sorted(os.listdir(src)):
        if not f.endswith('.png'):
            continue
        if f.startswith('08-'):
            dst = os.path.join(images, 'featureGraphic.png')
            if os.path.islink(dst):
                os.remove(dst)
            shutil.copyfile(os.path.join(src, f), dst)
        else:
            shutil.copyfile(os.path.join(src, f), os.path.join(shots, f))
    print('installed', loc)
