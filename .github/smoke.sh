#!/usr/bin/env bash
# Installs the release APK, launches ATA and collects crashes. Runs inside the emulator runner.
set -x
APK=$(ls app/build/outputs/apk/release/*.apk | head -1)
{
  echo "APK: $APK"
  adb install -r "$APK" 2>&1
  adb logcat -c
  adb shell am start -W -n org.tfv.ata/.ui.MainActivity 2>&1
  sleep 15
  echo "== pid: $(adb shell pidof org.tfv.ata)"
  echo "== crash buffer"
  adb logcat -d -b crash 2>&1
  echo "== AndroidRuntime / ATA"
  adb logcat -d 2>&1 | grep -E "AndroidRuntime|FATAL|org.tfv.ata|Process: org.tfv" | head -200
} > smoke.txt 2>&1
exit 0
