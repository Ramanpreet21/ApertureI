package dev.aperture.core.hardware

/**
 * Device capability tiers that govern the vision pipeline's resource budget.
 *
 * Tier classification is performed once at app startup by [DeviceClassifier]
 * and can be overridden in debug builds via `BuildConfig.HARDWARE_TIER_OVERRIDE`.
 */
enum class HardwareTier {

    /**
     * Full Performance — Galaxy S24 class.
     * Real-time semantic segmentation, dynamic depth mapping, vector alignment.
     * 30 fps vision loop, NPU delegation enabled.
     */
    TIER_1_FULL,

    /**
     * Balanced Performance — Galaxy S22 / iPhone 13 class.
     * Reduced segmentation sampling, cached depth maps during panning.
     * 15 fps vision loop, NPU delegation enabled.
     */
    TIER_2_BALANCED,

    /**
     * Minimal — Legacy or low-end devices.
     * Vision pipeline fully disabled. Static geometric grids only.
     * 0 fps vision loop.
     */
    TIER_3_MINIMAL
}
