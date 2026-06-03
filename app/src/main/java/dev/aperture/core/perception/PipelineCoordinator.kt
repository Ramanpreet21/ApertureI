package dev.aperture.core.perception

import dev.aperture.core.camera.FrameData
import dev.aperture.core.hardware.PerformanceBudget
import dev.aperture.core.sensors.TelemetrySnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Orchestrates all [Analyzer] implementations and merges their results into
 * a single [VisionFrame] per camera frame.
 *
 * Analyzers run **concurrently** via structured coroutines. The coordinator
 * respects the active [PerformanceBudget] — if the previous analysis cycle
 * is still in-flight when a new frame arrives, the new frame is dropped to
 * maintain the target fps budget.
 *
 * When the hardware tier is [dev.aperture.core.hardware.HardwareTier.TIER_3_MINIMAL],
 * the pipeline is fully disabled and no frames are processed.
 */
class PipelineCoordinator(
    private val horizonAnalyzer: HorizonAnalyzer,
    private val subjectDetector: SubjectDetector,
    private val depthEstimator: DepthEstimator,
    private val leadingLineDetector: LeadingLineDetector,
    private val performanceBudget: PerformanceBudget
) {
    private val _visionFrames = MutableSharedFlow<VisionFrame>(
        replay = 1,
        extraBufferCapacity = 1
    )

    /** Stream of completed vision frames. Downstream modules subscribe here. */
    val visionFrames: SharedFlow<VisionFrame> = _visionFrames.asSharedFlow()

    /** Guard against concurrent frame processing. */
    private val processing = AtomicBoolean(false)

    /**
     * Submit a camera frame for analysis.
     *
     * If the pipeline is already processing a frame, this call returns
     * immediately and the frame is dropped (back-pressure shedding).
     *
     * @param frame     Raw camera frame to analyze.
     * @param telemetry IMU snapshot at the time of frame capture.
     */
    suspend fun onFrame(frame: FrameData, telemetry: TelemetrySnapshot) {
        // Skip if pipeline is disabled for this tier.
        if (performanceBudget.visionLoopFps <= 0) {
            frame.close()
            return
        }

        // Drop frame if previous cycle is still running.
        if (!processing.compareAndSet(false, true)) {
            frame.close()
            return
        }

        try {
            val visionFrame = processFrame(frame, telemetry)
            _visionFrames.emit(visionFrame)
        } finally {
            frame.close()
            processing.set(false)
        }
    }

    /**
     * Run all analyzers concurrently and merge results.
     */
    private suspend fun processFrame(
        frame: FrameData,
        telemetry: TelemetrySnapshot
    ): VisionFrame = coroutineScope {
        val horizonDeferred = async { horizonAnalyzer.analyze(frame) }
        val subjectDeferred = async { subjectDetector.analyze(frame) }
        val depthDeferred = async { depthEstimator.analyze(frame) }
        val linesDeferred = async { leadingLineDetector.analyze(frame) }

        VisionFrame(
            timestamp = frame.timestamp,
            telemetry = telemetry,
            horizon = horizonDeferred.await(),
            subject = subjectDeferred.await(),
            depth = depthDeferred.await(),
            leadingLines = linesDeferred.await()
        )
    }

    /**
     * Release all analyzer resources. Call during application shutdown.
     */
    fun shutdown() {
        horizonAnalyzer.close()
        subjectDetector.close()
        depthEstimator.close()
        leadingLineDetector.close()
    }
}
