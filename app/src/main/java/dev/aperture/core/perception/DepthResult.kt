package dev.aperture.core.perception

/**
 * Result of monocular depth estimation on a single frame.
 *
 * Provides a coarse foreground/background binary mask and an overall
 * confidence score for the depth separation.
 */
data class DepthResult(
    /**
     * Flattened binary mask (0 = background, 1 = foreground) at reduced
     * resolution (e.g. 64×64). The mask dimensions are implied by the model
     * output tensor shape.
     */
    val foregroundMask: ByteArray,

    /** Model confidence in the depth separation, range [0, 1]. */
    val depthConfidence: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DepthResult) return false
        return foregroundMask.contentEquals(other.foregroundMask) &&
                depthConfidence == other.depthConfidence
    }

    override fun hashCode(): Int {
        var result = foregroundMask.contentHashCode()
        result = 31 * result + depthConfidence.hashCode()
        return result
    }
}
