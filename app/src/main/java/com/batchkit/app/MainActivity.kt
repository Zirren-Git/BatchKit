package com.batchkit.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batchkit.app.core.model.AppSettings
import com.batchkit.app.core.model.ThemeMode
import com.batchkit.app.ui.BatchKitRoot
import com.batchkit.app.ui.screens.StartupFailureScreen
import com.batchkit.app.ui.theme.BatchKitTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BatchKitApp
        val container = app.containerOrNull
        val startupError = app.containerError
        runCatching { container?.shizukuStatusProvider?.start() }

        setContent {
            if (container == null) {
                // The app always opens, even when a start-up component failed: the
                // failure is shown and can be copied instead of only being visible
                // in a crash dialog.
                BatchKitTheme(themeMode = ThemeMode.SYSTEM) {
                    StartupFailureScreen(
                        error = startupError,
                        onCopy = ::copyToClipboard,
                    )
                }
            } else {
                val settings by container.settingsRepository.settings
                    .collectAsStateWithLifecycle(initialValue = AppSettings())

                BatchKitTheme(themeMode = settings.themeMode) {
                    BatchKitRoot(container = container)
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text))
    }
}
