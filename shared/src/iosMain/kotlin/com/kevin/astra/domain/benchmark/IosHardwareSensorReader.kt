package com.kevin.astra.domain.benchmark

class IosHardwareSensorReader : HardwareSensorReader {
    override fun read(): HardwareSnapshot = HardwareSnapshot(
        batteryPercent = -1,
        temperatureCelsius = -1f,
        timestampMs = 0L,
    )
}

actual fun createHardwareSensorReader(): HardwareSensorReader = IosHardwareSensorReader()
