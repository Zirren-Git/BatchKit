# BatchKit

Batch multiple privileged actions on many apps at once, through Shizuku, without root.

## Fixed in this release

* **Actions failed on every app ("0 out of x successful").** Two separate bugs in
  the privileged call path:
  1. The `:privileged` process never received Shizuku's binder.
     `ShizukuProvider.enableMultiProcessSupport(...)` takes `isProviderProcess`, not
     "enable" — calling it with `true` in every process told the privileged process
     it already held a binder, so it reported "Shizuku is not available" for every
     single app. The privileged process now declares itself a non-provider process
     and asks the provider for the binder, as Shizuku documents.
  2. Every privileged call addressed the wrong binder. Shizuku's own binder was
     handed to `IActivityManager.Stub.asInterface()`; the binder that must be wrapped
     is the *system service* binder
     (`ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity"))`).
     Nothing could ever have worked, on any Android version.
* **Actions that used to say nothing now say what happened.** If the privileged
  process has no binder, the call is retried inside the app process instead of
  failing, and the hidden API exemptions are installed in whichever process makes
  the reflective calls.
* **New self-test on the Shizuku screen.** One tap runs the checks in *both*
  processes — binder present, permission, hidden API exemptions, every system
  service lookup, and the shell bridge — and shows the report as selectable text
  with a copy button. This is the report to attach to a bug report: it names the
  failing step directly.

## Previously fixed

* **BatchKit could not start at all.** The Shizuku provider was declared with
  `android:multiprocess="true"`. Shizuku rejects that while the provider is being
  installed — the process died before `Application.onCreate()` ever ran, so the app
  closed immediately, on every Android version, with
  `java.lang.IllegalStateException: android:multiprocess must be false`. The provider
  now runs in the main process only, exactly as Shizuku requires, and every other
  process opts into multi-process binder sharing the documented way.
* **The Shizuku provider is now protected** with
  `android.permission.INTERACT_ACROSS_USERS_FULL`, so only Shizuku (which runs as
  shell and holds that permission) can talk to it.
* **A start-up failure is no longer invisible.** If a component the app needs at
  start-up cannot be built, BatchKit opens and shows the reason, with a button to
  copy it, instead of dying silently.
* **CI now proves the app opens.** A job boots a real system image, installs both
  APKs, launches them and reports the outcome — including any crash trace — before a
  release is published.

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
* `scripts/verify_apks.sh dist/*.apk` — checks the built APKs (contents, manifest,
  provider declaration, permissions, signature).
* The `Launch on a device image` CI job installs and launches both APKs on a real
  system image and fails on a crash; see `docs/TESTING.md`.
* `./gradlew connectedDebugAndroidTest` — instrumented Shizuku smoke test on a
  device (see `docs/TESTING.md` for the wireless debugging run book).
