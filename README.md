# ApertureAI

An experimental Android application exploring real-time AR-style composition assistance using CameraX frame analysis, IMU sensor fusion, and on-device ML inference. The system processes live camera frames and device motion data through a modular pipeline to generate spatial overlay guidance for photographic composition.

This project is **in active development**. The pipeline infrastructure is functional, but all ML inference components currently use stub implementations that return synthetic data. No production-quality computer vision models are integrated.

---

## Current System Status

### Working

| Component | Description |
|---|---|
| CameraX lifecycle manager | Binds `Preview` + `ImageAnalysis` use-cases to `ProcessCameraProvider`. Delivers 1080p YUV\_420\_888 frames via `STRATEGY_KEEP_ONLY_LATEST`. |
| YUV-to-Bitmap conversion | `ImagePreprocessor` converts `ImageProxy` (YUV\_420\_888) to `Bitmap` and then to `TensorImage` (resized, uint8 RGB) using TFLite Support Library APIs. Runs on `Dispatchers.Default`. |
| IMU sensor fusion | Reads `TYPE_GAME_ROTATION_VECTOR` (falls back to `TYPE_ROTATION_VECTOR`), converts quaternion to Euler angles (pitch, roll, yaw), and emits via `StateFlow`. Pre-allocates scratch arrays to avoid per-event GC pressure. |
| Pipeline coordinator | Orchestrates four analyzers in parallel using `coroutineScope { async {} }`. Drops frames when the previous analysis cycle is still in-flight. Uses `AtomicBoolean` for lock-free concurrency control. |
| Hardware tier classification | Classifies devices into three tiers using `Build.SOC_MODEL`, `Build.MODEL`, and `ActivityManager.memoryInfo`. Maps tiers to concrete performance budgets (vision loop FPS, memory caps, NPU delegation). |
| Thermal monitoring | Listens to `PowerManager.THERMAL_STATUS_*` callbacks (API 29+) and maps to a simplified `ThermalState` enum (NOMINAL through CRITICAL). Dynamically downgrades the effective hardware tier: moderate throttling forces Tier 1 down to Tier 2; severe/critical forces any tier to Tier 3 (pipeline disabled). |
| JSON contract serialization | `Gson`-based serializer with `@SerializedName` annotations enforcing the inter-module JSON schema. Includes `fromVisionFrame()` for internal-to-contract conversion, `validate()` returning error lists, and `validateOrThrow()` for CI enforcement. |
| Reactive data stream | `VisionEngineStream` collects from `PipelineCoordinator`, converts `VisionFrame` to `VisionEngineOutput` via `ContractSerializer`, caches the latest JSON string for debug overlays, and emits on a `SharedFlow` (replay=1). |
| Mock data engine | `MockVisionEngine` loops through 8 pre-baked `VisionEngineOutput` payloads at configurable frame rates (timestamps updated to current time), enabling downstream development without camera hardware. |
| Camera preview UI | Jetpack Compose screen with full-bleed CameraX preview, semi-transparent debug overlay (telemetry, tier, NPU load, thermal state), and live/mock mode toggle. |
| Unit tests | `ContractSerializerTest` (roundtrip serialization, snake\_case key verification, validation error detection for invalid timestamps/NPU/bounding boxes) and `PipelineCoordinatorStressTest` (4 tests: sequential processing, backpressure frame-dropping, Tier 3 pipeline disablement, 200-frame heavy load stress test with resource leak verification). |

### Stubbed (Not Functional)

These components exist structurally and conform to the `Analyzer<T>` interface, but contain **no real inference logic**. They return randomized or hardcoded synthetic data.

| Component | What It Returns | Planned Model Contract |
|---|---|---|
| `HorizonAnalyzer` | Hardcoded `HorizonResult(detected=true, tiltDegrees=0f)`. TFLite interpreter code is commented out with TODO markers. | `[1, 224, 224, 3]` uint8 RGB input, `[1, 2]` float32 output |
| `SubjectDetector` | Hardcoded bounding box at the rule-of-thirds position `RectF(0.33, 0.45, 0.53, 0.80)`. | MobileNet-SSD or EfficientDet-Lite, `[1, 320, 320, 3]` input |
| `DepthEstimator` | Zero-filled 64x64 `ByteArray` with 0.5 confidence. | MiDaS-small, `[1, 256, 256, 3]` input, `[1, 64, 64, 1]` depth map |
| `LeadingLineDetector` | Two hardcoded diagonal `Line` objects with static confidence values. | LSD or custom TFLite, `[1, 256, 256, 1]` grayscale input |

No `.tflite` model files are present in `assets/models/`. The pipeline runs end-to-end, but all perception output is synthetic.

### Not Implemented

The following capabilities are described in the project architecture document but have **no code written**:

| Planned Component | Description |
|---|---|
| Ghost Template Engine | Reference image parsing, geometric/luminance/depth feature extraction, and template matching. |
| Guidance overlay rendering | Compositional wireframes (rule-of-thirds, golden ratio, vanishing point), color-coded alignment indicators (amber-to-emerald transitions). |
| Haptic feedback engine | Translation of geometric alignment errors into device vibration patterns. |
| Adjustable strictness control | UI slider for alignment tolerance (strict match to gentle suggestion). |
| Rule break detection | Intentional deviation detection after 2-second hold, suppressing corrective feedback. |
| Capture control | Smart shutter (auto-fire on stability + alignment convergence) and manual shutter modes. |
| Skill progression tiers | Beginner/Intermediate/Advanced modes with varying automation levels. |
| Post-capture review | Side-by-side comparison of captured image against template with geometric analysis card. |
| Taste learning daemon | Weighted feature histogram tracking user preference signals (export/favorite vs. delete). |
| iOS application | SwiftUI/AVFoundation counterpart. No iOS code exists. |
| Real ML models | Production object detection, semantic segmentation, monocular depth estimation, and leading-line extraction models. |

---

## Architecture Overview

```
CameraX (Preview + ImageAnalysis)
    |
    v
FrameData (timestamp, rotation, dimensions, ImageProxy, lazy Bitmap)
    |                                          IMU SensorManager
    |                                               |
    |                                               v
    |                                    ImuFusionManager
    |                                    (quaternion -> Euler angles)
    |                                               |
    |                                               v
    |                                    TelemetrySnapshot (pitch, roll, yaw)
    |                                               |
    +-----------------------------------------------+
    |
    v
PipelineCoordinator
    |
    +---> HorizonAnalyzer    [STUB]  --> HorizonResult
    +---> SubjectDetector    [STUB]  --> SubjectResult
    +---> DepthEstimator     [STUB]  --> DepthResult
    +---> LeadingLineDetector [STUB] --> LeadingLineResult
    |
    v
VisionFrame (aggregated results + telemetry)
    |
    v
ContractSerializer (Gson, snake_case)
    |
    v
VisionEngineStream (SharedFlow<VisionEngineOutput>)
    |
    v
[Downstream modules: NOT IMPLEMENTED]
```

All stages from `CameraX` through `VisionEngineStream` execute. The four analyzers within the pipeline coordinator are structural stubs. No downstream consumers (guidance UI, template engine, capture control) exist yet.

---

## Data Contracts

The pipeline emits `VisionEngineOutput` as the inter-module communication payload. The JSON schema is enforced by `@SerializedName` annotations and validated by `ContractSerializer.validate()`.

### Vision Engine Output

Produced by: `VisionEngineStream`
Consumed by: downstream modules (planned), `MockVisionEngine` (for testing)

```json
{
  "timestamp": 1718304000000,
  "telemetry": {
    "pitch": 1.25,
    "roll": -0.50,
    "yaw": 42.10
  },
  "extracted_features": {
    "horizon_detected": true,
    "horizon_tilt_degrees": -0.5,
    "primary_subject_bounding_box": [0.33, 0.45, 0.20, 0.35]
  },
  "hardware_performance_state": {
    "target_device": "Galaxy_S24",
    "npu_load_percentage": 64.2,
    "thermal_throttling_state": "nominal"
  }
}
```

- `telemetry`: Device orientation in degrees, sourced from IMU fusion.
- `extracted_features`: Perception results. Currently populated by stubs with synthetic values.
- `primary_subject_bounding_box`: Normalized `[x, y, width, height]`.
- `hardware_performance_state`: Device tier classification and thermal status from live system monitors.

### Ghost Template Output (Planned, Not Implemented)

```json
{
  "template_identifier": "cinematic_street_01",
  "bounding_box_target": [0.30, 0.40, 0.25, 0.35],
  "required_exposure_bias": -1.0,
  "alignment_tolerance_threshold": 0.12
}
```

This contract is defined in the architecture spec but has no producing or consuming code.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI framework | Jetpack Compose (Material 3) |
| Camera | CameraX (camera2, lifecycle, view) |
| Sensors | Android `SensorManager` (`TYPE_GAME_ROTATION_VECTOR`) |
| ML runtime | TensorFlow Lite + GPU delegate (declared as dependencies; no models loaded) |
| Serialization | Gson |
| Concurrency | Kotlin Coroutines (`StateFlow`, `SharedFlow`, `coroutineScope`) |
| Permissions | Accompanist Permissions |
| Min SDK | 26 (Android 8.0) |
| Target/Compile SDK | 35 |

---

## Key Design Decisions

**CameraX over raw Camera2.** CameraX abstracts device-specific quirks (lens facing, resolution negotiation, lifecycle binding) that Camera2 requires manual handling for. The `ImageAnalysis` use-case provides backpressure control (`STRATEGY_KEEP_ONLY_LATEST`) without custom ring buffer management. Camera2 interop remains available if lower-level control is needed later.

**IMU fusion using `TYPE_GAME_ROTATION_VECTOR`.** This sensor type fuses accelerometer and gyroscope data without magnetometer input, avoiding magnetic interference drift common in indoor environments. Fallback to `TYPE_ROTATION_VECTOR` (which includes magnetometer) is provided for devices lacking a gyroscope-fused option.

**JSON-based inter-module contracts.** Modules communicate through serialized JSON payloads rather than direct Kotlin object references. This enforces a hard boundary between pipeline stages, allows independent development with mock data generators, and enables CI-time contract validation without compiling the full dependency graph.

**Frame-dropping over queuing.** The `PipelineCoordinator` uses an `AtomicBoolean` gate to silently drop incoming frames when the previous analysis cycle is still running. This prevents unbounded memory growth from queued frames and guarantees the UI always receives the most recent analysis result rather than stale data.

**Three-tier hardware classification.** Rather than continuous performance scaling, the system classifies devices into three discrete tiers at startup. This simplifies downstream branching (analyzers either run or do not) and avoids runtime oscillation between intermediate performance states. The tier can be overridden via `BuildConfig` for testing.

---

## Known Limitations

- **All perception is synthetic.** No trained ML models are integrated. The four analyzers return random or hardcoded values. Pipeline output does not reflect actual scene content.
- **No overlay rendering.** The camera preview displays raw telemetry text. No compositional wireframes, alignment indicators, or AR overlays are rendered.
- **No pose estimation.** The IMU provides device orientation (pitch/roll/yaw) but there is no visual-inertial odometry, SLAM, or 6DoF pose tracking.
- **No depth estimation.** The `DepthEstimator` stub returns a fixed byte pattern. No monocular depth model or hardware depth sensor integration exists.
- **IMU drift.** The `TYPE_GAME_ROTATION_VECTOR` sensor accumulates gyroscope drift over time. No drift correction, complementary filtering, or visual-inertial alignment is implemented.
- **No sensor-camera temporal synchronization.** IMU samples and camera frames are paired by proximity in time, not by hardware-level timestamp alignment. This may introduce jitter under high motion.
- **Device classification is heuristic.** `DeviceClassifier` matches against a limited set of known SoC model strings. Unrecognized devices default to Tier 3 (vision pipeline disabled).
- **No automated integration tests.** Unit tests cover serialization and pipeline concurrency. No instrumented tests validate camera-to-UI data flow on device.
- **Single platform.** Android only. No iOS implementation exists.

---

## How to Run

### Prerequisites

- Android Studio (Hedgehog or later recommended)
- Android SDK 35
- Physical Android device with a camera (emulator works for mock mode only)

### Build and Install

```bash
git clone <repository-url>
cd aperture
./gradlew assembleDebug
```

Install the debug APK on a connected device:

```bash
./gradlew installDebug
```

Or open the project in Android Studio and run on a connected device.

### What You Will See

**On a physical device:** A full-screen camera preview with a semi-transparent debug overlay showing live IMU telemetry (pitch, roll, yaw), the detected hardware tier, NPU load estimate, and thermal state. A floating action button toggles between live camera input and mock data mode.

**On an emulator:** The camera preview will show the emulator's virtual camera. IMU data will be static or zero. Use mock mode for meaningful telemetry output.

**What you will not see:** No compositional overlays, alignment guides, haptic feedback, or capture controls exist. The app currently functions as a camera preview with a diagnostic telemetry display.

### Running Tests

```bash
./gradlew test
```

This executes `ContractSerializerTest` and `PipelineCoordinatorStressTest`. Both are pure JVM unit tests and do not require a device.

---

## Project Structure

```
aperture/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/dev/aperture/
│       │       ├── MainActivity.kt
│       │       ├── core/
│       │       │   ├── camera/
│       │       │   │   ├── ApertureCameraManager.kt
│       │       │   │   ├── CameraState.kt
│       │       │   │   └── FrameData.kt
│       │       │   ├── sensors/
│       │       │   │   ├── ImuFusionManager.kt
│       │       │   │   └── TelemetrySnapshot.kt
│       │       │   ├── perception/
│       │       │   │   ├── Analyzer.kt              (interface)
│       │       │   │   ├── HorizonAnalyzer.kt        (stub)
│       │       │   │   ├── SubjectDetector.kt        (stub)
│       │       │   │   ├── DepthEstimator.kt         (stub)
│       │       │   │   ├── LeadingLineDetector.kt    (stub)
│       │       │   │   ├── PipelineCoordinator.kt
│       │       │   │   ├── ImagePreprocessor.kt
│       │       │   │   └── VisionFrame.kt
│       │       │   ├── hardware/
│       │       │   │   ├── HardwareTier.kt
│       │       │   │   ├── DeviceClassifier.kt
│       │       │   │   ├── PerformanceBudget.kt
│       │       │   │   └── ThermalMonitor.kt
│       │       │   ├── contract/
│       │       │   │   ├── VisionEngineOutput.kt
│       │       │   │   ├── ContractSerializer.kt
│       │       │   │   └── VisionEngineStream.kt
│       │       │   └── mock/
│       │       │       ├── MockVisionEngine.kt
│       │       │       └── SamplePayloads.kt
│       │       └── ui/
│       │           ├── CameraPreviewScreen.kt
│       │           └── theme/
│       │               └── ApertureTheme.kt
│       └── test/
│           └── java/dev/aperture/core/
│               ├── contract/
│               │   └── ContractSerializerTest.kt
│               └── perception/
│                   └── PipelineCoordinatorStressTest.kt
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/
    └── libs.versions.toml
```

---

## License

Not yet specified.
