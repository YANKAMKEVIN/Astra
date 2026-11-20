package com.kevin.astra

import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.kevin.astra.core.design.astraWindowBackgroundArgb
import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.navigation.AstraNavigator
import com.kevin.astra.core.notification.NotificationKeys
import com.kevin.astra.domain.gmail.AndroidGmailController
import com.kevin.astra.domain.gmail.GmailIntegration
import com.kevin.astra.domain.gmail.GmailSignInBridge
import com.kevin.astra.domain.gmail.androidGmailAuthenticatorOrNull
import com.kevin.astra.domain.gmail.initializeAndroidGmailAuth
import com.kevin.astra.domain.settings.ThemeHolder
import com.kevin.astra.domain.settings.ThemeMode
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val navigator: AstraNavigator by inject()

    private lateinit var gmailSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        applySystemBars(ThemeHolder.mode())
        super.onCreate(savedInstanceState)
        // Follow the theme chosen in Settings (System / Dark / Light): bar icons and window color.
        lifecycleScope.launch {
            ThemeHolder.mode.collect { applySystemBars(it) }
        }
        // Platform services + Koin are wired in AstraApplication.onCreate.
        // Gmail stays here because its sign-in flow is Activity-coupled (ActivityResultLauncher).
        initializeAndroidGmailAuth(this, BuildConfig.GMAIL_ANDROID_CLIENT_ID)
        setupGmailSignIn()

        // Notification permission is asked on the first model download (Models screen), where
        // the progress notification gives it a reason.
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

    private fun applySystemBars(mode: ThemeMode) {
        val systemInDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val light = mode.isLight(systemInDarkTheme)
        val barStyle = if (light) {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        window.setBackgroundDrawable(ColorDrawable(astraWindowBackgroundArgb(light)))
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
