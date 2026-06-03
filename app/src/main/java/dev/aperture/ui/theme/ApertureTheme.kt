package dev.aperture.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ──────────────────────────────────────────────────────────────
//  Aperture Design Tokens
//  Spec: "Quiet Luxury Design Language"
//  - Muted amber during adjustment
//  - Crisp emerald on convergence
//  - Ultra-thin, high-contrast structural wireframes
// ──────────────────────────────────────────────────────────────

/** Muted, translucent amber — used during alignment adjustment. */
val ApertureAmber = Color(0xFFD4A853)
val ApertureAmberMuted = Color(0x99D4A853)

/** Crisp emerald — snaps on when framing metrics converge. */
val ApertureEmerald = Color(0xFF34D399)
val ApertureEmeraldBright = Color(0xFF10B981)

/** Surface and background tones — deep, cinematic blacks. */
val ApertureSurface = Color(0xFF0A0A0F)
val ApertureSurfaceElevated = Color(0xFF14141A)
val ApertureSurfaceOverlay = Color(0xCC000000)

/** On-surface text — slightly warm white for readability over dark surfaces. */
val ApertureOnSurface = Color(0xFFF0EDE6)
val ApertureOnSurfaceDim = Color(0x99F0EDE6)

/** Error / warning accent. */
val ApertureError = Color(0xFFEF4444)

private val ApertureDarkColorScheme = darkColorScheme(
    primary = ApertureEmerald,
    secondary = ApertureAmber,
    tertiary = ApertureAmberMuted,
    background = ApertureSurface,
    surface = ApertureSurfaceElevated,
    onPrimary = ApertureSurface,
    onSecondary = ApertureSurface,
    onBackground = ApertureOnSurface,
    onSurface = ApertureOnSurface,
    error = ApertureError,
    onError = ApertureOnSurface
)

private val ApertureTypography = Typography(
    // Monospaced telemetry readouts
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
        color = ApertureOnSurfaceDim
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    // UI labels
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    )
)

@Composable
fun ApertureTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ApertureDarkColorScheme,
        typography = ApertureTypography,
        content = content
    )
}
