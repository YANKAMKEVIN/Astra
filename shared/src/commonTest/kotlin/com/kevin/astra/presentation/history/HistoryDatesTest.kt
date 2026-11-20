package com.kevin.astra.presentation.history

import com.kevin.astra.domain.history.ChatConversation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryDatesTest {
    // 2026-11-20T10:00:00Z, a Friday.
    private val now = 1_795_168_800_000L
    private val hour = 3_600_000L
    private val day = 24 * hour

    @Test
    fun readsIsoInstantsAndEpochMillis() {
        assertEquals(now, parseCreatedAtMillis("2026-11-20T10:00:00Z"))
        assertEquals(now, parseCreatedAtMillis("1795168800000"))
        assertNull(parseCreatedAtMillis(""))
        assertNull(parseCreatedAtMillis("not a date"))
    }

    @Test
    fun bucketsFollowTheLocalCalendarDay() {
        assertEquals(HistoryBucket.Today, historyBucket(now - 9 * hour, now, 0))
        assertEquals(HistoryBucket.Yesterday, historyBucket(now - 11 * hour, now, 0))
        assertEquals(HistoryBucket.ThisWeek, historyBucket(now - 3 * day, now, 0))
        assertEquals(HistoryBucket.Earlier, historyBucket(now - 8 * day, now, 0))
        // 23:30 UTC the day before is already today at UTC+1.
        assertEquals(HistoryBucket.Today, historyBucket(now - 10 * hour - 30 * 60_000L, now, 3_600))
    }

    @Test
    fun timeLabelsMatchTheirBucket() {
        val t = now + 4 * hour + 32 * 60_000L // 14:32 UTC
        assertEquals("14:32", historyTimeLabel(t, HistoryBucket.Today, 0))
        assertEquals("15:32", historyTimeLabel(t, HistoryBucket.Today, 3_600))
        assertEquals("Fri 14:32", historyTimeLabel(t, HistoryBucket.ThisWeek, 0))
        assertEquals("20 Nov 2026", historyTimeLabel(t, HistoryBucket.Earlier, 0))
        assertEquals("1 Jan 1970", historyTimeLabel(0, HistoryBucket.Earlier, 0))
        assertEquals("29 Feb 2024", historyTimeLabel(1_709_164_800_000L, HistoryBucket.Earlier, 0))
    }

    @Test
    fun groupsKeepOrderAndSendUnreadableDatesToEarlier() {
        val conversations = listOf(
            conversation("a", "2026-11-20T09:00:00Z"),
            conversation("b", "2026-11-19T20:00:00Z"),
            conversation("c", ""),
            conversation("d", "2026-11-17T08:00:00Z"),
            conversation("e", "2026-11-01T08:00:00Z"),
        )

        val sections = groupConversationsByDay(conversations, now) { 0 }

        assertEquals(
            listOf(HistoryBucket.Today, HistoryBucket.Yesterday, HistoryBucket.ThisWeek, HistoryBucket.Earlier),
            sections.map { it.bucket },
        )
        assertEquals(listOf("c", "e"), sections.last().conversations.map { it.id })
    }

    private fun conversation(id: String, createdAt: String) = ChatConversation(
        id = id, title = id, modelName = "", backendName = "", industry = "",
        messages = emptyList(), createdAt = createdAt,
    )
}
