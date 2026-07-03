package com.kevin.astra

import android.app.Application
import com.kevin.astra.app.di.initializeKoin
import com.kevin.astra.core.ai.initializeAndroidEdgeAiRuntime
import com.kevin.astra.core.notification.initializeNotificationService
import com.kevin.astra.data.history.initializeAndroidConversationFileStore
import com.kevin.astra.data.settings.initializeAndroidAiConfigurationStorage
import com.kevin.astra.domain.benchmark.initializeAndroidHardwareSensorReader
import com.kevin.astra.domain.documents.initializeAndroidEmbeddingEngine
import com.kevin.astra.domain.documents.initializeAndroidPdfExtractor
import com.kevin.astra.domain.export.initializeAndroidConversationShareHelper
import com.kevin.astra.domain.modelmanager.initializeAndroidModelDownloadManager
import com.kevin.astra.domain.modelmanager.initializeAndroidModelReadinessProvider
import com.kevin.astra.domain.vision.initializeAndroidImageClassifier
import com.kevin.astra.domain.voice.initializeAndroidSpeechRecognitionService
import com.kevin.astra.domain.voice.initializeAndroidTextToSpeechService

/**
 * Single, guaranteed-earliest place to wire the Android platform services and start Koin.
 * Previously these ran scattered across MainActivity.onCreate — which is not the earliest entry
 * point (Koin was actually started later, from the App() composable, so `by inject()` in the
 * Activity could resolve before the graph existed). Centralising them in Application.onCreate fixes
 * that init-order fragility.
 */
class AstraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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
        initializeKoin()
    }
}
