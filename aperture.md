Here is the complete, finalized architecture and execution plan for **ApertureAI**. This document integrates all structural refinements, platform-native guidelines, performance constraints, and specific optimization targets for the focus hardware.

---

# ApertureAI: Project Architecture & Execution Master Document

## 1. Project Overview & Core Scope

ApertureAI is a modular, AI-assisted mobile camera application designed to function as a real-time composition coach. By parsing local computer vision data and device telemetry, the system guides users into optimal framing using minimalist spatial overlays and physical feedback.

### Engineering & Product Guardrails

* **Platform Native Over Dogmatic Flow:** Development strictly respects native ecosystems. No specific terminal-only text editors or niche shell environments are mandated. iOS engineering lives natively in Xcode (`Swift`/`SwiftUI`), and Android engineering lives natively in Android Studio (`Kotlin`/`Jetpack Compose`).
* **Assistance, Not Replacement:** The creative, decisive moment belongs entirely to the photographer. Algorithms assist with stability and geometric alignment, but the user retains ultimate control over the shutter unless they explicitly opt into the automated assist mode.
* **Local, Privacy-First Architecture:** To ensure sub-100ms latency and absolute user privacy, all scene analysis, semantic segmentation, and user preference loops run strictly on-device. No visual assets or raw pixel streams are transmitted to external servers.

---

## 2. Hardware Target Tiers & Performance Budgets

To guarantee stable battery longevity and prevent thermal throttling, the vision pipeline dynamically scales its compute footprint based on the host device's hardware tier, using the **Galaxy S24** platform as the baseline development target:

| Tier | Hardware Profiles | Target Performance | Pipeline Behavioral State |
| --- | --- | --- | --- |
| **Tier 1 (Target)** | **Galaxy S24 Series**, iPhone 15 Pro, OnePlus 12 (Snapdragon 8 Gen 3 / Exynos 2400, $\ge$ 8GB RAM) | 60fps viewfinder view,<br>

<br>30fps vision loop | **Full Performance:** Real-time semantic segmentation, dynamic depth mapping, and vector alignment calculations optimized for local NPU execution. |
| **Tier 2 (Supported)** | Galaxy S22, iPhone 13 | 60fps viewfinder view,<br>

<br>15fps vision loop | **Balanced Performance:** Reduced semantic segmentation sampling rate; heavily utilizes cached depth maps during camera panning to reduce CPU load. |
| **Tier 3 (Minimal)** | iPhone 11, Galaxy A52 | 60fps viewfinder view,<br>

<br>0fps vision loop | **Vision Pipeline Disabled:** Highly complex AI models are bypassed. The application functions strictly via static geometric UI grids and manual exposure control. |

---

## 3. Modular System Breakdown

```
 ┌────────────────────────────────────────────────────────┐
 │            MODULE 1: CORE VISION ENGINE                │
 │   (AVFoundation / Camera2 API ──► Sensor IMU Fusion)   │
 └───────────────────────────┬────────────────────────────┘
                             │
                             ▼ [Raw Data Stream via JSON Contract]
 ┌────────────────────────────────────────────────────────┐
 │            MODULE 3: ACTIVE GUIDANCE & UI              │
 │  (Strictness Slider ──► Rule Break ──► Haptic Engine)  │
 └─────────────┬────────────────────────────▲─────────────┘
               │                            │
               ▼                            │ [Template Metrics]
 ┌───────────────────────────┐    ┌─────────┴─────────────┐
 │    MODULE 4: CAPTURE      │    │   MODULE 2: GHOST     │
 │    & SKILL PROGRESSION    │    │   TEMPLATE ENGINE     │
 │  (Manual vs. Smart Shut)  │    │ (Luminance/Geometry)  │
 └─────────────┬─────────────┘    └───────────────────────┘
               │
               ▼ [Captured Image Asset]
 ┌────────────────────────────────────────────────────────┐
 │         MODULE 5: POST-CAPTURE & LEARNING LOOP         │
 │   (5A: Deterministic Review  ──► 5B: Taste Daemon)     │
 └────────────────────────────────────────────────────────┘

```

### Module 1: The Core Vision Engine (The Structural Base)

* **Platform Ingestion:** Manages native camera lifecycles (`AVFoundation` for iOS, `Camera2 API` for Android) targeting an uncompressed, raw frame feed.
* **Sensor Fusion:** Tracks hardware pitch, roll, and yaw by interfacing directly with the device's high-frequency Inertial Measurement Unit (IMU).
* **Perception Pipeline:** Executes quantized, local machine learning models (TensorFlow Lite / CoreML) to evaluate object boundary maps, isolate foreground/background depth planes (Z-axis), and track dominant leading lines.
* **Galaxy S24 Hardware Profiling:** Vision models must be quantized to INT8/FP16 formats to leverage the device's native NPU. The entire execution footprint must remain strictly within a **500MB runtime memory budget** to preserve OS performance metrics.
* *Risk Buffer Note:* The initial 3 weeks of development are strictly walled off for managing raw camera hardware permissions, frame open-latencies, and baseline thermal profiling. No UI features are to be layered on top until frame drops hit 0% under continuous testing.

### Module 2: The Ghost Template Engine

* **Deterministic Feature Extraction:** When a reference image is parsed, the engine breaks down the file into three flat, mathematical data layers:
1. *Geometric Array:* Normalized screen coordinate bounds $(X, Y)$ for primary subjects and intersection points.
2. *Luminance Map:* A low-resolution histogram grid tracking light and shadow distribution (e.g., distinguishing high-contrast scenes from flat overcast profiles).
3. *Depth Layering:* Structural estimates separating foreground silhouettes from background backdrops.


* **Ingestion Mechanics:**
* *System Photo Picker:* A clean, standard asset picker to easily ingest screenshots, movie frames, or reference compositions directly from the local gallery.
* *Quick-Capture Loop:* A rapid-action utility that allows the user to point the device at any real-world environment, tap a button, and instantly capture its active spatial composition layout to use as a dynamic wireframe for future shots.



### Module 3: Active Guidance & UI

* **Quiet Luxury Design Language:** Hides computational complexity behind an understated user experience. Displays ultra-thin, high-contrast structural wireframes (e.g., Vanishing Point lines, Golden Ratio spirals). Lines use a muted, translucent amber tone during adjustment and snap to a crisp emerald green the millisecond framing metrics converge.
* **Adjustable Strictness Slider:** A low-profile UI slider control allowing users to dial alignment tolerances anywhere from "Strict Match" (requiring exact, pixel-perfect alignment) down to "Gentle Suggestion" (broad, accommodating bounding regions).
* **Haptic Engine Translation:** Converts geometric alignment errors into intuitive physical feedback using the device's precision vibration motors (e.g., a localized pulse on the left bezel means "pan left").
* **Rule Break Detection:** If a photographer holds a composition outside the recommended template guidelines intentionally for more than 2.0 seconds, the engine notes the deliberate artistic choice. The UI transitions smoothly into a "Creative Deviation" state, instantly silencing corrective haptic notifications.

### Module 4: Capture & Skill Progression Control

* **User Progression Tiers:**
* *Beginner:* Full assistance mode. The Smart Shutter option automatically fires the camera at the exact millisecond device micro-shake approaches zero and geometric composition aligns with the active template.
* *Intermediate:* Telemetry overlays, alignment indicators, and haptic nudges remain active, but the final shutter execution must be triggered manually by the user.
* *Advanced:* The AI assistant steps back entirely, rendering only minimalist geometric wireframe outlines. All automated exposure formulas, stabilization locks, and smart shutters are bypassed.



### Module 5: Post-Capture & Learning Loop

* **Sub-Module 5A: Post-Capture Review (Deterministic)**
* Launches a localized side-by-side comparison screen pairing the target Ghost Template with the user's final captured asset.
* Generates a deterministic data card summarizing which geometric principles were successfully executed (e.g., *"Subject placed at Golden Ratio intersection; Leading line aligned at 12°"*). This relies strictly on frame parameters calculated during capture—**no external generative text or cloud LLMs are utilized.**


* **Sub-Module 5B: Taste Learning (Statistical Preference Daemon)**
* Avoids complex or battery-draining on-device retraining loops. Instead, it relies on a lightweight **weighted feature histogram**.
* This background utility monitors user preference signals (tracking which images are exported or favorited versus those instantly deleted). If the data consistently reveals that the user prefers to place their primary subject 15% further to the left margin than a standard template suggests, the daemon shifts the baseline alignment target vectors to reflect that specific aesthetic style over generic rules.



---

## 4. Inter-Module API Contracts

To facilitate independent, parallel modular engineering workflows, communication between segments must comply with these precise local JSON payloads. Contract validation is enforced during repository commits using shared CI/CD pipelines (such as GitHub Actions).

### Vision Engine (Mod 1) $\rightarrow$ Guidance UI (Mod 3)

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

### Ghost Template (Mod 2) $\rightarrow$ Capture Control (Mod 4)

```json
{
  "template_identifier": "cinematic_street_01",
  "bounding_box_target": [0.30, 0.40, 0.25, 0.35],
  "required_exposure_bias": -1.0,
  "alignment_tolerance_threshold": 0.12
}

```

---

## 5. Development Verification & Guidelines

* **IDE Autonomy:** Developers are expected to manage compilation and linting via their platform's primary developer utilities. Build commands must execute cleanly through command-line utilities (`xcodebuild` / `gradlew`) to guarantee clean execution inside CI/CD automation boxes.
* **Component-Level Isolation:** Every module must ship with a internal mock data generator asset. The interface development team must be capable of thoroughly validating haptic pulses and slider dynamics by piping static JSON arrays through a simulation container without needing live camera hardware or NPU models running.

---

## 6. Explicitly Out of Scope

To ensure strict scope boundaries and keep development timelines achievable, the following features are explicitly deferred:

1. Cloud hosting infrastructure, remote user account databases, and cross-device profile syncing.
2. Shared public template marketplaces, social profiles, or interactive community asset discovery layers.
3. Video recording functionality, multi-frame temporal alignment tracking, and slow-motion video optimizations.
4. Persistent Augmented Reality (AR) spatial anchors or persistent point-cloud world reconstructions.