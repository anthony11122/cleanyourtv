#!/usr/bin/env python3
"""Hisense / VIDAA preflight and audit.

Answers the questions that decide whether a Hisense set is safe to clean, in one connection:
what the set is, whether ADB is open to the network, which packages must stay enabled, whether
Home still resolves to a launcher, and whether a debloat list has drifted back on after a reboot.

    TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/hisense.py
    TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/hisense.py examples/hisense-vidaa-mt9653-cn/debloat.txt

Exit status: 0 when every hard check passes, 1 when something needs attention (a protected
package disabled, no Home, a listed package enabled again), 2 when the TV could not be read.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tv  # noqa: E402  (exits with a TV_HOST hint when the variable is missing)

# Packages the Hisense/VIDAA docs mark as must-stay: the TV's input path, remote, IME, AI picture,
# Dolby and MQTT base services. Disabling one is a known way to lose HDMI/AV, the remote or boot.
PROTECTED = {
    "com.jamdeo.tv.livetv": "Live TV, HDMI/AV playback",
    "com.jamdeo.tv.service": "TVServices, the TIL/JNI half of input switching",
    "com.hisense.tv.inputs": "SysUIMerge, source panel and task centre",
    "com.hisense.tv.baseservice": "TV base service",
    "com.hisense.tv.tiletransition": "TileAnimationService; the source panel binds it before switching",
    "com.hisense.bluetoothpair": "remote pairing",
    "com.sohu.inputmethod.sogou.tv": "the only IME on the set",
    "com.hisense.padinputmethod": "backup IME",
    "com.hisense.raycodereceiver": "remote channel (resident)",
    "com.hisense.uifac": "factory UI component",
    "com.hisense.systeminfo": "system info provider",
    "com.hisense.hiservice": "MQTT base service",
    "com.hisense.aiod": "AI picture (AIPQ) and gestures",
    "com.hisense.aiservice": "AI picture (AIPQ) and gestures",
    "com.keylab.speech.core.vidaa": "voice core",
    "com.vt.mic.service": "microphone",
    "com.vt.point.remoteservice": "air mouse",
    "com.dolby.android.audio.service": "Dolby audio",
    "com.dolby.android.audio.calibration": "Dolby audio calibration",
}

# The OTA trio: disabling them stops firmware and security updates for good on this firmware.
OTA = ("com.jamdeo.updateservice", "com.hisense.usbupgrade", "com.hisense.playerupdate")

HOME_QUERY = ("cmd package resolve-activity --brief -a android.intent.action.MAIN "
              "-c android.intent.category.HOME")
PKG = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$")


def package_set(out):
    """Package names from `pm list packages` output (`package:name`)."""
    names = set()
    for line in out.splitlines():
        line = line.strip()
        if line.startswith("package:"):
            names.add(line[len("package:"):].strip())
    return names


def read_list(path):
    """Package names from a debloat file, the same way disable.sh reads it."""
    names = []
    with open(path, encoding="utf-8-sig") as f:
        for line in f:
            line = line.split("#", 1)[0].strip()
            if not line:
                continue
            name = line.split()[0]
            if PKG.match(name):
                names.append(name)
    return names


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("-")]
    if len(args) > 1:
        sys.exit("usage: hisense.py [debloat list]")
    listed = read_list(args[0]) if args else []

    dev = tv.connect()
    try:
        def shell(cmd):
            return dev.shell(cmd, read_timeout_s=120.0).replace("\r", "")

        props = {}
        for p in ("ro.product.manufacturer", "ro.product.model", "ro.product.device",
                  "ro.board.platform", "ro.build.version.release", "ro.build.version.sdk",
                  "ro.build.display.id", "ro.adb.secure", "service.adb.tcp.port"):
            props[p] = shell(f"getprop {p}").strip()

        installed = package_set(shell("pm list packages"))
        disabled = package_set(shell("pm list packages -d"))
        enabled = installed - disabled
        home = shell(HOME_QUERY).strip().splitlines()
        home = home[-1].strip() if home else ""
        front = ""
        for line in shell("dumpsys window").splitlines():
            if "mCurrentFocus" in line:
                front = line.strip()
                break
        screensaver = shell("settings get secure screensaver_enabled").strip()
        ad_block = shell("settings get global com.hisense.service.ad_block").strip()
    finally:
        dev.close()

    problems, notes = [], []
    vendor = props["ro.product.manufacturer"]
    model = props["ro.product.model"]
    if "hisense" not in vendor.lower() and "vidaa" not in (vendor + model).lower():
        notes.append(f"not a Hisense-branded set ({vendor} {model}); the package lists still apply only "
                     "if it is the same board")

    print("== set ==")
    for p in ("ro.product.manufacturer", "ro.product.model", "ro.board.platform",
              "ro.build.version.release", "ro.build.version.sdk", "ro.build.display.id"):
        print(f"  {p[11:]:<18} {props[p] or '?'}")

    print("== ADB ==")
    if props["ro.adb.secure"] == "0":
        print("  ro.adb.secure=0: port 5555 accepts any device on this network, no prompt.")
        print("  Turn USB/network debugging off and reboot when the work is done.")
        notes.append("network ADB is unauthenticated")
    else:
        print(f"  ro.adb.secure={props['ro.adb.secure'] or '?'} (authenticated)")

    print("== Home ==")
    if home:
        print(f"  resolves to {home}")
        pkg = home.split("/", 1)[0]
        if pkg in disabled:
            problems.append(f"Home resolves to a disabled package: {home}")
    else:
        problems.append("no activity answers the HOME category")
        print("  no activity answers HOME")
    if front:
        print(f"  {front}")

    print("== protected packages ==")
    for pkg, why in PROTECTED.items():
        if pkg not in installed:
            print(f"  absent   {pkg}")
        elif pkg in disabled:
            print(f"  DISABLED {pkg}  ({why})")
            problems.append(f"{pkg} is disabled: {why}")
    missing = [p for p in PROTECTED if p not in installed]
    if missing:
        print(f"  {len(missing)} not installed on this set (other firmware or board)")

    print("== OTA ==")
    off = [p for p in OTA if p in disabled]
    print(f"  disabled: {', '.join(off) if off else 'none'}")
    if len(off) == len(OTA):
        print("  all three are off: this set will not receive firmware or security updates.")
    elif off:
        print("  partially off: check docs/hisense/屏蔽更新.md before relying on updates.")

    print("== ads and screensaver ==")
    print(f"  screensaver_enabled={screensaver or 'null'} (0 is off)")
    print(f"  com.hisense.service.ad_block={ad_block or 'null'} (updateservice rewrites it at boot)")
    if screensaver == "1":
        notes.append("the screensaver is on; on VIDAA it is an ad slot")

    if listed:
        print(f"== list: {os.path.basename(args[0])} ({len(listed)} packages) ==")
        absent = [p for p in listed if p not in installed]
        drift = [p for p in listed if p in enabled]
        print(f"  disabled: {len(listed) - len(absent) - len(drift)}")
        print(f"  enabled again (drift): {len(drift)}")
        for p in drift:
            print(f"    {p}")
        if absent:
            print(f"  not installed: {len(absent)}")
            for p in absent:
                print(f"    {p}")
        if drift:
            problems.append(f"{len(drift)} listed packages are enabled again")

    print("== verdict ==")
    for p in problems:
        print(f"  FAIL {p}")
    for n in notes:
        print(f"  NOTE {n}")
    if not problems:
        print("  OK")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
