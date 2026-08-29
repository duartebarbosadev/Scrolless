#!/usr/bin/env python3
"""
android_current_view_report.py

Print a report for the Android app currently in the foreground.

Nothing is saved:
- No XML file is left on the Android device.
- No report/XML file is created on the computer.
- Everything is read through ADB stdout and printed to your terminal.

Requirements:
    - Python 3.10+
    - adb in PATH
    - Android device/emulator connected with USB debugging enabled

Usage:
    python3 scripts/android_current_view_report.py
"""

from __future__ import annotations

import re
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET


def run(cmd: list[str], check: bool = True) -> str:
    try:
        result = subprocess.run(
            cmd,
            check=check,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
    except FileNotFoundError:
        print(f"Error: command not found: {cmd[0]}", file=sys.stderr)
        sys.exit(1)
    except subprocess.CalledProcessError as exc:
        print(f"Command failed: {' '.join(cmd)}", file=sys.stderr)
        if exc.stdout:
            print(exc.stdout, file=sys.stderr)
        if exc.stderr:
            print(exc.stderr, file=sys.stderr)
        sys.exit(exc.returncode)

    return result.stdout


def check_adb() -> None:
    if not shutil.which("adb"):
        print("Error: adb is not installed or not in PATH.", file=sys.stderr)
        sys.exit(1)

    state = run(["adb", "get-state"], check=False).strip()
    if state != "device":
        print(
            "Error: no usable ADB device found. "
            "Connect a device and enable USB debugging.",
            file=sys.stderr,
        )
        sys.exit(1)


def get_focus() -> tuple[str | None, str | None]:
    raw = run(["adb", "shell", "dumpsys", "window"])

    patterns = [
        r"mCurrentFocus=.*?\s(?:u\d+\s+)?([A-Za-z0-9._]+)/(?:([A-Za-z0-9.$_]+))",
        r"mFocusedApp=.*?\s(?:u\d+\s+)?([A-Za-z0-9._]+)/(?:([A-Za-z0-9.$_]+))",
    ]

    for pattern in patterns:
        match = re.search(pattern, raw)
        if match:
            return match.group(1), match.group(2)

    return None, None


def dump_ui_to_stdout() -> str:
    """
    Ask uiautomator to dump directly to /dev/tty.

    Because adb exec-out carries that terminal output back to this process,
    no temporary XML file is created on either the phone or computer.
    """
    raw = run(
        [
            "adb",
            "exec-out",
            "uiautomator",
            "dump",
            "--compressed",
            "/dev/tty",
        ]
    )

    # Some Android versions print a status line before/after the XML.
    start = raw.find("<?xml")
    if start == -1:
        start = raw.find("<hierarchy")

    end_marker = "</hierarchy>"
    end = raw.rfind(end_marker)

    if start == -1 or end == -1:
        print(
            "Error: could not find a UI hierarchy in uiautomator output.",
            file=sys.stderr,
        )
        if raw.strip():
            print("\nRaw output:\n", file=sys.stderr)
            print(raw, file=sys.stderr)
        sys.exit(1)

    end += len(end_marker)
    return raw[start:end]


def bool_attr(node: ET.Element, key: str) -> bool:
    return node.attrib.get(key, "false").lower() == "true"


def parse_bounds(value: str) -> tuple[int, int, int, int] | None:
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", value or "")
    if not match:
        return None
    return tuple(map(int, match.groups()))  # type: ignore[return-value]


def area(bounds: tuple[int, int, int, int] | None) -> int:
    if not bounds:
        return 0
    x1, y1, x2, y2 = bounds
    return max(0, x2 - x1) * max(0, y2 - y1)


def short_id(resource_id: str) -> str:
    if ":id/" in resource_id:
        return resource_id.split(":id/", 1)[1]
    return resource_id


def clean(value: str) -> str:
    return value.replace("\n", " ").replace("\r", " ").strip()


def score_node(
    node: ET.Element,
    app_package: str | None,
    screen_area: int,
) -> int:
    rid = node.attrib.get("resource-id", "")
    cls = node.attrib.get("class", "")
    bounds = parse_bounds(node.attrib.get("bounds", ""))

    score = 0

    if rid:
        score += 20

    if app_package and rid.startswith(app_package + ":id/"):
        score += 20

    if bool_attr(node, "scrollable"):
        score += 30

    if bool_attr(node, "selected"):
        score += 5

    interesting_classes = (
        "ViewPager",
        "RecyclerView",
        "ScrollView",
        "SurfaceView",
        "TextureView",
        "VideoView",
        "ViewGroup",
    )
    if any(name in cls for name in interesting_classes):
        score += 20

    if screen_area > 0:
        fraction = area(bounds) / screen_area
        if fraction >= 0.80:
            score += 25
        elif fraction >= 0.50:
            score += 15
        elif fraction >= 0.20:
            score += 5

    if rid == "android:id/content":
        score -= 40

    return score


def print_candidate_table(
    nodes: list[ET.Element],
    package: str | None,
    screen_area: int,
) -> None:
    candidates = [n for n in nodes if n.attrib.get("resource-id", "")]
    candidates.sort(
        key=lambda n: (
            score_node(n, package, screen_area),
            area(parse_bounds(n.attrib.get("bounds", ""))),
        ),
        reverse=True,
    )

    seen: set[str] = set()
    shown = 0

    for node in candidates:
        rid = node.attrib.get("resource-id", "")
        if not rid or rid in seen:
            continue

        seen.add(rid)
        shown += 1

        print(f"[{shown:02d}]")
        print(f"     view id : {short_id(rid)}")
        print(f"     full id : {rid}")
        print(f"     class   : {node.attrib.get('class', '')}")
        print(f"     bounds  : {node.attrib.get('bounds', '')}")

        if bool_attr(node, "scrollable"):
            print("     scroll  : true")
        if bool_attr(node, "clickable"):
            print("     click   : true")
        if bool_attr(node, "selected"):
            print("     selected: true")

        text = clean(node.attrib.get("text", ""))
        desc = clean(node.attrib.get("content-desc", ""))

        if text:
            print(f"     text    : {text}")
        if desc:
            print(f"     desc    : {desc}")

        print()

        if shown >= 30:
            break


def main() -> None:
    print("Checking ADB connection...", flush=True)
    check_adb()

    print("Finding the foreground app...", flush=True)
    package, activity = get_focus()

    print("Reading the current UI hierarchy...", flush=True)
    xml_text = dump_ui_to_stdout()

    print("Preparing the report...", flush=True)
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as exc:
        print(f"Error parsing UI hierarchy: {exc}", file=sys.stderr)
        sys.exit(1)

    all_nodes = list(root.iter("node"))

    bounds = [parse_bounds(node.attrib.get("bounds", "")) for node in all_nodes]
    largest_bounds = max(
        (b for b in bounds if b is not None),
        key=area,
        default=None,
    )
    screen_area = area(largest_bounds)

    report_nodes = all_nodes
    if package:
        report_nodes = [
            node
            for node in all_nodes
            if (
                not node.attrib.get("resource-id", "")
                or node.attrib.get("resource-id", "").startswith(package + ":")
                or node.attrib.get("resource-id", "").startswith("android:")
            )
        ]

    resource_ids = {
        node.attrib.get("resource-id", "")
        for node in all_nodes
        if node.attrib.get("resource-id", "")
    }

    print()
    print("=== ANDROID CURRENT VIEW REPORT ===")
    print(f"Foreground package : {package or 'Unknown'}")
    print(f"Foreground activity: {activity or 'Unknown'}")
    print(f"Accessibility nodes: {len(all_nodes)}")
    print(f"Unique resource IDs: {len(resource_ids)}")
    if largest_bounds:
        print(
            "Screen/root bounds  : "
            f"[{largest_bounds[0]},{largest_bounds[1]}]"
            f"[{largest_bounds[2]},{largest_bounds[3]}]"
        )
    print()

    print_candidate_table(report_nodes, package, screen_area)

    print(
        "Before sharing, double-check this report for identifying text "
        "such as names, usernames, or messages."
    )


if __name__ == "__main__":
    main()
