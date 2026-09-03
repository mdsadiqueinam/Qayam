package tech.sadique.qayam.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.ui.theme.GoldAccent
import tech.sadique.qayam.ui.theme.GoldLight
import tech.sadique.qayam.ui.theme.SkyAsrEnd
import tech.sadique.qayam.ui.theme.SkyAsrMid
import tech.sadique.qayam.ui.theme.SkyAsrStart
import tech.sadique.qayam.ui.theme.SkyDhuhrEnd
import tech.sadique.qayam.ui.theme.SkyDhuhrMid
import tech.sadique.qayam.ui.theme.SkyDhuhrStart
import tech.sadique.qayam.ui.theme.SkyFajrEnd
import tech.sadique.qayam.ui.theme.SkyFajrMid
import tech.sadique.qayam.ui.theme.SkyFajrStart
import tech.sadique.qayam.ui.theme.SkyIshaEnd
import tech.sadique.qayam.ui.theme.SkyIshaMid
import tech.sadique.qayam.ui.theme.SkyIshaStart
import tech.sadique.qayam.ui.theme.SkyMaghribEnd
import tech.sadique.qayam.ui.theme.SkyMaghribMid
import tech.sadique.qayam.ui.theme.SkyMaghribStart
import tech.sadique.qayam.ui.theme.SkySunriseEnd
import tech.sadique.qayam.ui.theme.SkySunriseMid
import tech.sadique.qayam.ui.theme.SkySunriseStart
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MasjidHorizonCanvas(
    state: CurrentPrayerState?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HorizonAnimation")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SunGlowPulse"
    )

    val starTwinkle by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StarTwinkle"
    )

    val currentPrayer = state?.currentPrayer ?: PrayerType.DHUHR
    val isDaytime = state?.isDaytime ?: true
    val progress = state?.sunProgressPercent ?: 0.5f
    val sunAltitude = state?.sunAltitudeDegrees ?: 45.0

    val skyGradientColors = when (currentPrayer) {
        PrayerType.FAJR -> listOf(SkyFajrStart, SkyFajrMid, SkyFajrEnd)
        PrayerType.SUNRISE -> listOf(SkySunriseStart, SkySunriseMid, SkySunriseEnd)
        PrayerType.ISRAQ -> listOf(SkySunriseMid, SkyDhuhrStart, SkyDhuhrMid)
        PrayerType.DHUHR -> listOf(SkyDhuhrStart, SkyDhuhrMid, SkyDhuhrEnd)
        PrayerType.ASR -> listOf(SkyAsrStart, SkyAsrMid, SkyAsrEnd)
        PrayerType.GURUB_E_AFTAB -> listOf(SkyMaghribStart, SkyMaghribMid, SkyMaghribEnd)
        PrayerType.MAGHRIB -> listOf(SkyMaghribStart, SkyMaghribMid, SkyMaghribEnd)
        PrayerType.ISHA -> listOf(SkyIshaStart, SkyIshaMid, SkyIshaEnd)
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Draw Sky Background Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = skyGradientColors,
                    startY = 0f,
                    endY = height
                )
            )

            // 2. Draw Stars (if night or twilight)
            if (!isDaytime || currentPrayer == PrayerType.FAJR || currentPrayer == PrayerType.ISHA) {
                drawStars(width, height * 0.7f, starTwinkle)
            }

            val horizonY = height * 0.75f

            // 3. Draw Celestial Arc (Sun / Moon orbit guide line)
            val arcPath = Path().apply {
                moveTo(width * 0.08f, horizonY)
                cubicTo(
                    width * 0.25f, height * 0.15f,
                    width * 0.75f, height * 0.15f,
                    width * 0.92f, horizonY
                )
            }
            drawPath(
                path = arcPath,
                color = Color.White.copy(alpha = 0.25f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )

            // 4. Calculate Sun / Moon position along the arc
            // Progress goes 0.0 -> 1.0 (Left horizon -> Peak -> Right horizon)
            val angle = PI * (1.0 - progress) // PI down to 0
            val arcCenterX = width * 0.5f
            val arcRadiusX = width * 0.42f
            val arcRadiusY = height * 0.55f

            val celestialX = (arcCenterX + arcRadiusX * cos(angle)).toFloat()
            val celestialY = (horizonY - arcRadiusY * sin(angle)).toFloat()

            if (isDaytime) {
                // Draw Sun
                drawSun(
                    center = Offset(celestialX, celestialY),
                    pulse = pulse,
                    prayerType = currentPrayer
                )
            } else {
                // Draw Moon (Crescent)
                drawCrescentMoon(
                    center = Offset(celestialX, celestialY),
                    pulse = pulse
                )
            }

            // 5. Draw Horizon Ground & Silhouette Layers
            drawHorizonAndMosque(width, height, horizonY, currentPrayer, isDaytime)
        }
    }
}

private fun DrawScope.drawStars(width: Float, maxHeight: Float, twinkle: Float) {
    val starCoords = listOf(
        Pair(0.12f, 0.20f), Pair(0.25f, 0.12f), Pair(0.38f, 0.28f),
        Pair(0.48f, 0.15f), Pair(0.62f, 0.22f), Pair(0.72f, 0.10f),
        Pair(0.85f, 0.25f), Pair(0.18f, 0.42f), Pair(0.82f, 0.45f),
        Pair(0.55f, 0.35f), Pair(0.30f, 0.48f), Pair(0.68f, 0.50f)
    )

    for ((index, coord) in starCoords.withIndex()) {
        val x = coord.first * width
        val y = coord.second * maxHeight
        val alpha = if (index % 2 == 0) twinkle else (1.3f - twinkle).coerceIn(0.2f, 1.0f)
        val radius = if (index % 3 == 0) 2.5.dp.toPx() else 1.5.dp.toPx()

        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.85f),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawSun(center: Offset, pulse: Float, prayerType: PrayerType) {
    val sunColor = when (prayerType) {
        PrayerType.SUNRISE, PrayerType.GURUB_E_AFTAB, PrayerType.MAGHRIB -> Color(0xFFFF7A00)
        PrayerType.ISRAQ, PrayerType.ASR -> Color(0xFFFFB300)
        else -> Color(0xFFFFD54F)
    }

    // Outer Glow / Corona
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                sunColor.copy(alpha = 0.55f),
                sunColor.copy(alpha = 0.20f),
                Color.Transparent
            ),
            center = center,
            radius = 42.dp.toPx() * pulse
        ),
        radius = 42.dp.toPx() * pulse,
        center = center
    )

    // Inner Radiant Sun
    drawCircle(
        color = Color(0xFFFFF9C4),
        radius = 16.dp.toPx(),
        center = center
    )

    drawCircle(
        color = sunColor,
        radius = 13.dp.toPx(),
        center = center
    )
}

private fun DrawScope.drawCrescentMoon(center: Offset, pulse: Float) {
    // Moon Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                GoldLight.copy(alpha = 0.35f),
                Color.Transparent
            ),
            center = center,
            radius = 35.dp.toPx() * pulse
        ),
        radius = 35.dp.toPx() * pulse,
        center = center
    )

    // Glowing Crescent Moon
    val moonRadius = 14.dp.toPx()
    drawCircle(
        color = Color(0xFFFFF7C2),
        radius = moonRadius,
        center = center
    )

    // Subtracting inner circle for crescent curve
    drawCircle(
        color = SkyIshaMid,
        radius = moonRadius * 0.85f,
        center = Offset(center.x + moonRadius * 0.45f, center.y - moonRadius * 0.25f)
    )

    // Small star near the moon
    drawCircle(
        color = GoldAccent,
        radius = 2.dp.toPx(),
        center = Offset(center.x + moonRadius * 1.3f, center.y + moonRadius * 0.2f)
    )
}

private fun DrawScope.drawHorizonAndMosque(
    width: Float,
    height: Float,
    horizonY: Float,
    prayerType: PrayerType,
    isDaytime: Boolean
) {
    val silhouetteColor = if (isDaytime) {
        when (prayerType) {
            PrayerType.SUNRISE -> Color(0xFF2C1608)
            PrayerType.ISRAQ -> Color(0xFF1B2E1B)
            PrayerType.DHUHR -> Color(0xFF042B22)
            PrayerType.ASR -> Color(0xFF1E1704)
            PrayerType.GURUB_E_AFTAB -> Color(0xFF2A1010)
            PrayerType.MAGHRIB -> Color(0xFF150A1C)
            else -> Color(0xFF071F1A)
        }
    } else {
        Color(0xFF030D0A)
    }

    val accentGoldColor = GoldAccent.copy(alpha = 0.6f)

    // Horizon line glow
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                if (isDaytime) GoldLight.copy(alpha = 0.6f) else Color(0xFF4EE2B6).copy(alpha = 0.4f),
                Color.Transparent
            )
        ),
        start = Offset(0f, horizonY),
        end = Offset(width, horizonY),
        strokeWidth = 2.5.dp.toPx()
    )

    val mosquePath = Path().apply {
        // Base starting point
        moveTo(0f, height)
        lineTo(0f, horizonY)

        val cx = width * 0.5f

        // Left ground terrace
        lineTo(cx - width * 0.38f, horizonY)

        // Left Minaret 1 (Outer left)
        val m1x = cx - width * 0.32f
        val m1Width = 14.dp.toPx()
        val m1Height = 85.dp.toPx()
        lineTo(m1x - m1Width / 2, horizonY)
        lineTo(m1x - m1Width / 2, horizonY - m1Height)
        // Balcony 1
        lineTo(m1x - m1Width * 0.8f, horizonY - m1Height)
        lineTo(m1x - m1Width * 0.8f, horizonY - m1Height - 4.dp.toPx())
        lineTo(m1x - m1Width * 0.4f, horizonY - m1Height - 4.dp.toPx())
        // Upper spire
        lineTo(m1x - m1Width * 0.3f, horizonY - m1Height - 22.dp.toPx())
        lineTo(m1x, horizonY - m1Height - 34.dp.toPx()) // Tip
        lineTo(m1x + m1Width * 0.3f, horizonY - m1Height - 22.dp.toPx())
        lineTo(m1x + m1Width * 0.4f, horizonY - m1Height - 4.dp.toPx())
        lineTo(m1x + m1Width * 0.8f, horizonY - m1Height - 4.dp.toPx())
        lineTo(m1x + m1Width * 0.8f, horizonY - m1Height)
        lineTo(m1x + m1Width / 2, horizonY - m1Height)
        lineTo(m1x + m1Width / 2, horizonY)

        // Left Side Dome
        val d1x = cx - width * 0.18f
        val d1Radius = 24.dp.toPx()
        val d1BaseY = horizonY - 18.dp.toPx()
        lineTo(d1x - d1Radius, horizonY)
        lineTo(d1x - d1Radius, d1BaseY)
        // Dome curve
        cubicTo(
            d1x - d1Radius, d1BaseY - d1Radius * 1.1f,
            d1x - d1Radius * 0.2f, d1BaseY - d1Radius * 1.4f,
            d1x, d1BaseY - d1Radius * 1.55f // Dome tip
        )
        cubicTo(
            d1x + d1Radius * 0.2f, d1BaseY - d1Radius * 1.4f,
            d1x + d1Radius, d1BaseY - d1Radius * 1.1f,
            d1x + d1Radius, d1BaseY
        )
        lineTo(d1x + d1Radius, horizonY)

        // Center Main Grand Dome
        val mainDomeRadius = 42.dp.toPx()
        val mainBaseY = horizonY - 26.dp.toPx()
        val domeTipY = mainBaseY - mainDomeRadius * 1.55f

        lineTo(cx - mainDomeRadius, horizonY)
        lineTo(cx - mainDomeRadius, mainBaseY)
        cubicTo(
            cx - mainDomeRadius, mainBaseY - mainDomeRadius * 1.15f,
            cx - mainDomeRadius * 0.25f, mainBaseY - mainDomeRadius * 1.5f,
            cx, domeTipY
        )
        // Crescent Finial on center dome
        lineTo(cx, domeTipY - 14.dp.toPx())
        lineTo(cx, domeTipY)
        cubicTo(
            cx + mainDomeRadius * 0.25f, mainBaseY - mainDomeRadius * 1.5f,
            cx + mainDomeRadius, mainBaseY - mainDomeRadius * 1.15f,
            cx + mainDomeRadius, mainBaseY
        )
        lineTo(cx + mainDomeRadius, horizonY)

        // Right Side Dome
        val d2x = cx + width * 0.18f
        val d2Radius = 24.dp.toPx()
        val d2BaseY = horizonY - 18.dp.toPx()
        lineTo(d2x - d2Radius, horizonY)
        lineTo(d2x - d2Radius, d2BaseY)
        cubicTo(
            d2x - d2Radius, d2BaseY - d2Radius * 1.1f,
            d2x - d2Radius * 0.2f, d2BaseY - d2Radius * 1.4f,
            d2x, d2BaseY - d2Radius * 1.55f
        )
        cubicTo(
            d2x + d2Radius * 0.2f, d2BaseY - d2Radius * 1.4f,
            d2x + d2Radius, d2BaseY - d2Radius * 1.1f,
            d2x + d2Radius, d2BaseY
        )
        lineTo(d2x + d2Radius, horizonY)

        // Right Minaret 2 (Outer right)
        val m2x = cx + width * 0.32f
        val m2Width = 14.dp.toPx()
        val m2Height = 85.dp.toPx()
        lineTo(m2x - m2Width / 2, horizonY)
        lineTo(m2x - m2Width / 2, horizonY - m2Height)
        lineTo(m2x - m2Width * 0.8f, horizonY - m2Height)
        lineTo(m2x - m2Width * 0.8f, horizonY - m2Height - 4.dp.toPx())
        lineTo(m2x - m2Width * 0.4f, horizonY - m2Height - 4.dp.toPx())
        lineTo(m2x - m2Width * 0.3f, horizonY - m2Height - 22.dp.toPx())
        lineTo(m2x, horizonY - m2Height - 34.dp.toPx()) // Tip
        lineTo(m2x + m2Width * 0.3f, horizonY - m2Height - 22.dp.toPx())
        lineTo(m2x + m2Width * 0.4f, horizonY - m2Height - 4.dp.toPx())
        lineTo(m2x + m2Width * 0.8f, horizonY - m2Height - 4.dp.toPx())
        lineTo(m2x + m2Width * 0.8f, horizonY - m2Height)
        lineTo(m2x + m2Width / 2, horizonY - m2Height)
        lineTo(m2x + m2Width / 2, horizonY)

        // Right ground terrace
        lineTo(width, horizonY)
        lineTo(width, height)
        close()
    }

    drawPath(
        path = mosquePath,
        color = silhouetteColor
    )

    // Draw illuminated crescent on top of the main dome
    val cx = width * 0.5f
    val mainDomeRadius = 42.dp.toPx()
    val mainBaseY = horizonY - 26.dp.toPx()
    val domeTipY = mainBaseY - mainDomeRadius * 1.55f

    drawCircle(
        color = accentGoldColor,
        radius = 3.5.dp.toPx(),
        center = Offset(cx, domeTipY - 14.dp.toPx())
    )
}
