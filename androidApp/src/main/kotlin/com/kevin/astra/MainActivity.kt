package com.kevin.astra

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.kevin.astra.core.ai.initializeAndroidEdgeAiRuntime
import com.kevin.astra.domain.gmail.AndroidGmailController
import com.kevin.astra.domain.gmail.GmailIntegration
import com.kevin.astra.domain.gmail.GmailSignInBridge
import com.kevin.astra.domain.gmail.androidGmailAuthenticatorOrNull
import com.kevin.astra.domain.gmail.initializeAndroidGmailAuth
import kotlinx.coroutines.launch
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
import com.kevin.astra.domain.documents.initializeAndroidEmbeddingEngine
import com.kevin.astra.domain.documents.initializeAndroidPdfExtractor
import com.kevin.astra.domain.vision.initializeAndroidImageClassifier
import com.kevin.astra.domain.voice.initializeAndroidSpeechRecognitionService
import com.kevin.astra.domain.voice.initializeAndroidTextToSpeechService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val navigator: AstraNavigator by inject()

    private lateinit var gmailSignInLauncher: ActivityResultLauncher<Intent>

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
        initializeAndroidEmbeddingEngine(this)
        initializeAndroidHardwareSensorReader(this)
        initializeAndroidGmailAuth(this, BuildConfig.GMAIL_ANDROID_CLIENT_ID)
        setupGmailSignIn()

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

    private fun setupGmailSignIn() {
        gmailSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val data = result.data ?: return@registerForActivityResult
            lifecycleScope.launch {
                runCatching { androidGmailAuthenticatorOrNull()?.handleAuthorizationResponse(data) }
            }
        }
        // Let the shared UI request the interactive consent that only the Activity can launch.
        GmailSignInBridge.onRequestSignIn = {
            androidGmailAuthenticatorOrNull()?.authorizationIntent()?.let(gmailSignInLauncher::launch)
        }
        // Publish the controller so shared code can query connection state and trigger sign-in.
        GmailIntegration.controller = AndroidGmailController(
            clientIdConfigured = BuildConfig.GMAIL_ANDROID_CLIENT_ID.isNotBlank(),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        GmailSignInBridge.onRequestSignIn = null
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
