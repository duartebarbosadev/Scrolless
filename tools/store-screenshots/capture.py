#!/usr/bin/env python3
"""Captures real app screenshots for the store art from a running emulator.

Needs the playDebug APK installed and one emulator connected. Usage:
    python3 capture.py setup            # once per emulator boot
    python3 capture.py en-US de ...     # app locales (BCP 47) to capture
    python3 capture.py history en-US    # only the history screen
Screens land in raw/<locale>/<screen>.png (device resolution).
"""
import os, subprocess, sys, time

PKG = 'com.scrolless.app.debug'
ACTIVITY = f'{PKG}/com.scrolless.app.ui.MainActivity'
SERVICE = f'{PKG}/com.scrolless.app.accessibility.ScrollessBlockAccessibilityService'
HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, 'raw')

# Tap targets on a 1080x2400 screen (Medium Phone). They do not depend on the language.
DAILY_LIMIT_TAB = (540, 1062)
TIME_INTERVAL_CARD = (884, 867)
SETTINGS_BUTTON = (971, 107)
EXPAND_TIMELINE = (933, 1207)
EXPAND_PAST_DAY = (940, 756)
EDIT_SCHEDULE = (540, 1392)
PREVIOUS_DAY = (98, 107)
KEYPAD = {'1': (354, 978), '0': (540, 1497)}


def _emulator_serial():
    # Only ever drive an emulator, even if a real phone is plugged in.
    if os.environ.get('ANDROID_SERIAL'):
        return os.environ['ANDROID_SERIAL']
    lines = subprocess.run(['adb', 'devices'], check=True, capture_output=True, text=True).stdout.splitlines()
    emulators = [l.split()[0] for l in lines[1:] if l.startswith('emulator-') and l.split()[-1] == 'device']
    if len(emulators) != 1:
        sys.exit(f'Expected exactly one running emulator, found {emulators}')
    return emulators[0]


os.environ['ANDROID_SERIAL'] = _emulator_serial()


def adb(*args):
    return subprocess.run(['adb', 'shell', *args], check=True, capture_output=True, text=True).stdout


RTL_LOCALES = {'ar', 'he', 'iw'}
mirror = False  # set per locale; right-to-left layouts flip horizontally


def tap(xy, wait=1.2):
    x = 1080 - xy[0] if mirror else xy[0]
    adb('input', 'tap', str(x), str(xy[1]))
    time.sleep(wait)


def back(wait=1.0):
    adb('input', 'keyevent', 'KEYCODE_BACK')
    time.sleep(wait)


def demo(command, **extras):
    args = ['am', 'broadcast', '-a', 'com.android.systemui.demo', '-e', 'command', command]
    for k, v in extras.items():
        args += ['-e', k, v]
    adb(*args)


def enable_accessibility():
    adb('settings', 'put', 'secure', 'enabled_accessibility_services', SERVICE)
    adb('settings', 'put', 'secure', 'accessibility_enabled', '1')


def open_app():
    enable_accessibility()  # Android can drop it after a reinstall or data clear
    # Clear-task restart without force-stop: force-stop makes Android drop the accessibility service.
    adb('am', 'start', '-n', ACTIVITY, '-f', '0x10008000')
    time.sleep(3)


def seed():
    adb('am', 'broadcast', '-n', f'{PKG}/com.scrolless.app.debug.ScreenshotSeedReceiver')
    time.sleep(2)


def setup():
    adb('pm', 'clear', PKG)
    adb('am', 'start', '-n', ACTIVITY)
    time.sleep(4)
    enable_accessibility()
    adb('settings', 'put', 'global', 'sysui_demo_allowed', '1')
    demo('enter')
    demo('clock', hhmm='1019')
    demo('battery', level='100', plugged='false')
    demo('network', wifi='show', level='4', fully='true')
    demo('network', mobile='hide')
    demo('notifications', visible='false')


def capture(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    for _ in range(5):
        png = subprocess.run(['adb', 'exec-out', 'screencap', '-p'], check=True, capture_output=True).stdout
        # A nearly blank frame means the activity is still being recreated after a locale change.
        if len(png) > 150_000:
            break
        time.sleep(2)
    with open(path, 'wb') as f:
        f.write(png)


def prepare(locale):
    global mirror
    mirror = locale.split('-')[0] in RTL_LOCALES
    adb('cmd', 'locale', 'set-app-locales', PKG, '--locales', locale)
    seed()
    open_app()


def capture_history_screen(out):
    time.sleep(2)  # taps during the opening animation get lost
    for _ in range(3):
        tap(PREVIOUS_DAY, 1.5)
    tap(EXPAND_PAST_DAY, 2.5)
    capture(f'{out}/06-history.png')


def capture_locale(locale):
    prepare(locale)
    out = os.path.join(RAW, locale)
    time.sleep(3)  # let the usage ring finish animating
    capture(f'{out}/01-home.png')

    tap(DAILY_LIMIT_TAB)
    tap(KEYPAD['1'], 0.4); tap(KEYPAD['0'], 0.4); tap(KEYPAD['0'], 0.8)
    capture(f'{out}/02-daily-limit.png')
    back()

    tap(TIME_INTERVAL_CARD, 1.5)
    tap(EDIT_SCHEDULE, 1.5)
    capture(f'{out}/03-interval.png')
    back()
    seed(); open_app()

    tap(SETTINGS_BUTTON, 1.8)
    capture(f'{out}/04-settings.png')
    back(1.5)

    tap(EXPAND_TIMELINE, 1.5)
    capture(f'{out}/05-timeline.png')
    open_app()

    capture_history_screen(out)


def capture_history(locale):
    prepare(locale)
    capture_history_screen(os.path.join(RAW, locale))


if __name__ == '__main__':
    if sys.argv[1:] == ['setup']:
        setup()
    elif sys.argv[1:2] == ['history']:
        for loc in sys.argv[2:]:
            capture_history(loc)
            print('captured history', loc, flush=True)
    else:
        for loc in sys.argv[1:]:
            capture_locale(loc)
            print('captured', loc, flush=True)
