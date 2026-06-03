package dev.aperture.core.perception

import android.graphics.RectF
import dev.aperture.core.camera.FrameData

/**
 * Detects the primary subject in a camera frame and returns its bounding box.
 *
 * **Current status:** Stub implementation returning a centred bounding box.
 * In production, this will run a MobileNet-SSD or EfficientDet-Lite model via TFLite.
 *
 * Model contract:
 * - Input:  [1, 320, 320, 3] uint8 RGB tensor
 * - Output: detection boxes [1, N, 4], scores [1, N], classes [1, N], count [1]
 */
class SubjectDetector : Analyzer<SubjectResult> {

    // TODO: Replace with real TFLite interpreter

    @Suppress("MagicNumber")
    override suspend fun analyze(frame: FrameData): SubjectResult {
        // Stub: simulate a subject at roughly the rule-of-thirds intersection.
        return SubjectResult(
            boundingBox = RectF(0.33f, 0.45f, 0.53f, 0.80f)
        )
    }

    override fun close() {
        // interpreter.close()
    }
}
