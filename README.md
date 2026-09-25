# BatchKit

**Batch privileged actions on many Android apps at once — no root required.**

BatchKit selects several installed apps and applies an action to all of them in one
tap, using [Shizuku](https://shizuku.rikka.app/) to borrow the ADB shell identity.
Freeze a dozen games before a meeting, block background activity for the social apps
that drain the battery, or clear the cache of everything — and see a per-app result
afterwards.

```
Apps ──select──> Action ──confirm──> Results (✅ / ❌ / ⏭ per app)
          │                                    │
          └── save as profile ── QS tile ──────┘
```

## Features

### App list
* Icon, label, package name, user/system flag, "running", "frozen", device-admin and
  protected badges.
* Search by label or package name.
* Sort by name, last used (usage access) or install date, ascending or descending.
* Filters for user apps, system apps, running apps and frozen apps.
* Multi-select with checkboxes, "select all filtered", selection counter.
* The selection is kept in `SavedStateHandle` *and* DataStore, so it survives
  rotation, navigation and process death.

### Batch actions

| Group | Action | Implementation |
| --- | --- | --- |
| Lifecycle | Force stop | `IActivityManager.forceStopPackage` |
| Lifecycle | Freeze | `IPackageManager.setApplicationEnabledSetting(DISABLED_USER)` |
| Lifecycle | Unfreeze | `IPackageManager.setApplicationEnabledSetting(ENABLED)` |
| Background | Block background | AppOps `RUN_IN_BACKGROUND` → ignored |
| Background | Block any background | AppOps `RUN_ANY_IN_BACKGROUND` → ignored |
| Background | Restore background | both app ops → default (falls back to allow) |
| Power | Battery optimisation exempt / not exempt | `IDeviceIdleController`, shell fallback `cmd deviceidle whitelist ±pkg` |
| Storage | Clear cache | `IPackageManager.deleteApplicationCacheFiles`, shell fallback `pm clear --cache-only` |
| Notifications | Notifications off / on | `INotificationManager.setNotificationsEnabledForPackage`, AppOps fallback |

Actions run **sequentially** in a dedicated `:privileged` process, never on the main
thread, and every app produces its own result row with a plain-language reason when
something fails.

### Safety rails
* A protected list is resolved per device: BatchKit itself, Shizuku, every launcher
  (resolved through `CATEGORY_HOME`), every enabled input method and a set of critical
  system packages.
* Force stop, freeze and cache clear are refused for protected packages; app-op
  changes are allowed but the confirmation dialog says so out loud.
* Device administrators are refused for the actions the system would reject anyway.
* The confirmation dialog states the count and lists the apps that will be skipped
  **before** anything runs.
* Safe mode (default) only offers force stop and the background app ops; freeze,
  notifications and cache clear need an explicit unlock in Settings.

### Profiles
* Named selection + action set, stored in Room.
* One-tap apply, pin one profile to the Quick Settings tile.
* Optional daily schedule through WorkManager, with the honest disclaimer that a run
  can only happen while the Shizuku session is alive.

### Shizuku lifecycle
* `Shizuku.pingBinder()` plus binder-received / binder-dead listeners keep the status
  fresh when Shizuku starts, dies or after a reboot.
* A dedicated screen for each phase — not installed, not running, permission missing,
  ready — with actions that fit the phase.
* Onboarding with the wireless-debugging steps and OEM quirks (Realme permission
  monitoring, Xiaomi USB-debugging security setting).
* Always-visible status chip: green when ready, red when not; tapping it opens help.

### Privacy
No analytics, no trackers, no network code, no accounts. Settings live in DataStore,
profiles in Room, both excluded from cloud backup.

## Requirements

* Android 8.0 (API 26) or newer.
* [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api)
  started through wireless debugging (Android 11+) or ADB. Root is not required
  (and not used).
* `QUERY_ALL_PACKAGES` to list installed apps, optional usage access for "last used"
  sorting.

## Build

```bash
./gradlew assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest     # unit tests
./gradlew connectedDebugAndroidTest   # Shizuku smoke test on a device
```

Toolchain: Gradle 9.6, AGP 9.4, Kotlin 2.4, KSP 2.3, `compileSdk`/`targetSdk` 37,
`minSdk` 26, Java 17. The app module does not apply `org.jetbrains.kotlin.android`:
AGP 9 compiles Kotlin itself (built-in Kotlin) and the Kotlin and KSP versions are
raised through the root build file's buildscript classpath.

## Architecture

```
app/src/main/java/com/batchkit/app/
├── core/          models, selection codec, safety policy (Android free, unit tested)
├── privileged/    Shizuku bridge: hidden-API bootstrap, reflective executors,
│                  shell fallback, Messenger service in the :privileged process
├── engine/        ActionDispatcher (sequential, per-app results), run coordinator,
│                  usage stats provider
├── data/          Room (profiles) + DataStore (settings), app list and icon loading
├── shizuku/       binder lifecycle state used by the status chip
├── work/          daily profile schedule
├── tile/          Quick Settings tile
├── ui/            Compose Material 3 screens, light + dark, English + Swedish
└── di/            hand written dependency container
```

Design notes:

* **Privileged work stays out of the UI process.** `PrivilegedActionService` runs in
  `:privileged`; the UI talks to it through a `Messenger` and falls back to executing
  in-process if a ROM never delivers the binder to a secondary process.
* **Everything hidden is reflective.** If a method is missing or renamed, the call
  fails with a precise `FailureReason` instead of crashing.
* **Safety is a pure function.** `SafetyPolicy` has no Android dependencies, so the
  rules that keep a launcher alive are covered by unit tests.

## Translations

The UI ships in English and Swedish (`values-sv`). Both string tables are kept in
sync; missing translations fall back to English.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
