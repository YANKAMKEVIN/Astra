package com.kevin.astra.domain.documents

import com.kevin.astra.data.documents.BowEmbeddingEngine

actual fun createEmbeddingEngine(): EmbeddingEngine = BowEmbeddingEngine()
