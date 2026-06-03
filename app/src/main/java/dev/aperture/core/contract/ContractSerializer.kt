package dev.aperture.core.contract

import android.os.Build
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dev.aperture.core.hardware.ThermalState
import dev.aperture.core.perception.VisionFrame

/**
 * Serializes [VisionFrame] into the inter-module [VisionEngineOutput] JSON contract
 * and provides validation utilities for CI enforcement.
 */
object ContractSerializer {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create()

    /**
     * Convert an internal [VisionFrame] into the public [VisionEngineOutput] contract.
     *
     * @param frame         The merged perception pipeline output.
     * @param thermalState  Current device thermal status.
     * @param npuLoad       Estimated NPU utilisation percentage (0–100).
     */
    fun fromVisionFrame(
        frame: VisionFrame,
        thermalState: ThermalState,
        npuLoad: Float = 0f
    ): VisionEngineOutput {
        val boundingBox = frame.subject.boundingBox?.let {
            listOf(it.left, it.top, it.width(), it.height())
        }

        return VisionEngineOutput(
            timestamp = frame.timestamp,
            telemetry = Telemetry(
                pitch = frame.telemetry.pitch,
                roll = frame.telemetry.roll,
                yaw = frame.telemetry.yaw
            ),
            extractedFeatures = ExtractedFeatures(
                horizonDetected = frame.horizon.detected,
                horizonTiltDegrees = frame.horizon.tiltDegrees,
                primarySubjectBoundingBox = boundingBox
            ),
            hardwarePerformanceState = HardwarePerformanceState(
                targetDevice = Build.MODEL.replace(" ", "_"),
                npuLoadPercentage = npuLoad,
                thermalThrottlingState = thermalState.name.lowercase()
            )
        )
    }

    /**
     * Serialize a [VisionEngineOutput] to its JSON string representation.
     */
    fun toJson(output: VisionEngineOutput): String {
        return gson.toJson(output)
    }

    /**
     * Deserialize a JSON string back into a [VisionEngineOutput].
     *
     * @throws com.google.gson.JsonSyntaxException if the JSON is malformed.
     */
    fun fromJson(json: String): VisionEngineOutput {
        return gson.fromJson(json, VisionEngineOutput::class.java)
    }

    /**
     * Validate that a [VisionEngineOutput] conforms to the required contract shape.
     *
     * Used in CI/CD pipeline for commit-time schema enforcement.
     *
     * @return A list of validation error messages, empty if valid.
     */
    fun validate(output: VisionEngineOutput): List<String> {
        val errors = mutableListOf<String>()

        if (output.timestamp <= 0) {
            errors.add("timestamp must be a positive epoch-millis value")
        }

        output.extractedFeatures.primarySubjectBoundingBox?.let { bbox ->
            if (bbox.size != BOUNDING_BOX_SIZE) {
                errors.add("primary_subject_bounding_box must have exactly 4 elements [x, y, w, h]")
            }
            if (bbox.any { it < 0f || it > 1f }) {
                errors.add("bounding box values must be normalised to [0, 1]")
            }
        }

        if (output.hardwarePerformanceState.npuLoadPercentage < 0f ||
            output.hardwarePerformanceState.npuLoadPercentage > MAX_NPU_LOAD
        ) {
            errors.add("npu_load_percentage must be in [0, 100]")
        }

        return errors
    }

    /**
     * Convenience: validate and throw if invalid.
     */
    fun validateOrThrow(output: VisionEngineOutput) {
        val errors = validate(output)
        if (errors.isNotEmpty()) {
            throw IllegalStateException(
                "Contract validation failed:\n${errors.joinToString("\n  • ", prefix = "  • ")}"
            )
        }
    }

    private const val BOUNDING_BOX_SIZE = 4
    private const val MAX_NPU_LOAD = 100f
}
