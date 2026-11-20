package com.kevin.astra.presentation.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kevin.astra.domain.modelmanager.ModelDownloadState
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

/**
 * Speed and time left of [downloading] ("12 MB/s · about 3 min left"), refreshed every second so
 * a download that stops receiving data turns into "Paused — waiting for network".
 * Null while there is no download or not enough samples yet.
 */
@Composable
fun rememberDownloadPaceText(downloading: ModelDownloadState.Downloading?): String? {
    val clock = remember { TimeSource.Monotonic.markNow() }
    val estimator = remember(downloading?.modelId) { DownloadRateEstimator() }
    var nowMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(downloading?.modelId, downloading?.downloadedMb) {
        if (downloading == null) return@LaunchedEffect
        estimator.record(clock.elapsedNow().inWholeMilliseconds, downloading.downloadedMb)
        while (true) {
            nowMillis = clock.elapsedNow().inWholeMilliseconds
            delay(1_000)
        }
    }
    return downloading?.let { formatDownloadPace(estimator.pace(nowMillis, it.totalMb)) }
}
