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
import com.kevin.astra.core.notification.NotificationKeys
import com.kevin.astra.core.notification.initializeNotificationService
import com.kevin.astra.data.history.initializeAndroidConversationFileStore
import com.kevin.astra.domain.export.initializeAndroidConversationShareHelper
import com.kevin.astra.data.settings.initializeAndroidAiConfigurationStorage
import com.kevin.astra.domain.modelmanager.initializeAndroidModelDownloadManager
import com.kevin.astra.domain.modelmanager.initializeAndroidModelReadinessProvider
import com.kevin.astra.domain.benchmark.initializeAndroidHardwareSensorReader
import com.kevin.astra.domain.documents.initializeAndroidPdfExtractor
import com.kevin.astra.domain.vision.initializeAndroidImageClassifier
import com.kevin.astra.domain.voice.initializeAndroidSpeechRecognitionService
import com.kevin.astra.domain.voice.initializeAndroidTextToSpeechService
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
        initializeAndroidModelDownloadManager(this)
        initializeAndroidConversationFileStore(this)
        initializeAndroidConversationShareHelper(this)
        initializeAndroidSpeechRecognitionService(this)
        initializeAndroidTextToSpeechService(this)
        initializeAndroidImageClassifier(this)
        initializeAndroidPdfExtractor(this)
        initializeAndroidHardwareSensorReader(this)

        requestNotificationPermission()
        requestMicrophonePermission()

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
        val targetId = intent?.getStringExtra(NotificationKeys.TARGET_DESTINATION)
        val destination = AstraDestination.fromId(targetId)
        
        if (destination != null) {
            navigator.navigateTo(destination)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestMicrophonePermission() {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
            .launch(android.Manifest.permission.RECORD_AUDIO)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
