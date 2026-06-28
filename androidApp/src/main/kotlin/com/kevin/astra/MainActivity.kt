package com.kevin.astra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kevin.astra.core.ai.initializeAndroidEdgeAiRuntime
import com.kevin.astra.data.settings.initializeAndroidAiConfigurationStorage
import com.kevin.astra.domain.modelmanager.initializeAndroidModelReadinessProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initializeAndroidAiConfigurationStorage(this)
        initializeAndroidEdgeAiRuntime(this)
        initializeAndroidModelReadinessProvider(this)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
