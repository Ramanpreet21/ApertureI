package dev.aperture.core.contract

import dev.aperture.core.hardware.ThermalMonitor
import dev.aperture.core.perception.PipelineCoordinator
import dev.aperture.core.perception.VisionFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Reactive stream that transforms raw [VisionFrame] outputs from the
 * [PipelineCoordinator] into validated [VisionEngineOutput] JSON-contract
 * objects and exposes them as a [SharedFlow].
 *
 * This is the **single public API surface** that downstream modules
 * (Ghost Template, Active Guidance, Capture Control) subscribe to.
 */
class VisionEngineStream(
    private val coordinator: PipelineCoordinator,
    private val thermalMonitor: ThermalMonitor,
    private val scope: CoroutineScope
) {
    private val _output = MutableSharedFlow<VisionEngineOutput>(
        replay = 1,
        extraBufferCapacity = 1
    )

    /** Subscribe to receive validated vision engine contract payloads. */
    val output: SharedFlow<VisionEngineOutput> = _output.asSharedFlow()

    /** The most recent raw JSON string, useful for debug overlays. */
    @Volatile
    var latestJson: String = "{}"
        private set

    /**
     * Begin collecting from the pipeline coordinator and emitting contract payloads.
     */
    fun start() {
        scope.launch {
            coordinator.visionFrames.collect { frame ->
                val contractOutput = ContractSerializer.fromVisionFrame(
                    frame = frame,
                    thermalState = thermalMonitor.thermalState.value
                )
                latestJson = ContractSerializer.toJson(contractOutput)
                _output.emit(contractOutput)
            }
        }
    }
}
