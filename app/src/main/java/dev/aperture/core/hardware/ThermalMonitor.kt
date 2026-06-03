package dev.aperture.core.hardware

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors the device's thermal state and dynamically downgrades the active
 * [PerformanceBudget] when thermal throttling is detected.
 *
 * Uses [PowerManager.OnThermalStatusChangedListener] (API 29+) to receive
 * real-time thermal status callbacks from the OS. On older devices, the
 * thermal state defaults to "nominal" (no throttling).
 *
 * Thermal states mapped to pipeline behaviour:
 * - **NOMINAL / LIGHT:** No change — maintain current tier budget.
 * - **MODERATE:** Downgrade one tier (e.g. Tier 1 → Tier 2 budget).
 * - **SEVERE / CRITICAL / EMERGENCY / SHUTDOWN:** Disable vision pipeline entirely.
 */
class ThermalMonitor(context: Context) {

    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val _thermalState = MutableStateFlow(ThermalState.NOMINAL)

    /** Current thermal state. Updated by OS callbacks. */
    val thermalState: StateFlow<ThermalState> = _thermalState.asStateFlow()

    private var listener: Any? = null  // Typed as Any to avoid API 29 class ref on older devices.

    /**
     * Start listening for thermal status changes.
     */
    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                _thermalState.value = mapThermalStatus(status)
            }
            powerManager.addThermalStatusListener(thermalListener)
            listener = thermalListener
        }
    }

    /**
     * Stop listening for thermal status changes.
     */
    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (listener as? PowerManager.OnThermalStatusChangedListener)?.let {
                powerManager.removeThermalStatusListener(it)
            }
            listener = null
        }
    }

    /**
     * Given the [baseTier], return the effective tier after applying thermal
     * throttling adjustments.
     */
    fun effectiveTier(baseTier: HardwareTier): HardwareTier {
        return when (_thermalState.value) {
            ThermalState.NOMINAL,
            ThermalState.LIGHT -> baseTier

            ThermalState.MODERATE -> when (baseTier) {
                HardwareTier.TIER_1_FULL -> HardwareTier.TIER_2_BALANCED
                else -> baseTier
            }

            ThermalState.SEVERE,
            ThermalState.CRITICAL -> HardwareTier.TIER_3_MINIMAL
        }
    }

    private fun mapThermalStatus(status: Int): ThermalState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalState.NOMINAL
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE,
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.NOMINAL

            PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE

            PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE

            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL

            else -> ThermalState.NOMINAL
        }
    }
}

/**
 * Simplified thermal state categories for pipeline decision-making.
 */
enum class ThermalState {
    NOMINAL,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL
}
