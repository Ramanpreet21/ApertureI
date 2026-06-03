package dev.aperture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dev.aperture.core.camera.ApertureCameraManager
import dev.aperture.core.contract.ContractSerializer
import dev.aperture.core.contract.VisionEngineOutput
import dev.aperture.core.contract.VisionEngineStream
import dev.aperture.core.hardware.DeviceClassifier
import dev.aperture.core.hardware.HardwareTier
import dev.aperture.core.hardware.PerformanceBudget
import dev.aperture.core.hardware.ThermalMonitor
import dev.aperture.core.mock.MockVisionEngine
import dev.aperture.core.perception.DepthEstimator
import dev.aperture.core.perception.HorizonAnalyzer
import dev.aperture.core.perception.LeadingLineDetector
import dev.aperture.core.perception.PipelineCoordinator
import dev.aperture.core.perception.SubjectDetector
import dev.aperture.core.sensors.ImuFusionManager
import dev.aperture.ui.CameraPreviewScreen
import dev.aperture.ui.theme.ApertureTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // ── State ───────────────────────────────────────────────
    private var latestOutput by mutableStateOf<VisionEngineOutput?>(null)
    private var hardwareTier by mutableStateOf(HardwareTier.TIER_1_FULL)
    private var isMockMode by mutableStateOf(false)

    // ── Core subsystems ─────────────────────────────────────
    private lateinit var imuManager: ImuFusionManager
    private lateinit var thermalMonitor: ThermalMonitor
    private lateinit var cameraManager: ApertureCameraManager
    private lateinit var coordinator: PipelineCoordinator
    private lateinit var engineStream: VisionEngineStream
    private lateinit var mockEngine: MockVisionEngine

    // ── Permission launcher ─────────────────────────────────
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onCameraPermissionGranted()
        } else {
            Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Classify hardware and derive budgets.
        hardwareTier = DeviceClassifier.classify(this)
        val budget = PerformanceBudget.forTier(hardwareTier)

        // 2. Initialise subsystems.
        imuManager = ImuFusionManager(this)
        thermalMonitor = ThermalMonitor(this)

        coordinator = PipelineCoordinator(
            horizonAnalyzer = HorizonAnalyzer(),
            subjectDetector = SubjectDetector(),
            depthEstimator = DepthEstimator(),
            leadingLineDetector = LeadingLineDetector(),
            performanceBudget = budget
        )

        cameraManager = ApertureCameraManager(this) { frame ->
            lifecycleScope.launch {
                coordinator.onFrame(frame, imuManager.telemetry.value)
            }
        }

        engineStream = VisionEngineStream(coordinator, thermalMonitor, lifecycleScope)
        mockEngine = MockVisionEngine(lifecycleScope, fps = budget.visionLoopFps.coerceAtLeast(1))

        // 3. Collect vision output into Compose state.
        lifecycleScope.launch {
            engineStream.output.collect { output -> latestOutput = output }
        }
        lifecycleScope.launch {
            mockEngine.output.collect { output -> if (isMockMode) latestOutput = output }
        }

        // 4. Compose UI.
        setContent {
            ApertureTheme {
                CameraPreviewScreen(
                    latestOutput = latestOutput,
                    hardwareTier = hardwareTier,
                    isMockMode = isMockMode,
                    onToggleMockMode = { toggleMockMode() },
                    onPreviewViewReady = { previewView ->
                        requestCameraAndStart(previewView)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        imuManager.start()
        thermalMonitor.start()
        engineStream.start()
    }

    override fun onPause() {
        super.onPause()
        imuManager.stop()
        thermalMonitor.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
        coordinator.shutdown()
    }

    private fun requestCameraAndStart(previewView: androidx.camera.view.PreviewView) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraManager.startCamera(this, previewView)
        } else {
            // Store the previewView reference for use after permission grant.
            pendingPreviewView = previewView
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private var pendingPreviewView: androidx.camera.view.PreviewView? = null

    private fun onCameraPermissionGranted() {
        pendingPreviewView?.let { cameraManager.startCamera(this, it) }
        pendingPreviewView = null
    }

    private fun toggleMockMode() {
        isMockMode = !isMockMode
        if (isMockMode) {
            mockEngine.start()
        }
    }
}
