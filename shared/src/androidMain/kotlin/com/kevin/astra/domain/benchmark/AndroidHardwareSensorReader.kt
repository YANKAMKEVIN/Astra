package com.kevin.astra.domain.benchmark

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

private var applicationContext: Context? = null

fun initializeAndroidHardwareSensorReader(context: Context) {
    applicationContext = context.applicationContext
}

class AndroidHardwareSensorReader(private val context: Context) : HardwareSensorReader {
    override fun read(): HardwareSnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempCelsius = if (rawTemp >= 0) rawTemp / 10f else -1f
        return HardwareSnapshot(
            batteryPercent = batteryPercent,
            temperatureCelsius = tempCelsius,
            timestampMs = System.currentTimeMillis(),
        )
    }
}

class MockHardwareSensorReader : HardwareSensorReader {
    override fun read(): HardwareSnapshot = HardwareSnapshot(
        batteryPercent = -1,
        temperatureCelsius = -1f,
        timestampMs = System.currentTimeMillis(),
    )
}

actual fun createHardwareSensorReader(): HardwareSensorReader {
    val ctx = applicationContext
    return if (ctx != null) AndroidHardwareSensorReader(ctx) else MockHardwareSensorReader()
}
