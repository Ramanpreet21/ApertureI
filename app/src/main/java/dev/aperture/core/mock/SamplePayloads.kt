package dev.aperture.core.mock

/**
 * Pre-baked JSON payloads representing diverse scene conditions.
 *
 * These match the inter-module contract defined in the spec (Section 4) and
 * cover edge cases that downstream modules must handle gracefully.
 */
object SamplePayloads {

    /** Returns all sample payloads as raw JSON strings. */
    fun all(): List<String> = listOf(
        levelHorizonCenteredSubject(),
        tiltedHorizonOffCenterSubject(),
        noSubjectDetected(),
        heavyNpuLoad(),
        thermalThrottling(),
        wideAngleLandscape(),
        portraitCloseUp(),
        lowLightScene()
    )

    /** Clean shot: level horizon, subject at rule-of-thirds intersection. */
    fun levelHorizonCenteredSubject(): String = """
    {
      "timestamp": 1718304000000,
      "telemetry": {
        "pitch": 0.50,
        "roll": -0.20,
        "yaw": 180.00
      },
      "extracted_features": {
        "horizon_detected": true,
        "horizon_tilt_degrees": -0.2,
        "primary_subject_bounding_box": [0.33, 0.45, 0.20, 0.35]
      },
      "hardware_performance_state": {
        "target_device": "Galaxy_S24",
        "npu_load_percentage": 42.5,
        "thermal_throttling_state": "nominal"
      }
    }
    """.trimIndent()

    /** Tilted horizon with subject shifted to the left third. */
    fun tiltedHorizonOffCenterSubject(): String = """
    {
      "timestamp": 1718304001000,
      "telemetry": {
        "pitch": 1.25,
        "roll": -3.80,
        "yaw": 42.10
      },
      "extracted_features": {
        "horizon_detected": true,
        "horizon_tilt_degrees": -3.5,
        "primary_subject_bounding_box": [0.10, 0.30, 0.25, 0.50]
      },
      "hardware_performance_state": {
        "target_device": "Galaxy_S24",
        "npu_load_percentage": 55.0,
        "thermal_throttling_state": "nominal"
      }
    }
    """.trimIndent()

    /** No primary subject detected — empty landscape or abstract scene. */
    fun noSubjectDetected(): String = """
    {
      "timestamp": 1718304002000,
      "telemetry": {
        "pitch": -0.10,
        "roll": 0.05,
        "yaw": 90.00
      },
      "extracted_features": {
        "horizon_detected": true,
        "horizon_tilt_degrees": 0.1,
        "primary_subject_bounding_box": null
      },
      "hardware_performance_state": {
        "target_device": "Galaxy_S24",
        "npu_load_percentage": 30.0,
        "thermal_throttling_state": "nominal"
      }
    }
    """.trimIndent()

    /** High NPU load — stress-testing performance budget logic. */
    fun heavyNpuLoad(): String = """
    {
      "timestamp": 1718304003000,
      "telemetry": {
        "pitch": 2.00,
        "roll": -1.00,
        "yaw": 270.00
      },
      "extracted_features": {
        "horizon_detected": true,
        "horizon_tilt_degrees": -0.8,
        "primary_subject_bounding_box": [0.40, 0.35, 0.20, 0.30]
      },
      "hardware_performance_state": {
        "target_device": "Galaxy_S24",
        "npu_load_percentage": 92.7,
        "thermal_throttling_state": "nominal"
      }
    }
    """.trimIndent()

    /** Device under thermal throttling — moderate state. */
    fun thermalThrottling(): String = """
    {
      "timestamp": 1718304004000,
      "telemetry": {
        "pitch": 0.30,
        "roll": 0.10,
        "yaw": 135.00
      },
      "extracted_features": {
        "horizon_detected": true,
        "horizon_tilt_degrees": 0.3,
        "primary_subject_bounding_box": [0.25, 0.40, 0.30, 0.40]
      },
      "hardware_performance_state": {
        "target_device": "Galaxy_S24",
        "npu_load_percentage": 64.2,
        "thermal_throttling_state": "moderate"
      }
    }
    """.trimIndent()

    /** Wide-angle landscape with distant horizon. */
    fun wideAngleLandscape(): String = """
    {
      "timestamp": 1718304005000,
      "telemetry": {
        "pitch": -5.00,
        "roll": 0.00,
        "yaw": 0.00
      },
      "extracted_features": {
        "horizon_detected": true,
        "horizon_tilt_degrees": 0.0,
        "primary_subject_bounding_box": [0.15, 0.60, 0.70, 0.30]
      },
      "hardware_performance_state": {
        "target_device": "Galaxy_S24",
        "npu_load_percentage": 38.0,
        "thermal_throttling_state": "nominal"
      }
    }
    """.trimIndent()

    /** Portrait close-up — large subject, no horizon. */
    fun portraitCloseUp(): String = """
    {
      "timestamp": 1718304006000,
      "telemetry": {
        "pitch": 3.50,
        "roll": -0.50,
        "yaw": 180.00
      },
      "extracted_features": {
        "horizon_detected": false,
        "horizon_tilt_degrees": 0.0,
        "primary_subject_bounding_box": [0.20, 0.10, 0.60, 0.80]
      },
      "hardware_performance_state": {
        "target_device": "Galaxy_S24",
        "npu_load_percentage": 58.3,
        "thermal_throttling_state": "nominal"
      }
    }
    """.trimIndent()

    /** Low-light scene — lower confidence, noisy telemetry. */
    fun lowLightScene(): String = """
    {
      "timestamp": 1718304007000,
      "telemetry": {
        "pitch": 0.80,
        "roll": -2.10,
        "yaw": 315.00
      },
      "extracted_features": {
        "horizon_detected": false,
        "horizon_tilt_degrees": 0.0,
        "primary_subject_bounding_box": [0.30, 0.35, 0.25, 0.40]
      },
      "hardware_performance_state": {
        "target_device": "Galaxy_S24",
        "npu_load_percentage": 71.5,
        "thermal_throttling_state": "light"
      }
    }
    """.trimIndent()
}
