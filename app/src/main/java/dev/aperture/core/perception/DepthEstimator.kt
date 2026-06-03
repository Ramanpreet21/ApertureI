package dev.aperture.core.perception

import dev.aperture.core.camera.FrameData

/**
 * Estimates monocular depth to separate foreground subjects from background.
 *
 * **Current status:** Stub implementation returning a uniform mask.
 * In production, this will run a MiDaS-small or similar depth model via TFLite.
 *
 * Model contract:
 * - Input:  [1, 256, 256, 3] float32 normalised RGB tensor
 * - Output: [1, 64, 64, 1] float32 relative depth map
 */
class DepthEstimator : Analyzer<DepthResult> {

    // TODO: Replace with real TFLite interpreter

    @Suppress("MagicNumber")
    override suspend fun analyze(frame: FrameData): DepthResult {
        // Stub: return an empty 64×64 mask with mid-range confidence.
        val maskSize = 64 * 64
        return DepthResult(
            foregroundMask = ByteArray(maskSize) { 0 },
            depthConfidence = 0.5f
        )
    }

    override fun close() {
        // interpreter.close()
    }
}
