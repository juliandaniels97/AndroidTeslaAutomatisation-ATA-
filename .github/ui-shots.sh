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

restart
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
  adb shell am start -S -W -n org.tfv.ata/.shortcut.CreateShortcutActivity >/dev/null; sleep 3; shot "$t-9-create-shortcut"
done
ls -la "$OUT"
