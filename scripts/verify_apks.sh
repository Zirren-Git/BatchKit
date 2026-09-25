#!/usr/bin/env bash
#
# Verifies the APKs that were just built (or the ones downloaded for a release)
# so that the artifacts attached to a GitHub release are known to be well formed,
# signed and installable. Every finding is printed as a GitHub annotation, which
# stays readable even when the raw job log is not reachable.
#
# Usage:
#   scripts/verify_apks.sh              # verifies app/build/outputs/apk/**/*.apk
#   scripts/verify_apks.sh dist/*.apk   # verifies an explicit list of APKs
#
set -uo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-/usr/local/lib/android/sdk}}"
BUILD_TOOLS="$(ls -d "$SDK_ROOT"/build-tools/* 2>/dev/null | sort -V | tail -n 1 || true)"
AAPT2="${BUILD_TOOLS:+$BUILD_TOOLS/aapt2}"
APKSIGNER="${BUILD_TOOLS:+$BUILD_TOOLS/apksigner}"

status=0
note() { echo "::notice::$1"; }
fail() { echo "::error::$1"; status=1; }
# Prints a command's output as indented notices, trimmed so the annotation list
# stays short enough to be readable.
dump() { head -n 20 <<< "$1" | while IFS= read -r line; do [ -n "$line" ] && echo "::notice::    $line"; done; }

if [ "$#" -gt 0 ]; then
  apks=("$@")
else
  shopt -s nullglob globstar
  apks=(app/build/outputs/apk/**/*.apk)
fi

if [ "${#apks[@]}" -eq 0 ]; then
  fail "no APK found to verify"
  exit 1
fi

for apk in "${apks[@]}"; do
  if [ ! -f "$apk" ]; then
    fail "$apk does not exist"
    continue
  fi
  size_mib=$(( $(stat -c%s "$apk") / 1024 / 1024 ))
  apk_notes=("${size_mib} MiB")

  # 1. An APK is a zip that must contain the manifest, the resource table and code.
  listing="$(unzip -l "$apk" 2>&1)"
  for entry in AndroidManifest.xml resources.arsc classes.dex; do
    if grep -q " $entry\$" <<< "$listing"; then
      apk_notes+=("$entry ok")
    else
      fail "$apk does not contain $entry"
    fi
  done

  # 2. The manifest parses and describes the app we think we built.
  if [ ! -x "$AAPT2" ]; then
    fail "$apk: aapt2 not found under $SDK_ROOT, contents were not inspected"
  else
    badging="$("$AAPT2" dump badging "$apk" 2>&1)"
    if ! grep -qiE "^min?sdkversion:'26'" <<< "$badging"; then
      fail "$apk: minSdkVersion is not 26"
      dump "$(grep -iE 'sdkversion' <<< "$badging")"
    fi
    grep -qi "package: name='com.batchkit.app" <<< "$badging" ||
      fail "$apk: unexpected package name"
    grep -qi "launchable-activity" <<< "$badging" ||
      fail "$apk: no launchable activity"
    for perm in \
      android.permission.QUERY_ALL_PACKAGES \
      android.permission.PACKAGE_USAGE_STATS \
      moe.shizuku.manager.permission.API_V23; do
      grep -q "$perm" <<< "$badging" || fail "$apk: missing $perm"
    done
    apk_notes+=("$(grep -m1 '^package:' <<< "$badging")")
    apk_notes+=("targetSdk $(grep -oE "targetSdkVersion:'[0-9]+'" <<< "$badging" | head -n 1)")
    apk_notes+=("$(grep -m1 'launchable-activity' <<< "$badging" | cut -c1-70)")
    # The Shizuku provider has to run in the main process only: with
    # android:multiprocess="true" Shizuku throws from onCreate while the provider is
    # installed, which kills the process before any app code runs. That shipped once;
    # this check makes a build-time regression impossible to publish.
    xmltree="$("$AAPT2" dump xmltree --file AndroidManifest.xml "$apk" 2>&1)"
    if ! grep -q "rikka.shizuku.ShizukuProvider" <<< "$xmltree"; then
      fail "$apk: the Shizuku provider is missing from the manifest"
    fi
    if grep -qE 'multiprocess[^:]*: [^ ]*0x1' <<< "$xmltree"; then
      fail "$apk: ShizukuProvider is declared with android:multiprocess=\"true\", which crashes the app during start-up"
    fi
    if ! grep -q "INTERACT_ACROSS_USERS_FULL" <<< "$xmltree"; then
      fail "$apk: the Shizuku provider is not protected by android.permission.INTERACT_ACROSS_USERS_FULL"
    fi
    apk_notes+=("manifest, permissions and launch activity ok")
  fi

  # 3. The file is signed with a key the platform will accept. minSdk 26 apps use
  #    APK Signature Scheme v2/v3, so no META-INF/*.RSA entry is expected here.
  if [ ! -x "$APKSIGNER" ]; then
    fail "$apk: apksigner not found under $SDK_ROOT, the signature was not verified"
  elif "$APKSIGNER" verify --min-sdk-version 26 "$apk" > /tmp/apksigner.txt 2>&1; then
    schemess="$(grep -oE 'Verified using v[0-9.]+ scheme[^:]*: true' /tmp/apksigner.txt | sed -e 's/ scheme.*v/ scheme v/' -e 's/.*scheme v/  v/' -e 's/: true//' | tr -d '\n')"
    cert="$(grep -m1 -E '^Signer #1 certificate SHA-256' /tmp/apksigner.txt | cut -c1-60)"
    apk_notes+=("signature ok:${schemess:- v2/v3}")
    [ -n "$cert" ] && apk_notes+=("$cert")
  else
    fail "$apk: apksigner verify failed"
    dump "$(tail -n 8 /tmp/apksigner.txt)"
  fi

  note "$apk: ${apk_notes[*]}"
done

if [ "$status" -eq 0 ]; then
  note "all ${#apks[@]} APK(s) verified: contents, manifest, permissions and signature"
fi
exit "$status"
