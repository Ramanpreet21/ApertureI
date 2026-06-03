package dev.aperture.core.perception

import dev.aperture.core.camera.FrameData

/**
 * Common contract for all vision-pipeline analyzers.
 *
 * Each analyzer accepts a [FrameData] and returns a strongly-typed result [T].
 * Implementations are expected to be **suspend** functions so the
 * [PipelineCoordinator] can run them concurrently via structured concurrency.
 *
 * Analyzers own heavyweight resources (TFLite interpreters, GPU delegates) and
 * MUST release them in [close].
 */
interface Analyzer<T> {

    /**
     * Run inference on [frame] and return the typed result.
     *
     * Implementations should NOT call [FrameData.close] — the coordinator
     * manages the frame lifecycle after all analyzers have finished.
     */
    suspend fun analyze(frame: FrameData): T

    /**
     * Release any native resources (interpreters, delegates, buffers).
     */
    fun close()
}
