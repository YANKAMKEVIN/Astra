package com.kevin.astra.presentation.history

import com.kevin.astra.domain.history.ChatConversation
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Section of the History list a conversation falls in, by local calendar day. */
enum class HistoryBucket(val label: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    ThisWeek("This week"),
    Earlier("Earlier"),
}

data class HistorySection(val bucket: HistoryBucket, val conversations: List<ChatConversation>)

/** Epoch millis of a stored `createdAt` (ISO-8601 instant, or epoch millis); null if unreadable. */
@OptIn(ExperimentalTime::class)
fun parseCreatedAtMillis(createdAt: String): Long? {
    if (createdAt.isBlank()) return null
    createdAt.toLongOrNull()?.let { return it }
    return runCatching { Instant.parse(createdAt).toEpochMilliseconds() }.getOrNull()
}

/** Local day number (days since 1970-01-01) of [epochMillis] with the given UTC offset. */
internal fun localEpochDay(epochMillis: Long, utcOffsetSeconds: Int): Long =
    (epochMillis + utcOffsetSeconds * 1_000L).floorDiv(MillisPerDay)

fun historyBucket(createdAtMillis: Long, nowMillis: Long, utcOffsetSeconds: Int): HistoryBucket {
    val daysAgo = localEpochDay(nowMillis, utcOffsetSeconds) - localEpochDay(createdAtMillis, utcOffsetSeconds)
    return when {
        daysAgo <= 0 -> HistoryBucket.Today
        daysAgo == 1L -> HistoryBucket.Yesterday
        daysAgo < 7 -> HistoryBucket.ThisWeek
        else -> HistoryBucket.Earlier
    }
}

/**
 * Splits [conversations] (newest first) into Today / Yesterday / This week / Earlier, keeping
 * their order. Conversations whose date can't be read go to Earlier.
 */
fun groupConversationsByDay(
    conversations: List<ChatConversation>,
    nowMillis: Long,
    utcOffsetSeconds: (Long) -> Int,
): List<HistorySection> =
    conversations
        .groupBy { conversation ->
            parseCreatedAtMillis(conversation.createdAt)
                ?.let { historyBucket(it, nowMillis, utcOffsetSeconds(it)) }
                ?: HistoryBucket.Earlier
        }
        .toList()
        .sortedBy { (bucket, _) -> bucket.ordinal }
        .map { (bucket, items) -> HistorySection(bucket, items) }

/**
 * Short time label for a list row: "14:32" for today and yesterday, "Mon 14:32" this week,
 * "12 Nov 2026" earlier.
 */
fun historyTimeLabel(createdAtMillis: Long, bucket: HistoryBucket, utcOffsetSeconds: Int): String {
    val localMillis = createdAtMillis + utcOffsetSeconds * 1_000L
    val epochDay = localMillis.floorDiv(MillisPerDay)
    val minuteOfDay = (localMillis.mod(MillisPerDay) / 60_000L).toInt()
    val time = "${(minuteOfDay / 60).pad2()}:${(minuteOfDay % 60).pad2()}"
    return when (bucket) {
        HistoryBucket.Today, HistoryBucket.Yesterday -> time
        HistoryBucket.ThisWeek -> "${WeekDays[(epochDay + 3).mod(7L).toInt()]} $time"
        HistoryBucket.Earlier -> {
            val (year, month, day) = civilFromEpochDay(epochDay)
            "$day ${Months[month - 1]} $year"
        }
    }
}

/** Year, month (1–12), day of a day count since 1970-01-01 (proleptic Gregorian). */
internal fun civilFromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
    val z = epochDay + 719_468
    val era = z.floorDiv(146_097L)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val year = (yoe + era * 400 + if (month <= 2) 1 else 0).toInt()
    return Triple(year, month, day)
}

private fun Int.pad2(): String = toString().padStart(2, '0')

private const val MillisPerDay = 86_400_000L
private val WeekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
