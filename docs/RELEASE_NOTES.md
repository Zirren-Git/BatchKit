# BatchKit

Batch multiple privileged actions on many apps at once, through Shizuku, without root.

## Highlights

* **App list** with search, sort (name / last used / install date) and filters
  (user, system, running, frozen) over every installed package.
* **Multi-select** with checkboxes, "select all filtered", a live selection counter
  and a selection that survives rotation and process death.
* **Eleven batch actions** applied sequentially with a per-app result:
  force stop, block background, block any background, restore background,
  freeze, unfreeze, battery optimisation whitelist on/off, clear cache,
  notifications on/off.
* **Results screen** with success/failure/skipped per app, the failure reason in
  plain language and a one-tap retry for the apps that failed.
* **Safety rails**: BatchKit, Shizuku, launchers, input methods and critical system
  packages can never be frozen, force stopped or disabled — not even through
  "select all". Device admins are refused for the actions the system would reject.
  Safe mode (on by default) only offers force stop and the background app ops.
* **Profiles**: save a selection plus its actions, apply it with one tap, pin it to
  the Quick Settings tile, optionally run it on a daily schedule.
* **Shizuku lifecycle**: live status chip, dedicated setup screen for each state
  (not installed / not running / permission missing / ready) and OEM troubleshooting.
* **No network, no analytics, no accounts.** Everything runs on the device.

## Install

1. Install [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api)
   and start it (wireless debugging, no root required).
2. Install the APK from this release.
3. Open BatchKit, tap the red Shizuku chip and grant the permission.

Requirements: Android 8.0 (API 26) or newer, Shizuku 13 or newer.

## Build from source

```bash
git clone https://github.com/Zirren-Git/BatchKit.git
cd BatchKit
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # unit tests for dispatch, safety and codecs
```

The APK is signed with the standard Android debug key: it is meant for side-loading,
not for the Play Store.

## Verification

* `./gradlew assembleDebug` — builds the debug APK.
* `./gradlew testDebugUnitTest` — unit tests for the dispatcher, the safety policy,
  the selection codec and the action registry.
* `./gradlew connectedDebugAndroidTest` — instrumented Shizuku smoke test on a
  device (see `docs/TESTING.md` for the wireless debugging run book).
