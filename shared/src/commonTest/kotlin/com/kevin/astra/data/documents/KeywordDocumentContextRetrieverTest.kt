package com.kevin.astra.data.documents

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class KeywordDocumentContextRetrieverTest {
    @Test
    fun retrievesRelevantPumpRestartContext() = runBlocking {
        val document = EmbeddedMaintenanceDocument.industrialPumpMaintenanceGuide
        val chunks = SimpleDocumentIndexer().index(document)
        val retriever = KeywordDocumentContextRetriever()

        val context = retriever.retrieve(
            question = "How should I restart Pump A after emergency shutdown?",
            chunks = chunks,
        )

        assertTrue(context.text.contains("Pump Restart Procedure"))
        assertTrue(context.text.contains("Emergency Shutdown Procedure"))
    }
}
