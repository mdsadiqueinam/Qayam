
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import tech.sadique.qayam.data.model.CurrentPrayerState
import tech.sadique.qayam.data.model.PrayerType
import tech.sadique.qayam.ui.theme.DarkPrimary
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

private const val STAR_FIELD_FRACTION = 0.7f
private const val HORIZON_FRACTION = 0.80f
private const val DASH_ON_LENGTH = 12f
private const val DASH_OFF_LENGTH = 10f
private const val MOON_CRATER_OFFSET_X_FACTOR = 0.45f
private const val MOON_CRATER_OFFSET_Y_FACTOR = 0.25f
private const val MOON_STAR_OFFSET_X_FACTOR = 1.3f
private const val MOON_STAR_OFFSET_Y_FACTOR = 0.2f

private val SUN_CORE_HIGHLIGHT = Color(0xFFFFF9C4)
private val MOON_SURFACE_COLOR = Color(0xFFFFF7C2)

@Composable
fun MasjidHorizonCanvas(state: CurrentPrayerState?, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "HorizonAnimation")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "SunGlowPulse",
    )

    val currentPrayer = state?.currentPrayer ?: PrayerType.DHUHR
    val isDaytime = state?.isDaytime ?: true
    val showStars = !isDaytime || currentPrayer == PrayerType.FAJR || currentPrayer == PrayerType.ISHA
    val starTwinkle = runStarTwinkle(showStars)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            drawSkyWithStars(width, height, currentPrayer, showStars, starTwinkle)
            val horizonY = height * HORIZON_FRACTION
            drawOrbitAndCelestial(width, horizonY, state, pulse)
            drawHorizonAndMosque(width, height, horizonY, currentPrayer, isDaytime)
        }
    }
}

@Composable
private fun runStarTwinkle(showStars: Boolean): Float {
    if (!showStars) return 1f
    val twinkleTransition = rememberInfiniteTransition(label = "StarTwinkle")
    val twinkle by twinkleTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "StarTwinkleValue",
    )
    return twinkle
}

private fun DrawScope.drawSkyWithStars(
    width: Float,
    height: Float,
    currentPrayer: PrayerType,
    showStars: Boolean,
    starTwinkle: Float,
) {
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
    drawRect(
        brush = Brush.verticalGradient(colors = skyGradientColors, startY = 0f, endY = height),
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent),
            startY = 0f,
            endY = height * 0.55f,
        ),
    )
    if (showStars) {
        val starCoords = listOf(
            Pair(0.12f, 0.20f), Pair(0.25f, 0.12f), Pair(0.38f, 0.28f),
            Pair(0.48f, 0.15f), Pair(0.62f, 0.22f), Pair(0.72f, 0.10f),
            Pair(0.85f, 0.25f), Pair(0.18f, 0.42f), Pair(0.82f, 0.45f),
            Pair(0.55f, 0.35f), Pair(0.30f, 0.48f), Pair(0.68f, 0.50f),
        )
        for ((index, coord) in starCoords.withIndex()) {
            val x = coord.first * width
            val y = coord.second * (height * STAR_FIELD_FRACTION)
            val alpha = if (index % 2 == 0) starTwinkle else (1.3f - starTwinkle).coerceIn(0.2f, 1.0f)
            val radius = if (index % 3 == 0) 2.5.dp.toPx() else 1.5.dp.toPx()
            drawCircle(color = Color.White.copy(alpha = alpha * 0.85f), radius = radius, center = Offset(x, y))
        }
    }
}

private fun DrawScope.drawOrbitAndCelestial(width: Float, horizonY: Float, state: CurrentPrayerState?, pulse: Float) {
    val height = size.height
    val currentPrayer = state?.currentPrayer ?: PrayerType.DHUHR
    val isDaytime = state?.isDaytime ?: true
    val progress = state?.sunProgressPercent ?: 0.5f
    val arcPath = Path().apply {
        moveTo(width * 0.08f, horizonY)
        cubicTo(width * 0.25f, height * 0.15f, width * 0.75f, height * 0.15f, width * 0.92f, horizonY)
    }
    drawPath(
        path = arcPath,
        color = Color.White.copy(alpha = 0.25f),
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON_LENGTH, DASH_OFF_LENGTH), 0f),
        ),
    )
    val angle = PI * (1.0 - progress)
    val arcCenterX = width * 0.5f
    val arcRadiusX = width * 0.42f
    val arcRadiusY = height * 0.55f
    val celestialX = (arcCenterX + arcRadiusX * cos(angle)).toFloat()
    val celestialY = (horizonY - arcRadiusY * sin(angle)).toFloat()
    if (isDaytime) {
        drawSun(center = Offset(celestialX, celestialY), pulse = pulse, prayerType = currentPrayer)
    } else {
        drawCrescentMoon(center = Offset(celestialX, celestialY), pulse = pulse)
    }
}

private fun DrawScope.drawSun(center: Offset, pulse: Float, prayerType: PrayerType) {
    val sunColor = when (prayerType) {
        PrayerType.SUNRISE, PrayerType.GURUB_E_AFTAB, PrayerType.MAGHRIB -> Color(0xFFFF7A00)
        PrayerType.ISRAQ, PrayerType.ASR -> Color(0xFFFFB300)
        else -> Color(0xFFFFD54F)
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(sunColor.copy(alpha = 0.55f), sunColor.copy(alpha = 0.20f), Color.Transparent),
            center = center,
            radius = 42.dp.toPx() * pulse,
        ),
        radius = 42.dp.toPx() * pulse,
        center = center,
    )
    drawCircle(color = SUN_CORE_HIGHLIGHT, radius = 16.dp.toPx(), center = center)
    drawCircle(color = sunColor, radius = 13.dp.toPx(), center = center)
}

private fun DrawScope.drawCrescentMoon(center: Offset, pulse: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(GoldLight.copy(alpha = 0.35f), Color.Transparent),
            center = center,
            radius = 35.dp.toPx() * pulse,
        ),
        radius = 35.dp.toPx() * pulse,
        center = center,
    )
    val moonRadius = 14.dp.toPx()
    drawCircle(color = MOON_SURFACE_COLOR, radius = moonRadius, center = center)
    drawCircle(
        color = SkyIshaMid,
        radius = moonRadius * 0.85f,
        center = Offset(
            center.x + moonRadius * MOON_CRATER_OFFSET_X_FACTOR,
            center.y - moonRadius * MOON_CRATER_OFFSET_Y_FACTOR,
        ),
    )
    drawCircle(
        color = GoldAccent,
        radius = 2.dp.toPx(),
        center = Offset(
            center.x + moonRadius * MOON_STAR_OFFSET_X_FACTOR,
            center.y + moonRadius * MOON_STAR_OFFSET_Y_FACTOR,
        ),
    )
}

private fun DrawScope.drawHorizonAndMosque(
    width: Float,
    height: Float,
    horizonY: Float,
    prayerType: PrayerType,
    isDaytime: Boolean,
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
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                if (isDaytime) GoldLight.copy(alpha = 0.6f) else DarkPrimary.copy(alpha = 0.4f),
                Color.Transparent,
            ),
        ),
        start = Offset(0f, horizonY),
        end = Offset(width, horizonY),
        strokeWidth = 2.5.dp.toPx(),
    )
    val minaretWidth = 14.dp.toPx()
    val minaretHeight = 85.dp.toPx()
    val sideDomeRadius = 24.dp.toPx()
    val sideDomeBaseOffset = 18.dp.toPx()
    val mainDomeRadius = 42.dp.toPx()
    val mainDomeBaseOffset = 26.dp.toPx()
    val cx = width * 0.5f
    val mosquePath = Path().apply {
        moveTo(0f, height)
        lineTo(0f, horizonY)
        lineTo(cx - width * 0.38f, horizonY)
        appendMinaret(cx - width * 0.32f, horizonY, minaretWidth, minaretHeight)
        appendDome(cx - width * 0.18f, horizonY, sideDomeBaseOffset, sideDomeRadius, false)
        appendDome(cx, horizonY, mainDomeBaseOffset, mainDomeRadius, true)
        appendDome(cx + width * 0.18f, horizonY, sideDomeBaseOffset, sideDomeRadius, false)
        appendMinaret(cx + width * 0.32f, horizonY, minaretWidth, minaretHeight)
        lineTo(width, horizonY)
        lineTo(width, height)
        close()
    }
    drawPath(path = mosquePath, color = silhouetteColor)
    val tipScale = 1.55f
    val domeTipY = horizonY - mainDomeBaseOffset - mainDomeRadius * tipScale
    drawCircle(
        color = accentGoldColor,
        radius = 3.5.dp.toPx(),
        center = Offset(cx, domeTipY - 14.dp.toPx()),
    )
}

private fun Path.appendMinaret(centerX: Float, horizonY: Float, towerWidth: Float, towerHeight: Float) {
    val halfWidth = towerWidth / 2
    val wideOffset = towerWidth * 0.8f
    val midOffset = towerWidth * 0.4f
    val tipOffset = towerWidth * 0.3f
    val balconyDepth = towerHeight * 4f / 85f
    val spireHeight = towerHeight * 22f / 85f
    val tipHeight = towerHeight * 34f / 85f
    lineTo(centerX - halfWidth, horizonY)
    lineTo(centerX - halfWidth, horizonY - towerHeight)
    lineTo(centerX - wideOffset, horizonY - towerHeight)
    lineTo(centerX - wideOffset, horizonY - towerHeight - balconyDepth)
    lineTo(centerX - midOffset, horizonY - towerHeight - balconyDepth)
    lineTo(centerX - tipOffset, horizonY - towerHeight - spireHeight)
    lineTo(centerX, horizonY - towerHeight - tipHeight)
    lineTo(centerX + tipOffset, horizonY - towerHeight - spireHeight)
    lineTo(centerX + midOffset, horizonY - towerHeight - balconyDepth)
    lineTo(centerX + wideOffset, horizonY - towerHeight - balconyDepth)
    lineTo(centerX + wideOffset, horizonY - towerHeight)
    lineTo(centerX + halfWidth, horizonY - towerHeight)
    lineTo(centerX + halfWidth, horizonY)
}

private fun Path.appendDome(centerX: Float, horizonY: Float, baseOffset: Float, radius: Float, isMain: Boolean) {
    val baseY = horizonY - baseOffset
    val lowFactor = if (isMain) 1.15f else 1.1f
    val highFactor = if (isMain) 1.5f else 1.4f
    val narrowFactor = if (isMain) 0.25f else 0.2f
    val tipFactor = 1.55f
    val tipY = baseY - radius * tipFactor
    val lowShoulder = radius * lowFactor
    val highShoulder = radius * highFactor
    val narrow = radius * narrowFactor
    lineTo(centerX - radius, horizonY)
    lineTo(centerX - radius, baseY)
    cubicTo(centerX - radius, baseY - lowShoulder, centerX - narrow, baseY - highShoulder, centerX, tipY)
    if (isMain) {
        val finialHeight = radius * 14f / 42f
        lineTo(centerX, tipY - finialHeight)
        lineTo(centerX, tipY)
    }
    cubicTo(centerX + narrow, baseY - highShoulder, centerX + radius, baseY - lowShoulder, centerX + radius, baseY)
    lineTo(centerX + radius, horizonY)
}

@androidx.compose.ui.tooling.preview.Preview(name = "Horizon day", showBackground = true)
@Composable
private fun MasjidHorizonDayPreview() {
    tech.sadique.qayam.ui.theme.SalahTheme {
        MasjidHorizonCanvas(state = previewPrayerState())
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Horizon night mosque", showBackground = true)
@Composable
private fun MasjidHorizonNightPreview() {
    tech.sadique.qayam.ui.theme.SalahTheme(
        themeMode = tech.sadique.qayam.data.model.AppThemeMode.NIGHT_MOSQUE,
    ) {
        MasjidHorizonCanvas(state = previewPrayerState().copy(isDaytime = false))
    }
}
