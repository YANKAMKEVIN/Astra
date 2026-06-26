package com.kevin.astra.data.settings

import android.content.Context
import android.content.SharedPreferences

private var astraApplicationContext: Context? = null

fun initializeAndroidAiConfigurationStorage(context: Context) {
    astraApplicationContext = context.applicationContext
}

actual fun createAiConfigurationKeyValueStore(): AiConfigurationKeyValueStore =
    astraApplicationContext
        ?.getSharedPreferences("astra_ai_configuration", Context.MODE_PRIVATE)
        ?.let(::AndroidAiConfigurationKeyValueStore)
        ?: InMemoryAiConfigurationKeyValueStore()

private class AndroidAiConfigurationKeyValueStore(
    private val sharedPreferences: SharedPreferences,
) : AiConfigurationKeyValueStore {
    override fun getString(key: String): String? =
        sharedPreferences.getString(key, null)

    override fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun getDouble(key: String): Double? =
        if (sharedPreferences.contains(key)) {
            sharedPreferences.getString(key, null)?.toDoubleOrNull()
        } else {
            null
        }

    override fun putDouble(key: String, value: Double) {
        sharedPreferences.edit().putString(key, value.toString()).apply()
    }

    override fun getInt(key: String): Int? =
        if (sharedPreferences.contains(key)) sharedPreferences.getInt(key, 0) else null

    override fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    override fun getBoolean(key: String): Boolean? =
        if (sharedPreferences.contains(key)) sharedPreferences.getBoolean(key, false) else null

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }
}

private class InMemoryAiConfigurationKeyValueStore : AiConfigurationKeyValueStore {
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
