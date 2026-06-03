package dev.aperture.core.perception

/**
 * A single detected leading line in normalised frame coordinates.
 *
 * All coordinates are in [0, 1] relative to the frame dimensions.
 */
data class Line(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,

    /** Model confidence for this line, range [0, 1]. */
    val confidence: Float
)

/**
 * Result of leading-line detection on a single frame.
 */
data class LeadingLineResult(
    /** Dominant leading lines detected, ordered by descending confidence. */
    val lines: List<Line>
)
