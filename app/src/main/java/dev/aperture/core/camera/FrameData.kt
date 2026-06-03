package dev.aperture.core.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

/**
 * Lightweight wrapper around a single analysis frame from CameraX.
 *
 * Holds the metadata needed by downstream [dev.aperture.core.perception.Analyzer]
 * implementations and provides a lazy [bitmap] accessor for pixel-level processing.
 *
 * **Important:** The caller MUST invoke [close] after the frame has been consumed to
 * release the underlying [ImageProxy] back to CameraX's buffer pool. Failure to do so
 * will stall the analysis pipeline.
 */
data class FrameData(
    /** Epoch-millis timestamp when the frame was captured. */
    val timestamp: Long,

    /** Clockwise rotation needed to align the image with the device's natural orientation. */
    val rotationDegrees: Int,

    /** Width of the raw image buffer in pixels. */
    val width: Int,

    /** Height of the raw image buffer in pixels. */
    val height: Int,

    /** The underlying CameraX image proxy — kept internal to this module. */
    internal val imageProxy: ImageProxy
) {
    /**
     * RGBA bitmap derived from the [imageProxy].
     * Lazily allocated on first access and cached for the lifetime of this frame.
     */
    val bitmap: Bitmap by lazy {
        imageProxy.toBitmap()
    }

    /** Release the underlying camera buffer. Must be called exactly once per frame. */
    fun close() {
        imageProxy.close()
    }
}
