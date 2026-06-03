package dev.aperture.core.sensors

/**
 * Immutable snapshot of the device's spatial orientation at a single point in time.
 *
 * Values are derived from the fused rotation-vector sensor and converted from
 * radians to degrees for human-readable telemetry and JSON contract emission.
 */
data class TelemetrySnapshot(
    /** Epoch-millis timestamp of the sensor reading. */
    val timestamp: Long,

    /** Device tilt around the X-axis (nose up/down), in degrees. */
    val pitch: Float,

    /** Device tilt around the Y-axis (left/right lean), in degrees. */
    val roll: Float,

    /** Device rotation around the Z-axis (compass heading), in degrees. */
    val yaw: Float
)
