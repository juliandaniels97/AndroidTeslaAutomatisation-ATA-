#!/usr/bin/env bash
# Smoke test inside the emulator: install the release APK ($1), start each given activity and fail if
# the app process crashes. Writes a report to smoke-<name>.txt.
APK="$1"; NAME="$2"; shift 2
OUT="smoke-${NAME}.txt"
FAILED=0
{
  echo "APK: $APK"
  adb install -r "$APK" 2>&1
  for START in "$@"; do
    adb logcat -c
    echo "== am start $START"
    adb shell am start -W $START 2>&1 | grep -E "Status|Error|Activity"
    sleep 8
    CRASH=$(adb logcat -d -b crash 2>&1 | grep -v "beginning of crash")
    if [ -n "$CRASH" ]; then
      echo "!! CRASH"; echo "$CRASH"; FAILED=1
    else
      echo "ok (pid $(adb shell pidof org.tfv.ata))"
    fi
    adb shell am force-stop org.tfv.ata
  done
  echo "RESULT: $([ $FAILED = 0 ] && echo PASS || echo FAIL)"
} > "$OUT" 2>&1
cat "$OUT"
exit $FAILED
