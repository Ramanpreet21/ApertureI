package dev.aperture.core.perception

import android.graphics.RectF

/**
 * Result of primary-subject bounding-box detection.
 *
 * Coordinates are normalized to [0, 1] relative to the frame dimensions:
 * `[x, y, width, height]` where (x, y) is the top-left corner.
 */
data class SubjectResult(
    /** Bounding box of the primary subject, or null if none detected. */
    val boundingBox: RectF?
)
