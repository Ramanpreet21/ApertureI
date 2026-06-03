package dev.aperture.core.hardware

/**
 * Concrete resource budgets derived from the active [HardwareTier].
 *
 * These values govern the [dev.aperture.core.perception.PipelineCoordinator]'s
 * frame analysis cadence and maximum memory allocation for ML models.
 *
 * | Budget           | Tier 1 | Tier 2 | Tier 3 |
 * |------------------|--------|--------|--------|
 * | Vision loop FPS  | 30     | 15     | 0      |
 * | Max model memory | 500 MB | 350 MB | 0 MB   |
 * | NPU delegation   | Yes    | Yes    | No     |
 */
data class PerformanceBudget(
    /** Target frames per second for the vision analysis loop. 0 = pipeline disabled. */
    val visionLoopFps: Int,

    /** Maximum combined runtime memory for all loaded TFLite models, in megabytes. */
    val maxModelMemoryMb: Int,

    /** Whether to attempt GPU/NPU delegate initialisation for TFLite. */
    val npuDelegationEnabled: Boolean
) {
    companion object {
        /** Create the budget for the given [tier]. */
        fun forTier(tier: HardwareTier): PerformanceBudget = when (tier) {
            HardwareTier.TIER_1_FULL -> PerformanceBudget(
                visionLoopFps = 30,
                maxModelMemoryMb = 500,
                npuDelegationEnabled = true
            )

            HardwareTier.TIER_2_BALANCED -> PerformanceBudget(
                visionLoopFps = 15,
                maxModelMemoryMb = 350,
                npuDelegationEnabled = true
            )

            HardwareTier.TIER_3_MINIMAL -> PerformanceBudget(
                visionLoopFps = 0,
                maxModelMemoryMb = 0,
                npuDelegationEnabled = false
            )
        }
    }
}
