package com.kevin.astra.presentation.models

import kotlin.math.roundToInt

/** How a model download is moving, as shown under its progress bar. */
sealed interface DownloadPace {
    /** Not enough samples yet to give a rate. */
    data object Measuring : DownloadPace

    data class Active(val mbPerSecond: Float, val secondsLeft: Long?) : DownloadPace

    /** No new data for a while (typically the network dropped). */
    data object Stalled : DownloadPace
}

/**
 * Estimates download speed and time left from the progress updates of a download.
 * The rate is averaged over the last [windowMillis] so it does not jump with each update;
 * no progress for [stallAfterMillis] reads as [DownloadPace.Stalled].
 */
class DownloadRateEstimator(
    private val windowMillis: Long = 8_000,
    private val stallAfterMillis: Long = 5_000,
    private val minimumSpanMillis: Long = 1_000,
) {
    private val samples = ArrayDeque<Pair<Long, Float>>()
    private var lastProgressAtMillis: Long? = null

    fun record(nowMillis: Long, downloadedMb: Float) {
        val last = samples.lastOrNull()
        if (last == null || downloadedMb > last.second) lastProgressAtMillis = nowMillis
        if (last != null && downloadedMb < last.second) samples.clear() // restarted
        samples.addLast(nowMillis to downloadedMb)
        while (samples.size > 2 && nowMillis - samples.first().first > windowMillis) samples.removeFirst()
    }

    fun pace(nowMillis: Long, totalMb: Float): DownloadPace {
        val progressAt = lastProgressAtMillis ?: return DownloadPace.Measuring
        if (nowMillis - progressAt >= stallAfterMillis) return DownloadPace.Stalled
        val first = samples.firstOrNull() ?: return DownloadPace.Measuring
        val last = samples.last()
        val spanMillis = last.first - first.first
        if (spanMillis < minimumSpanMillis) return DownloadPace.Measuring
        val mbPerSecond = (last.second - first.second) / (spanMillis / 1_000f)
        if (mbPerSecond <= 0f) return DownloadPace.Measuring
        val secondsLeft = if (totalMb > 0f) ((totalMb - last.second).coerceAtLeast(0f) / mbPerSecond).roundToInt().toLong() else null
        return DownloadPace.Active(mbPerSecond, secondsLeft)
    }
}

/** "12 MB/s · about 3 min left", "Paused — waiting for network", or null while measuring. */
fun formatDownloadPace(pace: DownloadPace): String? = when (pace) {
    DownloadPace.Measuring -> null
    DownloadPace.Stalled -> "Paused — waiting for network"
    is DownloadPace.Active -> {
        val rate = formatRate(pace.mbPerSecond)
        pace.secondsLeft?.let { "$rate · ${formatTimeLeft(it)}" } ?: rate
    }
}

internal fun formatRate(mbPerSecond: Float): String = when {
    mbPerSecond >= 10f -> "${mbPerSecond.roundToInt()} MB/s"
    mbPerSecond >= 1f -> "${(mbPerSecond * 10).roundToInt() / 10.0} MB/s"
    else -> "${(mbPerSecond * 1_000).roundToInt()} KB/s"
}

internal fun formatTimeLeft(seconds: Long): String = when {
    seconds < 60 -> "less than a minute left"
    seconds < 3_600 -> "about ${(seconds + 30) / 60} min left"
    else -> {
        val minutes = (seconds + 30) / 60
        val h = minutes / 60
        val m = minutes % 60
        if (m == 0L) "about $h h left" else "about $h h $m min left"
    }
}
