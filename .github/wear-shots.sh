#!/usr/bin/env bash
# Screenshots of the watch app (tile grid with 4 and 7/9 tiles, list page) with a fake config and status.
# Usage: wear-shots.sh <debug apk> <outdir>
APK="$1"; OUT="$2"; mkdir -p "$OUT"
adb install -r "$APK" >/dev/null
shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }

prefs() { # $1 = grid (4 or 7)
  python3 - "$1" > /tmp/ata_wear_store.xml <<'PY'
import json, sys, html
acts = [("lock","Lock"),("unlock","Unlock"),("climate_on","Climate on"),("flash_lights","Flash lights"),("frunk","Frunk"),
        ("trunk","Trunk"),("honk","Honk"),("vent_windows","Vent windows"),("sentry_on","Sentry on"),("defrost","Defrost"),("wake","Wake up")]
config = {"actions":[{"id":i,"label":l} for i,l in acts],"grid":int(sys.argv[1]),"complications":[]}
status = {"vehicle":"Neon","battery":76,"range":382,"charging":False,"locked":True,"inside":21.4,"outside":9.6,"climate":False,"updatedAt":1700000000000}
print("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>")
print('<string name="config">%s</string>' % html.escape(json.dumps(config)))
print('<string name="status">%s</string>' % html.escape(json.dumps(status)))
print("</map>")
PY
  adb shell am force-stop org.tfv.ata
  adb push /tmp/ata_wear_store.xml /data/local/tmp/ata_wear_store.xml >/dev/null
  adb shell chmod 644 /data/local/tmp/ata_wear_store.xml
  adb shell run-as org.tfv.ata sh -c "'mkdir -p shared_prefs && cp /data/local/tmp/ata_wear_store.xml shared_prefs/ata_wear_store.xml'"
}
start() { adb shell am start -S -W -n org.tfv.ata/org.tfv.ata.wear.WearMainActivity >/dev/null; sleep 12; }
deny() { # dismiss a permission dialog if it shows up
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  grep -q "permission" /tmp/ui.xml && adb shell input keyevent 4 && sleep 1
}

W=$(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1); WX=${W%x*}; WY=${W#*x}

# First start compiles the app and shows "Starting…" for a while.
adb shell am start -W -n org.tfv.ata/org.tfv.ata.wear.WearMainActivity >/dev/null; sleep 40; deny; deny
prefs 4; start; deny; shot "wear-grid-4"
adb shell input swipe $((WX * 8 / 10)) $((WY / 2)) $((WX / 10)) $((WY / 2)) 300; sleep 2; shot "wear-list"
prefs 7; start; deny; shot "wear-grid-7"
adb shell input tap $((WX / 2)) $((WY / 2)); sleep 1; shot "wear-grid-7-tap"
ls -la "$OUT"
