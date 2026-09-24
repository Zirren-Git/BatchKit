# BatchKit

**BatchKit** is an open-source, production-ready Android application built with Jetpack Compose and Material 3 that allows users to select multiple installed applications and execute privileged batch actions in a single tap. 

BatchKit requires **no root** access and leverages **Shizuku** (ADB privileges) to invoke privileged system APIs.

---

## 🚀 Key Features

### 1. App List & Multi-Selection
- **Full Application Discovery:** Displays all installed applications with real-time metadata (package name, app label, install date, last update time, UID).
- **Instant Search:** Case-insensitive search across application titles and package names.
- **Smart Filtering:** Filter between *All*, *User apps*, *System apps*, *Currently running apps*, and *Frozen / Disabled apps*.
- **Flexible Sorting:** Sort by name (A–Z / Z–A), package name, installation date, or update timestamp.
- **Batch Selection:** Single-tap selection, "Select all filtered", "Invert selection", and selection counter.
- **State Preservation:** Selection and query states survive device configuration changes and process death via `SavedStateHandle`.

### 2. Privileged Batch Actions
Apply actions sequentially across all selected applications with real-time progress indicators:

| Action | Privileged Mechanism (via Shizuku / ADB) |
|---|---|
| **Force stop** | `am force-stop <package>` / `IActivityManager.forceStopPackage` |
| **Block background run** | AppOps `RUN_IN_BACKGROUND` (op 63) → `ignore` |
| **Block ANY background run** | AppOps `RUN_ANY_IN_BACKGROUND` (op 70) → `ignore` |
| **Restore background run** | AppOps `RUN_IN_BACKGROUND` & `RUN_ANY_IN_BACKGROUND` → `allow` |
| **Freeze / Disable** | `pm disable-user --user 0 <package>` (removes from launcher) |
| **Unfreeze / Enable** | `pm enable <package>` (restores app to launcher) |
| **Battery optimization OFF** | `cmd deviceidle whitelist +<package>` (exempt from Doze) |
| **Battery optimization ON** | `cmd deviceidle whitelist -<package>` (remove from whitelist) |
| **Clear cache** | `pm trim-caches 999999999999` |
| **Notifications OFF** | `cmd appops set <package> POST_NOTIFICATION ignore` |
| **Notifications ON** | `cmd appops set <package> POST_NOTIFICATION allow` |

### 3. Safety Rails & System Protection
- **Critical Package Protection:** BatchKit dynamically resolves and strictly blocks destructive operations on:
  - BatchKit itself (`com.batchkit.app`)
  - Shizuku (`moe.shizuku.privileged.api`, `rikka.shizuku`)
  - Current default launcher (`Intent.CATEGORY_HOME`)
  - Core system services (`android`, `com.android.systemui`, Google Play Services)
  - Active keyboard (IME) and active telecom/dialer.
- **Device Administrator Detection:** Detects active Device Admin apps (which cannot be force-stopped or frozen) and marks them with an `ADMIN` badge, refusing destructive actions.
- **Default Safe Mode:** On first launch, "Safe Mode" is active. Dangerous actions like *Freeze* and *Clear cache* are locked until explicitly unlocked in Settings.
- **Destructive Confirmation Dialogs:** Displays confirmation dialogs with app counts before any freeze or cache operation.

### 4. Automation Profiles & Quick Settings Tile
- **Action Profiles:** Save custom combinations of target apps and actions (e.g. *"Night Mode: Freeze social apps"*).
- **Quick Settings Tile (`BatchKitTileService`):** Execute your primary profile or stop all background apps in one tap from the Android notification shade.
- **Scheduled Background Runs:** Schedule profile executions via AndroidX `WorkManager` with system notifications.

### 5. 100% Offline & Private
- **Zero Trackers:** No analytics, no advertising, no crash reporting SDKs.
- **No Internet Permission:** Does not request `android.permission.INTERNET`.

### 6. Localization
- Full strings resources for **English** (`values/strings.xml`) and **Swedish** (`values-sv/strings.xml`).

---

## 🛠️ Architecture & Tech Stack

- **UI:** Jetpack Compose with Material 3 design and dynamic color theming.
- **Privileged Backend:** `dev.rikka.shizuku:api:13.1.5` + `dev.rikka.shizuku:provider:13.1.5`.
- **Hidden API Bypass:** `org.lsposed.hiddenapibypass:4.3` for unrestricted Android 9+ reflection.
- **Database:** AndroidX Room with Kotlin Coroutines Flow for profiles.
- **Settings:** AndroidX DataStore Preferences.
- **Background Tasks:** AndroidX WorkManager.
- **Architecture:** Clean Architecture with MVVM, StateFlow, and Coroutines (`Dispatchers.IO` for privileged calls).

---

## ⚡ Shizuku Setup Guide (Wireless Debugging)

BatchKit interacts with Android's system services via Shizuku. Follow these steps once per reboot:

1. **Enable Developer Options:**
   - Go to **Settings > About Phone**.
   - Tap **Build Number** 7 times until Developer Options are unlocked.
2. **Enable Wireless Debugging (Android 11+):**
   - Connect to a Wi-Fi network.
   - Go to **Settings > System > Developer Options**.
   - Turn on **Wireless Debugging**.
3. **Pair Shizuku:**
   - Open the **Shizuku** app and select **Pairing**.
   - In Developer Options, select **Pair device with pairing code**.
   - Enter the 6-digit code in the Shizuku pairing notification.
4. **Start Shizuku Service:**
   - Return to Shizuku and tap **Start**.
5. **Grant BatchKit Permission:**
   - Open BatchKit. Tap **Request Shizuku Permission** on the Shizuku screen or when prompted.

### OEM Quirks & Troubleshooting

- **Xiaomi / HyperOS / MIUI:**
  - You must enable **USB debugging (Security settings)** in Developer Options (requires SIM card / Xiaomi account login).
- **OPPO / Realme / OnePlus (ColorOS):**
  - Disable **Permission monitoring** in Developer Options to prevent ColorOS from revoking ADB privileges.
- **Device Reboot:**
  - The Android system terminates Shizuku upon device reboot. Reopen Shizuku and tap "Start via Wireless Debugging" (no PC needed).

---

## 🧪 Testing

### Running Unit Tests
```bash
./gradlew test
```
Covers:
- `SafetyManagerTest`: Verifies that self, Shizuku, and core system packages can never be frozen, and validates Safe Mode rules.
- `BatchActionTest`: Verifies action classification and report counts.
- `ProfileSerializationTest`: Verifies profile entity persistence and action conversions.

### Running Instrumented Smoke Tests with Wireless Debugging
1. Connect ADB over Wi-Fi:
   ```bash
   adb connect <device-ip>:<port>
   ```
2. Run the instrumented test:
   ```bash
   ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.batchkit.app.ShizukuSmokeTest
   ```

---

## 📦 Building

To assemble the debug APK:
```bash
./gradlew assembleDebug
```
The resulting APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
MIT License. See [LICENSE](LICENSE) for details.
