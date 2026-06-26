package com.kevin.astra.data.settings

class TestAiConfigurationKeyValueStore : AiConfigurationKeyValueStore {
    private val values = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as? String
    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getDouble(key: String): Double? = values[key] as? Double
    override fun putDouble(key: String, value: Double) {
        values[key] = value
    }

    override fun getInt(key: String): Int? = values[key] as? Int
    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getBoolean(key: String): Boolean? = values[key] as? Boolean
    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}

fun testAiConfigurationRepository(
    keyValueStore: AiConfigurationKeyValueStore = TestAiConfigurationKeyValueStore(),
): PersistentAiConfigurationRepository =
    PersistentAiConfigurationRepository(
        localDataSource = AiConfigurationLocalDataSource(keyValueStore),
    )
