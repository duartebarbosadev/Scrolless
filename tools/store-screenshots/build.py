#!/usr/bin/env python3
"""Builds the Play Store screenshots and feature graphic from real app captures.

Text comes from copy.json, app screens from raw/<app locale>/ (see capture.py), and the look from
template/ (page skeleton, CSS, background shapes) and assets/ (logo, icons, feature-graphic art).
Each image is laid out as an HTML page and rendered with headless Chrome, so wrapping,
right-to-left text and every script are handled by a real text engine.

    python3 build.py                 # all locales
    python3 build.py en-US iw-IL     # some locales
Output: out/<store locale>/01-less-scrolling.png ... 07-built-on-trust.png, 08-feature-graphic.png
"""
import html, json, os, re, subprocess, sys
from concurrent.futures import ThreadPoolExecutor

ROOT = os.path.dirname(os.path.abspath(__file__))
CHROME = '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
COPY = json.load(open(os.path.join(ROOT, 'copy.json'), encoding='utf-8'))


def read(*path):
    with open(os.path.join(ROOT, *path), encoding='utf-8') as f:
        return f.read()


PAGE = read('template', 'page.html')
STYLE = read('template', 'style.css')
FIT = read('template', 'fit.js')
BACKGROUNDS = [read('template', 'backgrounds', f'{i}.svg').strip() for i in range(4)]
ICONS = {name: read('assets', 'icons', f'{name}.svg').strip() for name in ('code', 'no-ads', 'on-device', 'check')}

# Store locale -> app locale whose capture is used.
APP_LOCALE = {
    'en-US': 'en-US', 'en-GB': 'en-GB', 'cs-CZ': 'cs', 'de-DE': 'de-DE', 'el-GR': 'el', 'es-ES': 'es-ES',
    'es-419': 'es-419', 'fi-FI': 'fi-FI', 'fr-FR': 'fr-FR', 'hi-IN': 'hi-IN', 'hr': 'hr', 'hu-HU': 'hu',
    'it-IT': 'it-IT', 'ja-JP': 'ja', 'nl-NL': 'nl', 'no-NO': 'nb-NO', 'pl-PL': 'pl', 'pt-BR': 'pt-BR',
    'pt-PT': 'pt-PT', 'ro': 'ro', 'ru-RU': 'ru', 'sk': 'sk-SK', 'sv-SE': 'sv-SE', 'tr-TR': 'tr-TR',
    'uk': 'uk-UA', 'vi': 'vi', 'zh-CN': 'zh-CN', 'zh-TW': 'zh-TW',
    'ar': 'ar', 'da-DK': 'da-DK', 'et': 'et', 'fil': 'fil', 'id': 'id', 'is-IS': 'is', 'iw-IL': 'he',
    'ko-KR': 'ko', 'lt': 'lt', 'sq': 'sq', 'th': 'th',
}
RTL = {'ar', 'iw-IL'}
CJK_FONT = {'ja-JP': 'JP', 'ko-KR': 'KR', 'zh-CN': 'SC', 'zh-TW': 'TC'}
HTML_LANG = {'iw-IL': 'he', 'no-NO': 'nb', 'fil': 'fil'}

SCREENS = ['01-less-scrolling', '02-choose-your-cap', '03-set-your-rhythm', '04-keep-the-chats',
           '05-timeline-averages', '06-usage-history', '07-built-on-trust']
RAW_FOR = {0: '01-home', 1: '02-daily-limit', 2: '03-interval', 3: '04-settings', 4: '05-timeline', 5: '06-history'}
# Background shape per screenshot (template/backgrounds/<n>.svg).
BACKGROUND_FOR = [0, 1, 2, 3, 2, 1, 2]

FONTS = [('Display', 'NotoSansDisplay'), ('Body', 'NotoSans'), ('Hebrew', 'NotoSansHebrew'),
         ('Arabic', 'NotoSansArabic'), ('Devanagari', 'NotoSansDevanagari'), ('Thai', 'NotoSansThai'),
         ('JP', 'NotoSansJP'), ('KR', 'NotoSansKR'), ('SC', 'NotoSansSC'), ('TC', 'NotoSansTC')]


def font_faces():
    return ''.join(
        f"@font-face{{font-family:'{name}';src:url('file://{ROOT}/fonts/{file}.ttf');"
        f"font-weight:100 900;font-stretch:62.5% 100%}}"
        for name, file in FONTS)


def font_stack(locale, base):
    cjk = CJK_FONT.get(locale)
    stack = [base, 'Hebrew', 'Arabic', 'Devanagari', 'Thai'] + ([cjk] if cjk else ['SC'])
    return ','.join(f"'{f}'" for f in stack)


def sentences(text, locale=''):
    if locale in ('ja-JP', 'zh-CN', 'zh-TW'):
        # No spaces to wrap at, so break after each clause and keep clauses whole.
        parts = re.split(r'(?<=[。！？，、])', text)
    else:
        parts = re.split(r'(?<=[。！？])|(?<=[.!?])\s+', text)
    return [p.strip() for p in parts if p and p.strip()]


def esc(text):
    return html.escape(text, quote=True)


def fill(template, **values):
    for key, value in values.items():
        template = template.replace('{{' + key + '}}', str(value))
    return template


def page(locale, body, width, height, fit_js):
    return fill(PAGE,
                lang=HTML_LANG.get(locale, locale.split('-')[0]),
                dir='rtl' if locale in RTL else 'ltr',
                fonts=font_faces(),
                body_font=font_stack(locale, 'Body'),
                display_font=font_stack(locale, 'Display'),
                word_break='keep-all' if locale == 'ko-KR' else 'normal',
                width=width, height=height,
                style=STYLE, body=body, fit=fit_js)


def text_block(locale, headline, subtitle, left, top, width, height, hs, ss, hmin, smin, hshrink, physical=False):
    nowrap = ' style="white-space:nowrap"' if locale in ('ja-JP', 'zh-CN', 'zh-TW') else ''
    lines = ''.join(f'<span{nowrap}>{esc(s)}</span>' for s in sentences(headline, locale))
    side = 'left' if physical else 'inset-inline-start'
    block = (f'<div class="text" style="top:{top}px;{side}:{left}px;width:{width}px;height:{height}px">'
             f'<h1>{lines}</h1><p class="sub">{esc(subtitle)}</p></div>')
    js = fill(FIT, headline_size=hs, subtitle_size=ss, headline_min=hmin, subtitle_min=smin, shrink_subtitle_below=hshrink)
    return block, js


LOGO = f'<div class="logo"><img src="file://{ROOT}/assets/logo.png"><span>Scrolless</span></div>'


def raw_path(locale, index):
    app = APP_LOCALE.get(locale, 'en-US')
    return os.path.join(ROOT, 'raw', app, RAW_FOR[index] + '.png')


def phone_bleeding(src):
    # 708px wide screen inside a 16px frame; the phone runs off the bottom edge.
    return (f'<div class="phone" style="left:170px;top:648px;width:740px;height:1700px">'
            f'<div class="screen" style="width:708px;height:1668px"><img src="file://{src}" style="width:708px;top:0"></div></div>')


def panel_timeline(src):
    # Zoomed crop of the expanded timeline and the weekday averages.
    scale = 930 / 1080
    y0, y1 = 1130, 2330
    h = round((y1 - y0) * scale)
    top = 650
    return (f'<div class="panel" style="left:65px;top:{top}px;width:950px;height:{h + 20}px">'
            f'<img src="file://{src}" style="width:930px;top:{-round(y0 * scale)}px"></div>')


def tiles():
    return ('<div class="tiles" style="top:700px">'
            + ''.join(f'<div class="tile">{ICONS[k]}{ICONS["check"]}</div>' for k in ('code', 'no-ads', 'on-device'))
            + '</div>')


def screenshot_html(locale, index):
    c = COPY[locale]
    headline, subtitle = c[index * 2], c[index * 2 + 1]
    text, js = text_block(locale, headline, subtitle, 72, 196, 936, 412, 118, 42, 60, 32, 96)
    if index == 4:
        art = panel_timeline(raw_path(locale, index))
    elif index == 6:
        art = tiles()
    else:
        art = phone_bleeding(raw_path(locale, index))
    body = BACKGROUNDS[BACKGROUND_FOR[index]] + LOGO + text + art
    return page(locale, body, 1080, 1920, js)


def feature_html(locale):
    c = COPY[locale]
    text, js = text_block(locale, c[14], c[15], 64, 118, 480, 270, 92, 34, 44, 22, 72, physical=True)
    wordmark = '<div class="wordmark"><span>Scrolless</span></div>'
    body = f'<img src="file://{ROOT}/assets/feature-bg.png" style="position:absolute;inset:0;width:1024px;height:500px">' + wordmark + text
    return page(locale, body, 1024, 500, js)


def chrome(args, attempts=3):
    # Headless Chrome occasionally hangs or crashes; retry instead of failing the whole build.
    for attempt in range(attempts):
        try:
            return subprocess.run(args, check=True, capture_output=True, text=True, timeout=60).stdout
        except (subprocess.TimeoutExpired, subprocess.CalledProcessError):
            if attempt == attempts - 1:
                raise


def render(html_text, path, width, height):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    src = path[:-4] + '.html'
    with open(src, 'w', encoding='utf-8') as f:
        f.write(html_text)
    flags = [CHROME, '--headless=new', '--disable-gpu', '--hide-scrollbars', '--force-device-scale-factor=1',
             f'--window-size={width},{height}', '--virtual-time-budget=8000', '--allow-file-access-from-files']
    chrome(flags + [f'--screenshot={path}', f'file://{src}'])
    # template/fit.js sets the title to "overflow" if the text still does not fit at the minimum size.
    dom = chrome(flags + ['--dump-dom', f'file://{src}'])
    if '<title>overflow</title>' in dom:
        print('TEXT OVERFLOW', path, flush=True)
    os.remove(src)
    # Play wants opaque RGB PNGs.
    subprocess.run(['magick', path, '-alpha', 'off', '-strip', f'PNG24:{path}'], check=True)


def build(locale):
    out = os.path.join(ROOT, 'out', locale)
    for i, name in enumerate(SCREENS):
        render(screenshot_html(locale, i), os.path.join(out, name + '.png'), 1080, 1920)
    render(feature_html(locale), os.path.join(out, '08-feature-graphic.png'), 1024, 500)
    return locale


if __name__ == '__main__':
    locales = sys.argv[1:] or sorted(COPY)
    with ThreadPoolExecutor(max_workers=4) as pool:
        for done in pool.map(build, locales):
            print('built', done)
