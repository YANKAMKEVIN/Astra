package com.kevin.astra.core.ai

/**
 * Hardware the LiteRT-LM engine runs a `.litertlm` model on. CPU works on every device and is the
 * default; GPU and NPU depend on the chipset (and, for the NPU, on a model compiled for it) and
 * have not been validated on a real phone yet, so they are opt-in and always fall back to CPU.
 */
enum class HardwareAcceleration(val label: String) {
    Cpu("CPU"),
    Gpu("GPU"),
    Npu("NPU"),
    ;

    /** Accelerators tried in order: the chosen one, then CPU. */
    val fallbackOrder: List<HardwareAcceleration>
        get() = if (this == Cpu) listOf(Cpu) else listOf(this, Cpu)

    companion object {
        fun fromName(name: String?): HardwareAcceleration = entries.firstOrNull { it.name == name } ?: Cpu
    }
}

/** An engine created by [initializeWithFallback], with the accelerator it actually runs on. */
class AcceleratedEngine<E>(
    val engine: E,
    val acceleration: HardwareAcceleration,
    /** Set when the preferred accelerator failed: shown to the user next to the answer. */
    val fallbackReason: String?,
)

/**
 * Creates an engine on [preferred], falling back along [HardwareAcceleration.fallbackOrder] when
 * creation throws (unsupported GPU delegate, no NPU library…). Rethrows the last error when every
 * accelerator fails, CPU included.
 */
fun <E> initializeWithFallback(
    preferred: HardwareAcceleration,
    onFailure: (HardwareAcceleration, Throwable) -> Unit = { _, _ -> },
    create: (HardwareAcceleration) -> E,
): AcceleratedEngine<E> {
    var firstFailure: Pair<HardwareAcceleration, Throwable>? = null
    var lastError: Throwable? = null
    for (acceleration in preferred.fallbackOrder) {
        try {
            val engine = create(acceleration)
            val reason = firstFailure?.let { (failed, error) ->
                "${failed.label} unavailable (${error.message ?: error::class.simpleName}); ran on ${acceleration.label}."
            }
            return AcceleratedEngine(engine, acceleration, reason)
        } catch (error: Throwable) {
            onFailure(acceleration, error)
            if (firstFailure == null) firstFailure = acceleration to error
            lastError = error
        }
    }
    throw lastError ?: IllegalStateException("No accelerator to try.")
}
