package com.kevin.astra.test

import kotlinx.coroutines.delay
import kotlin.time.TimeSource

/**
 * Polls [condition] until it holds, returning as soon as it does and failing the test if
 * [timeoutMillis] elapses first.
 *
 * Async ViewModel tests used to wait for background work with a single fixed `delay(...)` and then
 * assert completion. That is fine on the fast JVM host runner but flakes on the slower
 * Kotlin/Native CI runner, where the work can take longer than the hard-coded delay. Waiting on the
 * actual terminal condition instead removes the race in both directions.
 */
suspend fun awaitCondition(
    timeoutMillis: Long = 5_000,
    pollMillis: Long = 10,
    message: () -> String = { "Condition not met within ${timeoutMillis}ms" },
    condition: () -> Boolean,
) {
    val start = TimeSource.Monotonic.markNow()
    while (!condition()) {
        check(start.elapsedNow().inWholeMilliseconds < timeoutMillis) { message() }
        delay(pollMillis)
    }
}
