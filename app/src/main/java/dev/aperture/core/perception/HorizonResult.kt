package dev.aperture.core.perception

/**
 * Result of horizon-line detection on a single frame.
 */
data class HorizonResult(
    /** Whether a horizon line was detected in the frame. */
    val detected: Boolean,

    /** Tilt angle of the detected horizon in degrees. Positive = clockwise tilt. */
    val tiltDegrees: Float
)
