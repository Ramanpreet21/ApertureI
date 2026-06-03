package dev.aperture.core.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Owns the CameraX lifecycle: binds [Preview] and [ImageAnalysis] use-cases to
 * the provided [LifecycleOwner], and feeds raw frames into [onFrameAvailable].
 *
 * Downstream consumers (the perception pipeline) subscribe to frames through the
 * callback rather than polling — CameraX drives the cadence via its internal
 * back-pressure strategy ([ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST]).
 */
class ApertureCameraManager(
    private val context: Context,
    private val onFrameAvailable: (FrameData) -> Unit
) {
    private val _state = MutableStateFlow<CameraState>(CameraState.Idle)

    /** Observable camera lifecycle state. */
    val state: StateFlow<CameraState> = _state.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Initialise the camera and bind use-cases.
     *
     * @param lifecycleOwner Activity or Fragment that owns the camera lifecycle.
     * @param previewView    The [PreviewView] surface to render the live viewfinder on.
     */
    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
                bindUseCases(lifecycleOwner, previewView)
            } catch (e: Exception) {
                _state.value = CameraState.Error(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // ──────────────────────────────────────────────────────────────
    //  Internal: Use-case binding
    // ──────────────────────────────────────────────────────────────

    @Suppress("MagicNumber")
    private fun bindUseCases(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val provider = cameraProvider ?: return

        // 1. Preview — renders the live viewfinder at sensor-native resolution.
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // 2. ImageAnalysis — feeds frames into the perception pipeline.
        //    Target 1080p for analysis to keep memory bounded while preserving
        //    enough detail for horizon / subject detection.
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1920, 1080))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    val frame = FrameData(
                        timestamp = System.currentTimeMillis(),
                        rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                        width = imageProxy.width,
                        height = imageProxy.height,
                        imageProxy = imageProxy
                    )
                    onFrameAvailable(frame)
                }
            }

        // 3. Bind everything to the back camera.
        try {
            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            _state.value = CameraState.Previewing(camera.cameraInfo)
        } catch (e: Exception) {
            _state.value = CameraState.Error(e)
        }
    }

    /**
     * Unbind all use-cases and release the analysis executor.
     * Safe to call multiple times.
     */
    fun shutdown() {
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
        _state.value = CameraState.Idle
    }
}
