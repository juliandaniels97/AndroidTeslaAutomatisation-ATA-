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
  if [ -n "$xy" ]; then adb shell input tap $xy; sleep 2; else echo "not found: $1"; return 1; fi
}
tapany() { # like tap, scrolling down until the text shows up (at most 5 times)
  for i in 1 2 3 4 5; do tap "$1" && return 0; scroll; done
}
tapswitch() { # tap the first switch (checkable node) on screen
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  local xy
  xy=$(python3 - <<'PY'
import re, xml.etree.ElementTree as ET
for n in ET.parse('/tmp/ui.xml').iter('node'):
    if n.get('checkable') == 'true' and n.get('package') == 'org.tfv.ata':
        x1, y1, x2, y2 = map(int, re.findall(r'\d+', n.get('bounds')))
        print((x1 + x2) // 2, (y1 + y2) // 2); break
PY
)
  if [ -n "$xy" ]; then adb shell input tap $xy; sleep 2; else echo "no switch"; fi
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

# Vehicle types: add a car, preview every body style in the editor, then the 3D and 2D views.
restart; tap "Settings"; tap "Vehicles"; tap "Add vehicle"
tap "Name"; adb shell input text "Neon"
tap "VIN (17 characters)"; adb shell input text "7G2CEHED1RA123456"
adb shell input keyevent 4; sleep 1
shot "body-0-auto"
for M in "Model 3" "Model Y" "Model S" "Model X" "Cybertruck" "Roadster"; do
  tap "$M"; shot "body-$(echo "$M" | tr -cd 'a-zA-Z0-9' | tr A-Z a-z)"
done
tap "Automatic (VIN: Cybertruck)"; tap "Save"
restart; tap "Vehicle"; sleep 2; shot "body-3d-tab"
adb shell am start -S -W -n org.tfv.ata/.ui.MainActivity -a android.intent.action.VIEW -d ata://car3d >/dev/null; sleep 6; shot "body-3d-full"
demo() { tap "Show animations"; sleep 0.6; for n in 01 02 03 04 05 06 07 08 09 10 11 12 13 14; do shot "$1-$n"; sleep 1.9; done; }
demo "demo-3d"
restart; tap "Settings"; tap "Design"; tap "3D vehicle view"; shot "body-settings-2d"
restart; tap "Vehicle"; sleep 2; shot "body-2d-tab"
adb shell am start -S -W -n org.tfv.ata/.ui.MainActivity -a android.intent.action.VIEW -d ata://car3d >/dev/null; sleep 6; shot "body-2d-full"
demo "demo-2d"
restart; tap "Settings"; tap "Design"; tap "3D vehicle view"

# From here on a Model Y (the Cybertruck VIN stays, the type is set by hand).
restart; tap "Settings"; tap "Vehicles"; tap "Edit vehicle"; tap "Model Y"; tap "Save"

# README: vehicle view with the BLE action tiles (BLE features on).
restart; tap "Vehicle"; tapswitch
for b in "Allow" "While using the app" "Allow" "While using the app"; do tap "$b"; done
restart; tap "Vehicle"; sleep 3; shot "readme-vehicle"

# Demo mode for the rest: a connected car at 50 %, charging at 22 kW to 80 %, 1 h left, 20 kWh added.
restart; tap "Settings"; tapany "Demo mode"
restart; tap "Vehicle"; sleep 3; shot "demo-vehicle"

for THEME in Dark Light; do
  t=$(echo $THEME | tr A-Z a-z)
  restart; tap "Settings"; tap "Design"; tap "$THEME"
  restart; tap "Favorites"; sleep 2; shot "$t-1-home"
  tap "Vehicle"; sleep 2; shot "$t-2-vehicle"; scroll; shot "$t-2b-vehicle"; scroll; shot "$t-2c-vehicle"; scroll; shot "$t-2d-vehicle"
  restart; tap "Settings"; tap "Design"; shot "$t-2e-design"
  tap "Settings"; shot "$t-3-settings"; scroll; shot "$t-4-settings-2"; scroll; shot "$t-5-settings-3"
  restart; tap "Settings"; tapany "Behavior, status, Bluetooth & NFC"; shot "$t-3b-behavior"; scroll; shot "$t-3c-behavior-2"
  i=6
  for ITEM in "NFC tags" "Automation (Routines, Tasker)" "Menu & widgets" "Wear OS watch" "Vehicles"; do
    restart; tap "Settings"; tap "$ITEM"; shot "$t-$i-$(echo "$ITEM" | tr -cd 'a-zA-Z' | tr A-Z a-z)"; i=$((i+1))
  done
  restart; tap "Settings"; tap "Guide & setup"; shot "$t-10-guide"
  scroll; tap "Bluetooth control (BETA)"; scroll; shot "$t-11-guide-ble"
  tap "Open pairing assistant"; shot "$t-12-pair"; scroll; shot "$t-13-pair-2"; scroll; shot "$t-14-pair-3"
  restart; tap "Settings"; tap "Wear OS watch"; tap "Start"; sleep 5; shot "$t-15-wear-pair"; scroll; shot "$t-16-wear-pair-2"
  restart; tap "Settings"; tapany "Support ATA"; sleep 2; shot "$t-23-donate"
  restart; tap "Settings"; tapany "Send bug report"; sleep 6; shot "$t-17-report"; scroll; shot "$t-18-report-2"
  restart; tap "Settings"; tapany "Behavior, status, Bluetooth & NFC"; tapany "Set up NFC key card"; sleep 3; shot "$t-19-keycard"; scroll; shot "$t-20-keycard-2"
  adb shell am start -S -W -n org.tfv.ata/.ui.MainActivity -a android.intent.action.VIEW -d ata://car3d >/dev/null; sleep 6; shot "$t-21-car3d"
  adb shell input swipe 300 1200 900 1200 600; sleep 2; shot "$t-22-car3d-turned"
  tap "Show animations"; sleep 0.5; for n in 1 2 3 4 5 6 7; do sleep 3.2; shot "$t-demo-$n"; done
  adb shell am start -S -W -n org.tfv.ata/.shortcut.CreateShortcutActivity >/dev/null; sleep 3; shot "$t-9-create-shortcut"
done
# Material design in dark and light (Model Y in its default paint).
for THEME in Dark Light; do
  t=$(echo $THEME | tr A-Z a-z)
  restart; tap "Settings"; tap "Design"; tap "Material"; tap "$THEME"
  if [ "$t" = dark ]; then
    restart; tap "Settings"; tap "Vehicles"; tap "Edit vehicle"; shot "m-$t-4-editor"; tap "Cancel"
  else
    restart; tap "Settings"; tap "Vehicles"; tap "Edit vehicle"; shot "m-$t-4-editor"; tap "Cancel"
  fi
  restart; tap "Favorites"; sleep 2; shot "m-$t-1-home"
  tap "Vehicle"; sleep 2; shot "m-$t-2-vehicle"; scroll; shot "m-$t-2b-vehicle"; scroll; shot "m-$t-2c-vehicle"
  restart; tap "Settings"; shot "m-$t-5-settings"; scroll; shot "m-$t-6-settings-2"
  adb shell am start -S -W -n org.tfv.ata/.ui.MainActivity -a android.intent.action.VIEW -d ata://car3d >/dev/null; sleep 6; shot "m-$t-3-car3d"
  tap "Show animations"; sleep 0.5
  for n in 1 2 3 4 5 6 7; do sleep 3.2; shot "m-$t-demo-$n"; done
done
# Tablet (landscape): the car on the left, the tiles on the right.
restart; tap "Settings"; tapany "Design"; tapany "Neon (standard)"; tapany "Dark"
adb shell wm size 2560x1600; adb shell wm density 320; sleep 3
restart; tap "Vehicle"; sleep 3; shot "tablet-neon-dark"
restart; tap "Settings"; tapany "Design"; tapany "Material"; tapany "Light"
restart; tap "Vehicle"; sleep 3; shot "tablet-material-light"
# Google Play tablet screenshots (16:9): 10" (2560x1440) and 7" (1920x1080), neon dark and material light.
for T in "10:2560x1440:320" "7:1920x1080:280"; do
  IFS=: read -r INCH SIZE DPI <<< "$T"
  # Switch the design at phone size (the settings list needs scrolling, which only works there).
  adb shell wm size reset; adb shell wm density reset; sleep 3
  restart; tap "Settings"; tapany "Design"; tapany "Neon (standard)"; tapany "Dark"
  adb shell wm size "$SIZE"; adb shell wm density "$DPI"; sleep 3
  restart; tap "Vehicle"; sleep 3; shot "play-tablet$INCH-1-vehicle"
  tap "Favorites"; sleep 2; shot "play-tablet$INCH-2-favorites"
  adb shell am start -S -W -n org.tfv.ata/.ui.MainActivity -a android.intent.action.VIEW -d ata://car3d >/dev/null; sleep 6; shot "play-tablet$INCH-3-car3d"
  adb shell wm size reset; adb shell wm density reset; sleep 3
  restart; tap "Settings"; tapany "Design"; tapany "Material"; tapany "Light"
  adb shell wm size "$SIZE"; adb shell wm density "$DPI"; sleep 3
  restart; tap "Vehicle"; sleep 3; shot "play-tablet$INCH-4-material"
done
adb shell wm size reset; adb shell wm density reset

# Google Play phone screenshots in German (app language de-DE, demo mode, neon).
adb shell cmd locale set-app-locales org.tfv.ata --user current --locales de-DE; sleep 2
for THEME in Dunkel Hell; do
  t=$([ "$THEME" = Dunkel ] && echo dark || echo light)
  restart; tap "Einstellungen"; tapany "Design"; tapany "Neon (Standard)"; tapany "$THEME"
  restart; tap "Favoriten"; sleep 2; shot "de-$t-1-home"
  tap "Fahrzeug"; sleep 2; shot "de-$t-2-vehicle"; scroll; shot "de-$t-2b-vehicle"; scroll; shot "de-$t-2c-vehicle"; scroll; shot "de-$t-2d-vehicle"
  adb shell am start -S -W -n org.tfv.ata/.ui.MainActivity -a android.intent.action.VIEW -d ata://car3d >/dev/null; sleep 6; shot "de-$t-21-car3d"
  tap "Animationen zeigen"; sleep 0.5; for n in 1 2 3; do sleep 3.2; shot "de-$t-demo-$n"; done
  restart; tap "Einstellungen"; tap "Automation (Routinen, Tasker)"; shot "de-$t-7-automationroutinestasker"
done
restart; tap "Einstellungen"; tapany "Design"; tapany "Material"; tapany "Dunkel"
restart; tap "Fahrzeug"; sleep 2; shot "de-m-dark-2-vehicle"
adb shell cmd locale set-app-locales org.tfv.ata --user current --locales en-US
ls -la "$OUT"
