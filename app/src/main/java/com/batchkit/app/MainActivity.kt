package com.batchkit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.batchkit.app.core.model.AppSettings
import com.batchkit.app.ui.BatchKitRoot
import com.batchkit.app.ui.theme.BatchKitTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as BatchKitApp).container
        container.shizukuStatusProvider.start()

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            BatchKitTheme(themeMode = settings.themeMode) {
                BatchKitRoot(container = container)
            }
        }
    }
}
