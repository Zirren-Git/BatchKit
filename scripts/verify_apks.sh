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
fail() { echo "::error::$1"; status=1; }
note() { echo "::notice::$1"; }

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
  note "$apk (${size_mib} MiB)"

  # 1. An APK is a zip that must contain the manifest, the resource table and code.
  listing="$(unzip -l "$apk" 2>&1)"
  for entry in AndroidManifest.xml resources.arsc classes.dex; do
    if grep -q " $entry\$" <<< "$listing"; then
      note "  ok: $entry"
    else
      fail "$apk does not contain $entry"
    fi
  done
  if grep -qE " META-INF/.*\.(RSA|DSA|EC)\$" <<< "$listing"; then
    note "  ok: signature block present"
  else
    fail "$apk has no META-INF signature block"
  fi

  # 2. The manifest parses and describes the app we think we built.
  if [ -x "$AAPT2" ]; then
    badging="$("$AAPT2" dump badging "$apk" 2>&1)"
    while IFS= read -r line; do note "  $line"; done < <(
      grep -E "^(package|sdkVersion|targetSdkVersion|application-label:|launchable-activity)" <<< "$badging"
    )
    grep -q "package: name='com.batchkit.app" <<< "$badging" ||
      fail "$apk: unexpected package name"
    grep -q "launchable-activity" <<< "$badging" ||
      fail "$apk: no launchable activity"
    grep -q "^sdkVersion:'26'" <<< "$badging" ||
      fail "$apk: minSdkVersion is not 26"
    for perm in \
      android.permission.QUERY_ALL_PACKAGES \
      android.permission.PACKAGE_USAGE_STATS \
      moe.shizuku.manager.permission.API_V23; do
      grep -q "$perm" <<< "$badging" || fail "$apk: missing $perm"
    done
    note "  ok: permissions and launch activity present"
  else
    fail "aapt2 not found under $SDK_ROOT, APK contents were not verified"
  fi

  # 3. The file is signed with a key the platform will accept.
  if [ -x "$APKSIGNER" ]; then
    if "$APKSIGNER" verify --min-sdk-version 26 "$apk" > /tmp/apksigner.txt 2>&1; then
      note "  ok: apksigner verify passed"
      while IFS= read -r line; do note "  $line"; done < <(
        grep -E "^Signer #1 certificate (DN|SHA-256)" /tmp/apksigner.txt
      )
    else
      fail "$apk: apksigner verify failed: $(tail -n 2 /tmp/apksigner.txt | tr '\n' ' ')"
    fi
  else
    fail "apksigner not found under $SDK_ROOT, the signature was not verified"
  fi
done

if [ "$status" -eq 0 ]; then
  note "all ${#apks[@]} APK(s) verified: contents, manifest and signature"
fi
exit "$status"
