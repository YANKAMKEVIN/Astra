package com.kevin.astra.domain.benchmark

/**
 * On-device CO₂ estimation based on battery drain.
 *
 * Model:
 *   smartphone battery ≈ 15 Wh (3 750 mAh × 4 V)
 *   1% drain ≈ 0.15 Wh energy consumed
 *   global average grid carbon intensity ≈ 475 g CO₂/kWh  (IEA 2023)
 *   → 1% battery drain ≈ 0.15 × 0.475 / 1000 × 1e6 ≈ 71 mg CO₂
 *
 * Cloud LLM reference (GPT-4-class, ~500B params):
 *   ~0.001–0.003 kWh per 1 000 tokens  → 0.47–1.4 g CO₂ per 1 000 tokens
 *   We use 2.0 g per 1 000 tokens as a conservative worst-case cloud estimate.
 */
object Co2Estimator {

    private const val BATTERY_WH = 15.0
    private const val GRID_G_CO2_PER_KWH = 475.0
    private const val MG_PER_G = 1_000.0

    private const val CLOUD_G_CO2_PER_1K_TOKENS = 2.0

    /** On-device CO₂ in milligrams from battery drain percentage (0–100). */
    fun onDeviceMg(batteryDrainPercent: Int): Double {
        val whConsumed = BATTERY_WH * batteryDrainPercent / 100.0
        return whConsumed * GRID_G_CO2_PER_KWH / 1_000.0 * MG_PER_G
    }

    /** Equivalent cloud CO₂ in milligrams for the same number of tokens. */
    fun cloudEquivalentMg(tokensGenerated: Int): Double =
        tokensGenerated.toDouble() / 1_000.0 * CLOUD_G_CO2_PER_1K_TOKENS * MG_PER_G

    /** Human-readable display string for milligrams: "12 mg" or "1.3 g". */
    fun display(mg: Double): String =
        if (mg < 1_000) "${mg.toInt()} mg CO₂"
        else "${(mg / 10.0).toInt() / 100.0} g CO₂"

    /** Savings percentage: how much less CO₂ on-device vs cloud (clamped 0–100). */
    fun savingsPercent(onDeviceMg: Double, cloudMg: Double): Int {
        if (cloudMg <= 0) return 0
        return ((1.0 - onDeviceMg / cloudMg) * 100).toInt().coerceIn(0, 100)
    }
}
