package com.kevin.astra.data.settings

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

actual fun createAiConfigurationKeyValueStore(): AiConfigurationKeyValueStore =
    IosAiConfigurationKeyValueStore(NSUserDefaults.standardUserDefaults)

private class IosAiConfigurationKeyValueStore(
    private val userDefaults: NSUserDefaults,
) : AiConfigurationKeyValueStore {
    override fun getString(key: String): String? =
        userDefaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }

    override fun getDouble(key: String): Double? =
        (userDefaults.objectForKey(key) as? NSNumber)?.doubleValue

    override fun putDouble(key: String, value: Double) {
        userDefaults.setDouble(value, forKey = key)
    }

    override fun getInt(key: String): Int? =
        (userDefaults.objectForKey(key) as? NSNumber)?.integerValue?.toInt()

    override fun putInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), forKey = key)
    }

    override fun getBoolean(key: String): Boolean? =
        (userDefaults.objectForKey(key) as? NSNumber)?.boolValue

    override fun putBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, forKey = key)
    }
}
