package dev.aperture.core.contract

import com.google.gson.annotations.SerializedName

/**
 * Inter-module JSON contract payload emitted by the Core Vision Engine (Module 1).
 *
 * This data class is the **single public API surface** that Modules 2, 3, and 4
 * subscribe to. The JSON schema matches the spec's Section 4 contract:
 *
 * ```json
 * {
 *   "timestamp": 1718304000000,
 *   "telemetry": { "pitch": 1.25, "roll": -0.50, "yaw": 42.10 },
 *   "extracted_features": {
 *     "horizon_detected": true,
 *     "horizon_tilt_degrees": -0.5,
 *     "primary_subject_bounding_box": [0.33, 0.45, 0.20, 0.35]
 *   },
 *   "hardware_performance_state": {
 *     "target_device": "Galaxy_S24",
 *     "npu_load_percentage": 64.2,
 *     "thermal_throttling_state": "nominal"
 *   }
 * }
 * ```
 */
data class VisionEngineOutput(
    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("telemetry")
    val telemetry: Telemetry,

    @SerializedName("extracted_features")
    val extractedFeatures: ExtractedFeatures,

    @SerializedName("hardware_performance_state")
    val hardwarePerformanceState: HardwarePerformanceState
)

data class Telemetry(
    @SerializedName("pitch")
    val pitch: Float,

    @SerializedName("roll")
    val roll: Float,

    @SerializedName("yaw")
    val yaw: Float
)

data class ExtractedFeatures(
    @SerializedName("horizon_detected")
    val horizonDetected: Boolean,

    @SerializedName("horizon_tilt_degrees")
    val horizonTiltDegrees: Float,

    @SerializedName("primary_subject_bounding_box")
    val primarySubjectBoundingBox: List<Float>?
)

data class HardwarePerformanceState(
    @SerializedName("target_device")
    val targetDevice: String,

    @SerializedName("npu_load_percentage")
    val npuLoadPercentage: Float,

    @SerializedName("thermal_throttling_state")
    val thermalThrottlingState: String
)
