package dev.aperture.core.camera

import androidx.camera.core.CameraInfo

/**
 * Represents the current state of the camera lifecycle.
 *
 * Exposed as a [kotlinx.coroutines.flow.StateFlow] by [ApertureCameraManager]
 * so that the UI and pipeline coordinator can react to camera availability.
 */
sealed interface CameraState {

    /** Camera is not bound to any lifecycle. */
    data object Idle : CameraState

    /** Camera preview and analysis use-cases are active. */
    data class Previewing(val cameraInfo: CameraInfo) : CameraState

    /** An unrecoverable error occurred during camera initialisation. */
    data class Error(val throwable: Throwable) : CameraState
}
