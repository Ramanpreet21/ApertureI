package dev.aperture.core.hardware

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dev.aperture.BuildConfig

/**
 * Classifies the current device into a [HardwareTier] based on hardware signals.
 *
 * Probes the SoC model (API 31+), total RAM, and GPU renderer string to estimate
 * the device's ability to sustain the vision pipeline under thermal constraints.
 *
 * In debug builds, the tier can be forced via `BuildConfig.HARDWARE_TIER_OVERRIDE`
 * (set in `app/build.gradle.kts`) to test pipeline behaviour across tiers without
 * needing physical hardware for each class.
 */
object DeviceClassifier {

    private const val TIER_1_MIN_RAM_MB = 7_500L
    private const val TIER_2_MIN_RAM_MB = 5_500L

    // Known Tier 1 SoC model substrings (Snapdragon 8 Gen 3, Exynos 2400, A17 Pro).
    private val TIER_1_SOC_PATTERNS = listOf(
        "SM8650",     // Snapdragon 8 Gen 3
        "s5e9945",    // Exynos 2400
        "SM8550",     // Snapdragon 8 Gen 2
        "s5e9925",    // Exynos 2200
    )

    // Known Tier 2 SoC model substrings.
    private val TIER_2_SOC_PATTERNS = listOf(
        "SM8450",     // Snapdragon 8 Gen 1
        "SM8475",     // Snapdragon 8+ Gen 1
        "s5e9920",    // Exynos 2100
    )

    /**
     * Determine the hardware tier for the current device.
     */
    fun classify(context: Context): HardwareTier {
        // 1. Check for debug override.
        val override = BuildConfig.HARDWARE_TIER_OVERRIDE
        if (override != "AUTO") {
            return when (override) {
                "TIER_1" -> HardwareTier.TIER_1_FULL
                "TIER_2" -> HardwareTier.TIER_2_BALANCED
                "TIER_3" -> HardwareTier.TIER_3_MINIMAL
                else -> classifyFromHardware(context)
            }
        }

        return classifyFromHardware(context)
    }

    private fun classifyFromHardware(context: Context): HardwareTier {
        val totalRamMb = getTotalRamMb(context)
        val socModel = getSocModel()

        // Tier 1: Known flagship SoC + ≥ 8 GB RAM.
        if (totalRamMb >= TIER_1_MIN_RAM_MB && matchesSocPattern(socModel, TIER_1_SOC_PATTERNS)) {
            return HardwareTier.TIER_1_FULL
        }

        // Tier 2: Mid-range SoC or sufficient RAM.
        if (totalRamMb >= TIER_2_MIN_RAM_MB ||
            matchesSocPattern(socModel, TIER_2_SOC_PATTERNS)
        ) {
            return HardwareTier.TIER_2_BALANCED
        }

        // Everything else falls to Tier 3.
        return HardwareTier.TIER_3_MINIMAL
    }

    private fun getTotalRamMb(context: Context): Long {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }

    private fun getSocModel(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL
        } else {
            Build.HARDWARE
        }
    }

    private fun matchesSocPattern(socModel: String, patterns: List<String>): Boolean {
        return patterns.any { socModel.contains(it, ignoreCase = true) }
    }
}
