package dev.aperture.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.aperture.R
import dev.aperture.core.contract.VisionEngineOutput
import dev.aperture.core.hardware.HardwareTier
import dev.aperture.ui.theme.*

@Composable
fun CameraPreviewScreen(
    latestOutput: VisionEngineOutput?,
    hardwareTier: HardwareTier,
    isMockMode: Boolean,
    onToggleMockMode: () -> Unit,
    onPreviewViewReady: (PreviewView) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also {
                    it.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    it.scaleType = PreviewView.ScaleType.FILL_CENTER
                    onPreviewViewReady(it)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TelemetryHud(output = latestOutput, tier = hardwareTier)
            Spacer(modifier = Modifier.weight(1f))
            ModeToggleBar(isMockMode = isMockMode, onToggle = onToggleMockMode)
        }
    }
}

@Composable
private fun TelemetryHud(output: VisionEngineOutput?, tier: HardwareTier) {
    Surface(color = ApertureSurfaceOverlay, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.debug_overlay_title), style = MaterialTheme.typography.titleMedium, color = ApertureOnSurface)
                TierBadge(tier)
            }
            Spacer(Modifier.height(8.dp))
            if (output != null) {
                TelemetryRow("PITCH", "%.2f°".format(output.telemetry.pitch))
                TelemetryRow("ROLL", "%.2f°".format(output.telemetry.roll))
                TelemetryRow("YAW", "%.2f°".format(output.telemetry.yaw))
                Spacer(Modifier.height(6.dp))
                val hColor by animateColorAsState(
                    if (output.extractedFeatures.horizonDetected && kotlin.math.abs(output.extractedFeatures.horizonTiltDegrees) < 1f) ApertureEmerald else ApertureAmber,
                    tween(300), label = "hc"
                )
                TelemetryRow("HORIZON", if (output.extractedFeatures.horizonDetected) "%.1f°".format(output.extractedFeatures.horizonTiltDegrees) else "—", hColor)
                val bbox = output.extractedFeatures.primarySubjectBoundingBox
                TelemetryRow("SUBJECT", bbox?.let { "[%.2f, %.2f, %.2f, %.2f]".format(it[0], it[1], it[2], it[3]) } ?: "none")
                Spacer(Modifier.height(6.dp))
                TelemetryRow("NPU", "%.1f%%".format(output.hardwarePerformanceState.npuLoadPercentage))
                TelemetryRow("THERMAL", output.hardwarePerformanceState.thermalThrottlingState.uppercase(),
                    when (output.hardwarePerformanceState.thermalThrottlingState) { "nominal" -> ApertureEmerald; "moderate" -> ApertureAmber; else -> Color(0xFFEF4444) })
            } else {
                Text("Waiting for pipeline…", style = MaterialTheme.typography.labelMedium, color = ApertureOnSurfaceDim)
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, valueColor: Color = ApertureOnSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ApertureOnSurfaceDim)
        Text(value, style = MaterialTheme.typography.labelMedium, color = valueColor)
    }
}

@Composable
private fun TierBadge(tier: HardwareTier) {
    val (text, color) = when (tier) { HardwareTier.TIER_1_FULL -> "T1" to ApertureEmerald; HardwareTier.TIER_2_BALANCED -> "T2" to ApertureAmber; HardwareTier.TIER_3_MINIMAL -> "T3" to Color(0xFFEF4444) }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = color)
    }
}

@Composable
private fun ModeToggleBar(isMockMode: Boolean, onToggle: () -> Unit) {
    Surface(color = ApertureSurfaceOverlay, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            val liveC by animateColorAsState(if (!isMockMode) ApertureEmerald else ApertureOnSurfaceDim, tween(200), label = "lc")
            val mockC by animateColorAsState(if (isMockMode) ApertureAmber else ApertureOnSurfaceDim, tween(200), label = "mc")
            TextButton(onClick = { if (isMockMode) onToggle() }) { Text(stringResource(R.string.mode_live), color = liveC, style = MaterialTheme.typography.titleMedium) }
            Box(Modifier.padding(horizontal = 12.dp).size(4.dp).clip(CircleShape).background(ApertureOnSurfaceDim))
            TextButton(onClick = { if (!isMockMode) onToggle() }) { Text(stringResource(R.string.mode_mock), color = mockC, style = MaterialTheme.typography.titleMedium) }
        }
    }
}
