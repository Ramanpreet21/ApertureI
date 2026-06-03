package dev.aperture.core.perception

import dev.aperture.core.camera.FrameData

/**
 * Detects the horizon line in a camera frame and measures its tilt angle.
 *
 * **Current status:** Stub implementation returning synthetic data.
 * In production, this will load `assets/models/horizon_detector.tflite` and run
 * INT8-quantized inference via the device NPU.
 *
 * Model contract:
 * - Input:  [1, 224, 224, 3] uint8 RGB tensor
 * - Output: [1, 2] float32 — [detected_probability, tilt_degrees]
 */
class HorizonAnalyzer : Analyzer<HorizonResult> {

    // TODO: Replace with real TFLite interpreter
    // private lateinit var interpreter: Interpreter

    override suspend fun analyze(frame: FrameData): HorizonResult {
        // Stub: simulate a level horizon with minor noise.
        // Production implementation would:
        //   1. Resize frame.bitmap to 224×224
        //   2. Normalize pixel values to model input range
        //   3. Run interpreter.run(inputBuffer, outputBuffer)
        //   4. Parse outputBuffer into HorizonResult
        return HorizonResult(
            detected = true,
            tiltDegrees = 0f
        )
    }

    override fun close() {
        // interpreter.close()
    }
}
