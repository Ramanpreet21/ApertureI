# ApertureAI — Module 1: Core Vision Engine

Module 1 is the structural foundation of the entire app. It owns the camera lifecycle, sensor telemetry, and ML perception pipeline, and exposes a streaming JSON contract that all downstream modules consume.

## User Review Required

> [!IMPORTANT]
> **Platform choice:** The spec names the Galaxy S24 as the baseline target device, so this plan starts with **Android (Kotlin / Jetpack Compose)**. iOS (Swift/SwiftUI) is deferred to a follow-up phase. Please confirm this is the right order.

> [!IMPORTANT]
> **CameraX vs raw Camera2:** This plan uses **CameraX** (Google's recommended abstraction over Camera2). It simplifies lifecycle management, automatic device-quirk handling, and ImageAnalysis use-case binding while still exposing Camera2-level interop when needed. If you'd prefer the raw Camera2 API instead, let me know.

> [!WARNING]
> **ML models:** For the initial build, we will use **placeholder/stub models** with well-defined input/output tensor contracts. Real production models (object boundary, depth, leading-line) require a separate training or procurement effort. The architecture will be fully swappable so real `.tflite` models can be dropped in later.

## Open Questions

1. **Min SDK version** — Targeting API 26 (Android 8.0) gives broad reach; API 29+ enables some CameraX optimizations. Preference?
2. **Build system** — Standard single-module Gradle with version catalogs, or do you want a multi-module Gradle setup from day one (`:core-vision`, `:app`, etc.)?
3. **CI/CD** — The spec mentions GitHub Actions for contract validation. Should we scaffold the workflow YAML in this phase?

---

## Proposed Changes

The project will live at `/home/rs/Projects/aperture/`. Everything below is new.

---

### Phase 1 — Android Project Scaffolding

Bootstrap a Kotlin/Jetpack Compose Android project using Gradle (Kotlin DSL).

#### [NEW] `build.gradle.kts` (project-level)
- Gradle plugin declarations: AGP, Kotlin, KSP.

#### [NEW] `app/build.gradle.kts`
- Min SDK 26, Target SDK 35, Compile SDK 35.
- Dependencies:
  - `androidx.camera:camera-camera2`, `camera-lifecycle`, `camera-view` (CameraX)
  - `org.tensorflow:tensorflow-lite`, `tensorflow-lite-gpu`, `tensorflow-lite-support`
  - `androidx.compose.*` (Material 3, Activity Compose)
  - `com.google.code.gson:gson` (JSON contract serialisation)
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- BuildConfig field for `HARDWARE_TIER` override (debug builds).

#### [NEW] `settings.gradle.kts`
#### [NEW] `gradle.properties`
#### [NEW] `gradle/libs.versions.toml` — Version catalog

---

### Phase 2 — Camera Lifecycle Manager

Owns CameraX initialisation, preview surface binding, and the ImageAnalysis use-case that feeds raw frames into the perception pipeline.

#### [NEW] `app/src/main/java/dev/aperture/core/camera/CameraManager.kt`
- Binds CameraX `Preview` + `ImageAnalysis` use-cases to the `ProcessCameraProvider` lifecycle.
- Exposes a `StateFlow<CameraState>` (`Idle | Previewing | Error`).
- Configures `ImageAnalysis` for `STRATEGY_KEEP_ONLY_LATEST` at 30 fps analysis target (Tier 1).
- Resolution selection strategy: targets 1080p for analysis frames (down-scaled from sensor native).

#### [NEW] `app/src/main/java/dev/aperture/core/camera/CameraState.kt`
- Sealed interface: `Idle`, `Previewing(cameraInfo)`, `Error(throwable)`.

#### [NEW] `app/src/main/java/dev/aperture/core/camera/FrameData.kt`
- Data class wrapping `ImageProxy` metadata: timestamp, rotation, dimensions, and a lazy `Bitmap` accessor.

---

### Phase 3 — IMU Sensor Fusion

Reads accelerometer + gyroscope + magnetometer via `SensorManager` and fuses them into pitch/roll/yaw at the IMU's native frequency (~100–200 Hz), down-sampled to match the vision loop cadence.

#### [NEW] `app/src/main/java/dev/aperture/core/sensors/ImuFusionManager.kt`
- Registers `TYPE_GAME_ROTATION_VECTOR` (gyro-fused quaternion, no magnetometer drift) as primary source.
- Falls back to `TYPE_ROTATION_VECTOR` if unavailable.
- Converts quaternion → Euler angles (pitch, roll, yaw in degrees).
- Exposes `StateFlow<TelemetrySnapshot>` sampled at the current tier's vision-loop rate.

#### [NEW] `app/src/main/java/dev/aperture/core/sensors/TelemetrySnapshot.kt`
- Data class: `timestamp: Long`, `pitch: Float`, `roll: Float`, `yaw: Float`.

---

### Phase 4 — Perception Pipeline (TFLite Inference)

The ML inference layer. Each detector is a pluggable `Analyzer` behind a common interface, orchestrated by a pipeline coordinator.

#### [NEW] `app/src/main/java/dev/aperture/core/perception/Analyzer.kt`
- Interface:
  ```kotlin
  interface Analyzer<T> {
      suspend fun analyze(frame: FrameData): T
      fun close()
  }
  ```

#### [NEW] `app/src/main/java/dev/aperture/core/perception/HorizonAnalyzer.kt`
- Stub implementation detecting horizon line tilt (degrees) from an input frame.
- Loads a placeholder `.tflite` model from `assets/models/horizon_detector.tflite`.
- Output: `HorizonResult(detected: Boolean, tiltDegrees: Float)`.

#### [NEW] `app/src/main/java/dev/aperture/core/perception/SubjectDetector.kt`
- Stub for primary subject bounding-box detection.
- Output: `SubjectResult(boundingBox: RectF?)` — normalized `[x, y, w, h]`.

#### [NEW] `app/src/main/java/dev/aperture/core/perception/DepthEstimator.kt`
- Stub for monocular depth estimation producing a coarse foreground/background mask.
- Output: `DepthResult(foregroundMask: ByteArray, depthConfidence: Float)`.

#### [NEW] `app/src/main/java/dev/aperture/core/perception/LeadingLineDetector.kt`
- Stub for dominant leading-line extraction.
- Output: `LeadingLineResult(lines: List<Line>)` where `Line(startX, startY, endX, endY, confidence)`.

#### [NEW] `app/src/main/java/dev/aperture/core/perception/PipelineCoordinator.kt`
- Orchestrates all analyzers in parallel using `coroutineScope { async {} }`.
- Merges results into a single `VisionFrame` output.
- Respects the tier's fps budget — drops frames if the previous analysis is still in-flight.

#### [NEW] `app/src/main/java/dev/aperture/core/perception/VisionFrame.kt`
- Aggregate data class holding `HorizonResult`, `SubjectResult`, `DepthResult`, `LeadingLineResult`, plus the originating `TelemetrySnapshot`.

#### [NEW] `app/src/main/assets/models/` (placeholder `.tflite` files)
- Minimal valid TFLite flatbuffer stubs so the loader doesn't crash.

---

### Phase 5 — Hardware Tier Classification & Dynamic Scaling

Detects device capabilities at startup and adjusts the pipeline's frame analysis rate and model complexity accordingly.

#### [NEW] `app/src/main/java/dev/aperture/core/hardware/HardwareTier.kt`
- Enum: `TIER_1_FULL`, `TIER_2_BALANCED`, `TIER_3_MINIMAL`.

#### [NEW] `app/src/main/java/dev/aperture/core/hardware/DeviceClassifier.kt`
- Reads `Build.MODEL`, `Build.SOC_MODEL` (API 31+), `ActivityManager.memoryInfo`, and GPU renderer string.
- Classifies into a `HardwareTier`.
- Allows debug override via `BuildConfig.HARDWARE_TIER_OVERRIDE`.

#### [NEW] `app/src/main/java/dev/aperture/core/hardware/PerformanceBudget.kt`
- Maps tier → concrete numbers:
  | | Tier 1 | Tier 2 | Tier 3 |
  |---|---|---|---|
  | Vision loop FPS | 30 | 15 | 0 (disabled) |
  | Max model memory | 500 MB | 350 MB | — |
  | NPU delegation | Enabled | Enabled | — |

#### [NEW] `app/src/main/java/dev/aperture/core/hardware/ThermalMonitor.kt`
- Listens to `PowerManager.THERMAL_STATUS_*` callbacks (API 29+).
- Dynamically downgrades pipeline when thermal throttling is detected.

---

### Phase 6 — JSON Contract Emission & Inter-Module API

Serializes the `VisionFrame` + telemetry into the exact JSON schema defined in Section 4 of the spec and exposes it as a reactive stream for downstream modules.

#### [NEW] `app/src/main/java/dev/aperture/core/contract/VisionEngineOutput.kt`
- Data class matching the spec's JSON contract:
  ```kotlin
  data class VisionEngineOutput(
      val timestamp: Long,
      val telemetry: Telemetry,
      val extractedFeatures: ExtractedFeatures,
      val hardwarePerformanceState: HardwarePerformanceState
  )
  ```

#### [NEW] `app/src/main/java/dev/aperture/core/contract/ContractSerializer.kt`
- `Gson`-based serializer with `@SerializedName` annotations enforcing snake_case keys.
- Includes a `validate()` method that asserts required fields are present (used in CI).

#### [NEW] `app/src/main/java/dev/aperture/core/contract/VisionEngineStream.kt`
- Wraps the pipeline output as a `SharedFlow<VisionEngineOutput>`.
- This is the single public API surface that Modules 2, 3, and 4 subscribe to.

---

### Phase 7 — Mock Data Generator & Minimal UI Shell

Per the spec: *"Every module must ship with an internal mock data generator."*

#### [NEW] `app/src/main/java/dev/aperture/core/mock/MockVisionEngine.kt`
- Emits a looping sequence of pre-baked `VisionEngineOutput` JSON payloads at a configurable frame rate.
- Useful for downstream module development without camera/NPU hardware.

#### [NEW] `app/src/main/java/dev/aperture/core/mock/SamplePayloads.kt`
- 5–10 representative JSON payloads covering: level horizon, tilted horizon, centered subject, off-center subject, thermal throttling state, etc.

#### [NEW] `app/src/main/java/dev/aperture/ui/CameraPreviewScreen.kt`
- Minimal Jetpack Compose screen:
  - Full-bleed camera preview (`CameraX PreviewView`).
  - Semi-transparent debug overlay showing live telemetry (pitch/roll/yaw) and current hardware tier.
  - Toggle button to switch between live camera and mock data mode.

#### [NEW] `app/src/main/java/dev/aperture/ui/theme/ApertureTheme.kt`
- Material 3 dark theme with the spec's muted amber / emerald accent colors.

#### [NEW] `app/src/main/java/dev/aperture/MainActivity.kt`
- Entry point. Handles camera permission requests, initialises DI, and hosts `CameraPreviewScreen`.

#### [NEW] `app/src/main/AndroidManifest.xml`
- Permissions: `CAMERA`, `HIGH_REFRESH_RATE_DISPLAY` (optional).
- Feature declaration: `android.hardware.camera`.

---

## File Tree Summary

```
aperture/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/models/
        │   └── (placeholder .tflite stubs)
        ├── java/dev/aperture/
        │   ├── MainActivity.kt
        │   ├── core/
        │   │   ├── camera/
        │   │   │   ├── CameraManager.kt
        │   │   │   ├── CameraState.kt
        │   │   │   └── FrameData.kt
        │   │   ├── sensors/
        │   │   │   ├── ImuFusionManager.kt
        │   │   │   └── TelemetrySnapshot.kt
        │   │   ├── perception/
        │   │   │   ├── Analyzer.kt
        │   │   │   ├── HorizonAnalyzer.kt
        │   │   │   ├── SubjectDetector.kt
        │   │   │   ├── DepthEstimator.kt
        │   │   │   ├── LeadingLineDetector.kt
        │   │   │   ├── PipelineCoordinator.kt
        │   │   │   └── VisionFrame.kt
        │   │   ├── hardware/
        │   │   │   ├── HardwareTier.kt
        │   │   │   ├── DeviceClassifier.kt
        │   │   │   ├── PerformanceBudget.kt
        │   │   │   └── ThermalMonitor.kt
        │   │   ├── contract/
        │   │   │   ├── VisionEngineOutput.kt
        │   │   │   ├── ContractSerializer.kt
        │   │   │   └── VisionEngineStream.kt
        │   │   └── mock/
        │   │       ├── MockVisionEngine.kt
        │   │       └── SamplePayloads.kt
        │   └── ui/
        │       ├── CameraPreviewScreen.kt
        │       └── theme/
        │           └── ApertureTheme.kt
        └── res/
            └── values/
                └── strings.xml
```

---

## Verification Plan

### Automated Tests

1. **Unit tests** for each analyzer stub — verify they return well-formed output from a synthetic `FrameData`.
2. **Unit tests** for `ContractSerializer` — roundtrip serialize/deserialize and validate against the spec's JSON schema.
3. **Unit tests** for `DeviceClassifier` — mock `Build` fields and assert correct tier assignment.
4. **Unit tests** for `ImuFusionManager` — feed synthetic sensor events and verify Euler angle math.
5. **`./gradlew assembleDebug`** — must pass cleanly with zero warnings.

### Manual Verification

1. **Install on a physical device (or emulator):** Camera preview renders at 60 fps, debug overlay shows live telemetry.
2. **Mock mode toggle:** Switching to mock data shows cycling payloads in the debug overlay without camera access.
3. **Thermal simulation:** Force a tier downgrade via `BuildConfig` override and verify the vision loop rate adjusts.
