# Play Store screenshots

Builds the seven 1080×1920 phone screenshots and the 1024×500 feature graphic for every Play Store locale (39), from real app screens and typeset text. The published images live in `fastlane/metadata/android/<locale>/images/`; everything here is how they are made.

## What's here

| Path | What it is |
|---|---|
| `copy.json` | Headline and subtitle for each image, per store locale (16 strings: screens 01–07, then the feature graphic). |
| `template/page.html`, `template/style.css` | Page skeleton and all styling (colours, type, phone frame, cards). |
| `template/backgrounds/*.svg` | The four background wave shapes. |
| `template/fit.js` | Shrinks the headline and subtitle until they fit their box. |
| `assets/` | Logo, feature-graphic illustration and the trust-screen icons. |
| `capture.py` | Drives an emulator and saves real app screens to `raw/<app locale>/`. |
| `build.py` | Combines template, copy and captures into `out/<store locale>/`, rendered with headless Chrome. |
| `gallery.py` | Writes `out/index.html` to review every locale at once. |
| `check_glyphs.py` | Confirms every character in `copy.json` exists in the fonts. |
| `install.py` | Copies `out/` into `fastlane/metadata/android/<locale>/images/`. |

To change the look, edit `template/` and `assets/` (open the SVGs or a built page in a browser to preview), then rebuild. To change wording, edit `copy.json`. Rebuilding with unchanged inputs produces byte-identical images, so git only records images that actually changed.

## Regenerate

Needs Google Chrome, ImageMagick (`magick`), Python 3 with `fonttools`, `numpy` and `Pillow`, and the Android SDK.

1. **Fonts (once):** download the Noto variable fonts into `fonts/`: NotoSansDisplay, NotoSans, NotoSansHebrew, NotoSansArabic, NotoSansDevanagari, NotoSansThai, NotoSansJP, NotoSansKR, NotoSansSC, NotoSansTC, from `https://raw.githubusercontent.com/google/fonts/main/ofl/<family>/` (file names like `NotoSans%5Bwdth%2Cwght%5D.ttf`), saved as `<Family>.ttf`.
2. **App screens** (only when the app UI or its translations change): build and install the debug app (`./gradlew :app:assemblePlayDebug`, then `adb install -r app/build/outputs/apk/play/debug/app-play-debug.apk`), start the `Medium_Phone_API_36.1` emulator (1080×2400), then:
   ```
   python3 capture.py setup
   python3 capture.py en-US en-GB cs de-DE el es-ES es-419 fi-FI fr-FR hi-IN hr hu it-IT ja nl nb-NO pl pt-BR pt-PT ro ru sk-SK sv-SE tr-TR uk-UA vi zh-CN zh-TW ar da-DK et fil id is he ko lt sq th
   ```
   Debug builds load fixed sample data through `ScreenshotSeedReceiver`, which also hides the debug panel until the app restarts; the status bar uses Android demo mode (10:19, full battery). `capture.py` only ever drives an emulator, even with a phone plugged in.
3. **Images:** `python3 build.py` (all locales, a few minutes), `python3 check_glyphs.py`, and `python3 gallery.py`. `build.py` prints `TEXT OVERFLOW` for any text that still does not fit.
4. **Publish:** `python3 install.py`, review, then commit the changed images under `fastlane/`.

Notes: tap positions in `capture.py` assume the 1080×2400 Medium Phone emulator and are mirrored for right-to-left languages. Sample data is relative to the capture day and the history screen steps back three days; capture all locales in one session (the current set shows Wednesday 7 October) so every locale shows the same day.
