package dev.aperture.core.perception

import dev.aperture.core.sensors.TelemetrySnapshot

/**
 * Aggregate output of a single perception-pipeline cycle.
 *
 * Combines all analyzer results with the IMU telemetry snapshot that was
 * current at the time the frame was captured. This is the internal
 * representation that gets serialized into the inter-module JSON contract
 * by [dev.aperture.core.contract.ContractSerializer].
 */
data class VisionFrame(
    /** Epoch-millis timestamp of the originating camera frame. */
    val timestamp: Long,

    /** Device orientation at capture time. */
    val telemetry: TelemetrySnapshot,

    /** Horizon detection result. */
    val horizon: HorizonResult,

    /** Primary subject detection result. */
    val subject: SubjectResult,

    /** Monocular depth estimation result. */
    val depth: DepthResult,

    /** Leading line detection result. */
    val leadingLines: LeadingLineResult
)
