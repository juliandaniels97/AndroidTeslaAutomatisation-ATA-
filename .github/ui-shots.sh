#!/usr/bin/env bash
# Takes screenshots of the main screens in the app's Dark and Light theme (emulator, English locale).
# Usage: ui-shots.sh <apk> <outdir>
APK="$1"; OUT="$2"; mkdir -p "$OUT"
adb install -r "$APK" >/dev/null

tap() { # tap the first node whose text or content-desc equals $1
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - "$1" <<'PY'
import re, sys, xml.etree.ElementTree as ET
want = sys.argv[1]
for n in ET.parse('/tmp/ui.xml').iter('node'):
    if n.get('text') == want or n.get('content-desc') == want:
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; sleep 2; else echo "not found: $1"; fi
}
shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
scroll() { adb shell input swipe 540 1800 540 700 400; sleep 1; }
restart() { adb shell am start -S -W -n org.tfv.ata/.ui.MainActivity >/dev/null; sleep 6; }

# Start animation: needs real animation speed (the emulator runs with animations off).
adb shell settings put global animator_duration_scale 1
adb shell am start -S -n org.tfv.ata/.ui.MainActivity >/dev/null
for n in 01 02 03 04 05 06 07 08 09 10 11 12 13 14; do shot "00-splash-$n"; done
adb shell settings put global animator_duration_scale 0
sleep 4
shot 00-onboarding
tap "Let's go"

for THEME in Dark Light; do
  t=$(echo $THEME | tr A-Z a-z)
  restart; tap "Settings"; tap "$THEME"
  restart; shot "$t-1-home"
  tap "Vehicle"; shot "$t-2-vehicle"; scroll; shot "$t-2b-vehicle"
  tap "Settings"; shot "$t-3-settings"; scroll; shot "$t-4-settings-2"; scroll; shot "$t-5-settings-3"
  i=6
  for ITEM in "NFC tags" "Automation (Routines, Tasker)" "Menu & widgets" "Wear OS watch" "Vehicles"; do
    restart; tap "Settings"; tap "$ITEM"; shot "$t-$i-$(echo "$ITEM" | tr -cd 'a-zA-Z' | tr A-Z a-z)"; i=$((i+1))
  done
  restart; tap "Settings"; tap "Guide & setup"; shot "$t-10-guide"
  scroll; tap "Bluetooth control (BETA)"; scroll; shot "$t-11-guide-ble"
  tap "Open pairing assistant"; shot "$t-12-pair"; scroll; shot "$t-13-pair-2"; scroll; shot "$t-14-pair-3"
  restart; tap "Settings"; tap "Wear OS watch"; tap "Start"; sleep 5; shot "$t-15-wear-pair"; scroll; shot "$t-16-wear-pair-2"
  restart; tap "Settings"; scroll; scroll; scroll; scroll; tap "Send bug report"; sleep 6; shot "$t-17-report"; scroll; shot "$t-18-report-2"
  restart; tap "Settings"; scroll; scroll; scroll; tap "Set up NFC key card"; sleep 3; shot "$t-19-keycard"; scroll; shot "$t-20-keycard-2"
  adb shell am start -S -W -n org.tfv.ata/.shortcut.CreateShortcutActivity >/dev/null; sleep 3; shot "$t-9-create-shortcut"
done
ls -la "$OUT"
