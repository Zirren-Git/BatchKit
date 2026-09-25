#!/usr/bin/env bash
#
# Installs and launches the built APKs on the emulator the workflow just booted,
# and reports what happened as GitHub annotations. This is the only check that
# answers "does the app actually open on a device", which a build on its own
# cannot tell us: the first release compiled, packaged and passed every other
# check while crashing before Application.onCreate() on every device.
#
# Usage: scripts/emulator_launch_check.sh [dist-directory]
#
set -uo pipefail

DIST="${1:-dist}"
PACKAGE_RELEASE="com.batchkit.app"
PACKAGE_DEBUG="com.batchkit.app.debug"
ACTIVITY_NAME="com.batchkit.app.MainActivity"

status=0
phases_done=""
note() { echo "::notice::$1"; }
fail() { echo "::error::$1"; status=1; }
phase() { echo "::notice::phase: $1"; phases_done="$phases_done $1"; }
dump() { head -n "$1" <<< "$2" | while IFS= read -r line; do [ -n "$line" ] && echo "::notice::    $line"; done; }

sdk="$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r' || true)"
release="$(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r' || true)"
note "device: Android ${release:-?} (API ${sdk:-?})"

launch_and_check() {
  local apk="$1"
  local pkg="$2"
  local label="$3"
  local activity="$pkg/$ACTIVITY_NAME"

  phase "$label"

  if [ ! -f "$apk" ]; then
    fail "$label: $apk is missing"
    return
  fi

  if ! adb install -r -t "$apk" > /tmp/install.txt 2>&1; then
    fail "$label: install failed: $(tail -n 2 /tmp/install.txt | tr '\n' ' ' || true)"
    return
  fi
  note "$label: installed $apk"

  adb logcat -c > /dev/null 2>&1 || true
  local start_output
  start_output="$(adb shell am start -W -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER -n "$activity" 2>&1 | tr -d '\r' || true)"
  dump 8 "$start_output"
  sleep 10

  local pid resumed
  pid="$(adb shell pidof "$pkg" 2>/dev/null | tr -d '\r' | tr '\n' ' ' || true)"
  resumed="$(adb shell dumpsys activity activities 2>/dev/null | grep -m1 'ResumedActivity' | tr -d '\r' | sed 's/^ *//' || true)"

  if [ -n "$pid" ]; then
    note "$label: process is alive (pid ${pid% })"
  else
    fail "$label: the process is gone 10 s after launch"
  fi

  case "$resumed" in
    *"$pkg"*) note "$label: in the foreground (${resumed})" ;;
    *) fail "$label: no resumed activity (dumpsys says: ${resumed:-nothing})" ;;
  esac

  # Why the process died, straight from the platform.
  if [ -z "$pid" ]; then
    dump 30 "$(adb shell dumpsys activity exit-info "$pkg" 2>/dev/null | tr -d '\r' || true)"
  fi

  # The crash buffer survives the process, so this also covers "it closed instantly".
  local crash
  crash="$(adb logcat -d -b crash -v brief 2>/dev/null | grep -iE "FATAL|AndroidRuntime|$pkg" | tail -n 20 || true)"
  if [ -n "$crash" ]; then
    while IFS= read -r line; do fail "$label: $line"; done <<< "$crash"
  else
    note "$label: no crash in the platform crash buffer"
  fi
}

launch_and_check "$DIST/app-release.apk" "$PACKAGE_RELEASE" "release"
launch_and_check "$DIST/app-debug.apk" "$PACKAGE_DEBUG" "debug"

# The app must also survive being opened from a cold state after the first run.
phase "relaunch"
adb shell am force-stop "$PACKAGE_RELEASE" > /dev/null 2>&1 || true
adb shell am start -W -n "$PACKAGE_RELEASE/$ACTIVITY_NAME" > /dev/null 2>&1 || true
sleep 8
if [ -n "$(adb shell pidof "$PACKAGE_RELEASE" 2>/dev/null | tr -d '\r' || true)" ]; then
  note "relaunch after force-stop: still running"
else
  fail "relaunch after force-stop: the process is gone"
fi

# A phase that silently never ran must not look like a pass.
for expected in release debug relaunch; do
  case " $phases_done " in
    *" $expected "*) ;;
    *) fail "the $expected check never ran" ;;
  esac
done

if [ "$status" -eq 0 ]; then
  note "all APKs opened and stayed open on API ${sdk:-?}"
fi
exit "$status"
