package com.kevin.astra.domain.modelmanager

actual fun createModelReadinessProvider(): ModelReadinessProvider =
    StaticModelReadinessProvider(
        platformName = "iOS",
        supportsLiteRtLmAssets = false,
    )

