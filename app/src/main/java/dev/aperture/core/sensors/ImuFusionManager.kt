package dev.aperture.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fuses IMU sensor data into a continuous stream of [TelemetrySnapshot] values.
 *
 * Uses [Sensor.TYPE_GAME_ROTATION_VECTOR] (gyro-fused quaternion, no magnetometer
 * drift) as the primary source. Falls back to [Sensor.TYPE_ROTATION_VECTOR] on
 * devices that lack a dedicated game rotation sensor.
 *
 * Orientation quaternions are converted to Euler angles (pitch, roll, yaw) via
 * [SensorManager.getOrientation] and emitted as degrees.
 */
class ImuFusionManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _telemetry = MutableStateFlow(TelemetrySnapshot(0L, 0f, 0f, 0f))

    /** Latest fused orientation snapshot. Updated at the sensor's native cadence. */
    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    // Pre-allocated scratch arrays — avoids per-event allocation pressure.
    private val rotationMatrix = FloatArray(ROTATION_MATRIX_SIZE)
    private val orientationAngles = FloatArray(ORIENTATION_ARRAY_SIZE)

    /**
     * Register sensor listeners. Call from `onResume()` or equivalent lifecycle hook.
     */
    fun start() {
        // Prefer TYPE_GAME_ROTATION_VECTOR (gyro-only fusion, no mag drift).
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    /**
     * Unregister sensor listeners. Call from `onPause()` or equivalent lifecycle hook.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    // ──────────────────────────────────────────────────────────────
    //  SensorEventListener
    // ──────────────────────────────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                _telemetry.value = TelemetrySnapshot(
                    timestamp = System.currentTimeMillis(),
                    pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat(),
                    roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat(),
                    yaw = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Intentionally empty — accuracy changes don't affect our fusion logic.
    }

    companion object {
        private const val ROTATION_MATRIX_SIZE = 9
        private const val ORIENTATION_ARRAY_SIZE = 3
    }
}
