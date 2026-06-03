package dev.aperture.core.mock

import dev.aperture.core.contract.VisionEngineOutput
import dev.aperture.core.contract.ContractSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Emits a looping sequence of pre-baked [VisionEngineOutput] payloads at a
 * configurable frame rate — no camera or NPU hardware required.
 *
 * Enables downstream module development (Ghost Template, Active Guidance,
 * Capture Control) to validate their rendering and logic against realistic
 * data without needing live sensor input.
 *
 * Usage:
 * ```kotlin
 * val mock = MockVisionEngine(scope, fps = 30)
 * mock.output.collect { payload -> /* render */ }
 * mock.start()
 * ```
 */
class MockVisionEngine(
    private val scope: CoroutineScope,
    private val fps: Int = 30
) {
    private val _output = MutableSharedFlow<VisionEngineOutput>(
        replay = 1,
        extraBufferCapacity = 1
    )

    /** Subscribe to receive mock vision engine contract payloads. */
    val output: SharedFlow<VisionEngineOutput> = _output.asSharedFlow()

    /** The most recent raw JSON string, useful for debug overlays. */
    @Volatile
    var latestJson: String = "{}"
        private set

    private val payloads: List<VisionEngineOutput> by lazy {
        SamplePayloads.all().map { json ->
            ContractSerializer.fromJson(json)
        }
    }

    /**
     * Start emitting mock payloads in a loop.
     * The loop runs until the parent [scope] is cancelled.
     */
    fun start() {
        val frameDelayMs = if (fps > 0) 1000L / fps else 1000L

        scope.launch {
            var index = 0
            while (isActive) {
                val payload = payloads[index % payloads.size]

                // Update timestamp to current time for realistic behaviour.
                val livePayload = payload.copy(timestamp = System.currentTimeMillis())

                latestJson = ContractSerializer.toJson(livePayload)
                _output.emit(livePayload)

                index++
                delay(frameDelayMs)
            }
        }
    }
}
