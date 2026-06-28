package com.kevin.astra

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kevin.astra.core.ai.initializeAndroidEdgeAiRuntime
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.navigation.AstraNavigator
import com.kevin.astra.core.notification.initializeNotificationService
import com.kevin.astra.data.settings.initializeAndroidAiConfigurationStorage
import com.kevin.astra.domain.modelmanager.initializeAndroidModelReadinessProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val navigator: AstraNavigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initializeAndroidAiConfigurationStorage(this)
        initializeNotificationService(this)
        initializeAndroidEdgeAiRuntime(this)
        initializeAndroidModelReadinessProvider(this)

        requestNotificationPermission()

        handleIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val target = intent?.getStringExtra("EXTRA_TARGET_DESTINATION")
        when (target) {
            "assistant" -> navigator.navigateTo(AstraDestination.Assistant)
            "documents" -> navigator.navigateTo(AstraDestination.Documents)
            "benchmark" -> navigator.navigateTo(AstraDestination.Benchmark)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                // Permission granted or denied, we can log it if needed
            }
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
