# Testing BatchKit

## Unit tests (no device needed)

```bash
./gradlew testDebugUnitTest
```

They cover the parts where a bug is expensive: the safety policy (protected
packages, device admins, "select all" can never freeze a launcher), the selection
codec (malformed package names never reach a shell command), the action registry
(stable ids) and the batch dispatcher (ordering, per-app failures, progress,
stop-the-batch behaviour).

## Launch check on a real system image (CI)

The `Launch on a device image` job in `.github/workflows/build.yml` boots an Android
system image, installs both APKs, opens them from a cold start and fails if either
process is gone ten seconds later. It reads the platform crash buffer and
`dumpsys activity exit-info`, and reports what it finds as check annotations, so a
crash trace is readable without downloading anything.

This job exists because a build-only pipeline cannot prove the app opens. The first
release shipped a Shizuku provider declared with `android:multiprocess="true"`: it
compiled, packaged, passed the unit tests and the APK inspection, and then crashed
on every device before `Application.onCreate()` ran, because Shizuku rejects that
attribute while the provider is installed. Nothing short of launching the APK on a
device catches that class of bug, so a release is now published only after this job
passes.

The job runs `scripts/emulator_launch_check.sh`, which expects a directory holding
`app-release.apk` and `app-debug.apk` (the layout of the `batchkit-apks` artifact).
It can be pointed at a locally connected device as well.

## Instrumented Shizuku smoke test

`app/src/androidTest/java/com/batchkit/app/ShizukuSmokeTest.kt` talks to a real
Shizuku session. Every test is skipped when Shizuku is not running or has not
granted BatchKit its permission, so running it on a plain device is harmless.

What it covers:

| Test | What it proves |
| --- | --- |
| `shizukuBinderIsReachable` | the binder is alive and the permission is granted |
| `shellBridgeRunsAsShellUser` | the reflective `Shizuku.newProcess` bridge works (`id -u` returns `2000`) |
| `appOpRoundTripIsReversible` | blocking and restoring `RUN_IN_BACKGROUND` both succeed |
| `runningPackagesAreVisible` | the "running" filter can actually see other processes |
| `protectedPackagesAreDetected` | BatchKit, Shizuku and the launcher end up on the protected list |

### Run book: wireless debugging on a non-rooted phone

1. **Phone**: Settings → About phone → tap *Build number* seven times to unlock
   developer options.
2. **Phone**: Settings → System → Developer options → enable *Wireless debugging*
   (keep *USB debugging* enabled as well, it makes re-pairing easier).
3. **Phone**: connect to the same Wi-Fi network as your computer.
4. **Computer**: enable adb over Wi-Fi and pair:
   ```bash
   adb pair <ip>:<pairing-port>      # the code is shown on the phone
   adb connect <ip>:<debug-port>     # shown on the Wireless debugging screen
   adb devices                       # the phone must be listed as "device"
   ```
5. **Phone**: install Shizuku from the Play Store and open it.
6. **Phone**: in Shizuku, choose *Start via Wireless debugging* and follow the
   pairing steps Shizuku shows (it starts its own service, no root needed).
7. **Phone**: build and install BatchKit:
   ```bash
   ./gradlew installDebug
   ```
8. **Phone**: open BatchKit, tap the red status chip, grant BatchKit the Shizuku
   permission when asked.
9. **Computer**: run the smoke test:
   ```bash
   ./gradlew connectedDebugAndroidTest
   ```

Notes:

* Shizuku stops when the phone reboots. Restart it from the Shizuku app (Android 11+
  can do it fully from the phone through wireless debugging).
* On Realme/Oppo/OnePlus phones, disable *Permission monitoring* in developer
  options, otherwise the ADB session gets killed a few seconds after it starts.
* On Xiaomi/HyperOS, also enable *USB debugging (Security settings)*, otherwise
  Force stop and AppOps calls are refused.
* `connectedDebugAndroidTest` needs the device to be visible to `adb`; with
  wireless debugging only, make sure step 4 succeeded before running it.
