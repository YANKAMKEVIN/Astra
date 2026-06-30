package com.kevin.astra.domain.benchmark

data class HardwareSnapshot(
    val batteryPercent: Int,
    val temperatureCelsius: Float,
    val timestampMs: Long,
)

interface HardwareSensorReader {
    fun read(): HardwareSnapshot
}

expect fun createHardwareSensorReader(): HardwareSensorReader
