package com.lexaprograms.polishcards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BouncySuccessCheckmark(playSignal: Int) {
    val progress = remember { Animatable(0f) }
    val scale = remember { Animatable(0.72f) }

    LaunchedEffect(playSignal) {
        progress.snapTo(0f)
        scale.snapTo(0.72f)
        scale.animateTo(1.12f, tween(260, easing = EaseOutBack))
        scale.animateTo(1f, tween(120))
        progress.animateTo(1f, tween(420))
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size((178 * scale.value).dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.42f
            drawCircle(
                color = Color(0xFFE4F8F2),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0xFF37B99D),
                radius = radius,
                center = center,
                style = Stroke(width = size.minDimension * 0.055f)
            )

            val start = Offset(size.width * 0.31f, size.height * 0.54f)
            val mid = Offset(size.width * 0.46f, size.height * 0.68f)
            val end = Offset(size.width * 0.72f, size.height * 0.38f)
            if (progress.value <= 0.5f) {
                val t = progress.value / 0.5f
                drawLine(
                    color = Color(0xFF303735),
                    start = start,
                    end = lerp(start, mid, t),
                    strokeWidth = size.minDimension * 0.07f,
                    cap = StrokeCap.Round
                )
            } else {
                drawLine(
                    color = Color(0xFF303735),
                    start = start,
                    end = mid,
                    strokeWidth = size.minDimension * 0.07f,
                    cap = StrokeCap.Round
                )
                val t = (progress.value - 0.5f) / 0.5f
                drawLine(
                    color = Color(0xFF303735),
                    start = mid,
                    end = lerp(mid, end, t),
                    strokeWidth = size.minDimension * 0.07f,
                    cap = StrokeCap.Round
                )
            }
        }
        var caption by remember { mutableStateOf("Correct!") }
        LaunchedEffect(playSignal) { caption = "Correct!" }
        Text(
            text = caption,
            modifier = Modifier.align(Alignment.BottomCenter),
            color = Color(0xFF37B99D),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
    }
}

private fun lerp(start: Offset, end: Offset, t: Float): Offset {
    return Offset(
        x = start.x + (end.x - start.x) * t.coerceIn(0f, 1f),
        y = start.y + (end.y - start.y) * t.coerceIn(0f, 1f)
    )
}

@Composable
fun WrongAnswerCardShake(playSignal: Int) {
    val shake = remember { Animatable(0f) }

    LaunchedEffect(playSignal) {
        shake.snapTo(0f)
        listOf(-26f, 24f, -18f, 14f, -8f, 0f).forEach { target ->
            shake.animateTo(target, tween(48))
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .width(292.dp)
                .height(176.dp)
                .offset { IntOffset(shake.value.roundToInt(), 0) },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    text = "Almost",
                    color = Color(0xFFE85D75),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Try this card again",
                    color = Color(0xFF303735),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "The answer is close, but one detail needs a second look.",
                    color = Color(0xFF65706C),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun LessonCardFlipReveal(playSignal: Int) {
    val rotation = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(playSignal) {
        rotation.snapTo(0f)
        rotation.animateTo(180f, tween(650))
    }

    val rawRotation = rotation.value
    val showingBack = rawRotation > 90f
    val visibleRotation = if (showingBack) rawRotation - 180f else rawRotation

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .width(304.dp)
                .height(202.dp)
                .graphicsLayer {
                    rotationY = visibleRotation
                    cameraDistance = 14f * density.density
                },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (showingBack) Color(0xFFE4F8F2) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (showingBack) "Answer" else "Prompt",
                    color = Color(0xFF65706C),
                    fontWeight = FontWeight.Bold
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (showingBack) "I make mistakes" else "I learn daily",
                        color = Color(0xFF303735),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = if (showingBack) "tap next" else "tap to reveal",
                    color = Color(0xFF37B99D),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StreakFlamePulse(playSignal: Int) {
    val infinite = rememberInfiniteTransition(label = "flameLoop")
    val pulse by infinite.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1080), RepeatMode.Reverse),
        label = "flamePulse"
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "streakShimmer"
    )
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Restart),
        label = "streakOrbit"
    )
    val flicker by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(1180), RepeatMode.Restart),
        label = "flameFlicker"
    )
    val burst = remember { Animatable(0f) }

    LaunchedEffect(playSignal) {
        burst.snapTo(1f)
        burst.animateTo(0f, tween(680))
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(328.dp)) {
            val w = size.width
            val h = size.height
            val cardTop = h * 0.15f
            val cardLeft = w * 0.11f
            val cardSize = Size(w * 0.78f, h * 0.66f)
            val c = Offset(w * 0.5f, h * 0.47f)
            val badgeRadius = w * (0.155f + pulse * 0.006f + burst.value * 0.006f)
            val flameLean = sin(flicker) * badgeRadius * 0.055f
            val flameLift = sin(flicker * 1.7f) * badgeRadius * 0.035f - burst.value * badgeRadius * 0.08f

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD166).copy(alpha = 0.24f + burst.value * 0.18f),
                        Color(0xFF8FE3CF).copy(alpha = 0.24f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = w * (0.55f + burst.value * 0.10f)
                ),
                topLeft = Offset(w * 0.02f, h * 0.02f),
                size = Size(w * 0.96f, h * 0.92f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )
            drawRoundRect(
                color = Color(0xFF21302D).copy(alpha = 0.10f),
                topLeft = Offset(cardLeft, cardTop + h * 0.020f),
                size = cardSize,
                cornerRadius = CornerRadius(w * 0.060f, w * 0.060f)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.96f), Color(0xFFE9FAF6).copy(alpha = 0.96f)),
                    startY = cardTop,
                    endY = cardTop + cardSize.height
                ),
                topLeft = Offset(cardLeft, cardTop),
                size = cardSize,
                cornerRadius = CornerRadius(w * 0.060f, w * 0.060f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.70f),
                topLeft = Offset(cardLeft + w * 0.035f, cardTop + h * 0.035f),
                size = Size(w * 0.32f, h * 0.010f),
                cornerRadius = CornerRadius(w * 0.010f, w * 0.010f)
            )

            repeat(7) { index ->
                val x = cardLeft + w * 0.125f + index * w * 0.088f
                val y = cardTop + h * 0.105f
                val filled = index < 5
                val active = index == 4
                drawRoundRect(
                    color = if (filled) Color(0xFF37B99D).copy(alpha = if (active) 0.95f else 0.58f) else Color(0xFFDCE7E3),
                    topLeft = Offset(x - w * 0.027f, y - h * 0.010f),
                    size = Size(w * 0.054f + if (active) w * 0.025f else 0f, h * 0.020f),
                    cornerRadius = CornerRadius(w * 0.012f, w * 0.012f)
                )
                if (active) {
                    drawCircle(Color.White.copy(alpha = 0.72f), w * 0.006f, Offset(x + w * 0.027f, y))
                }
            }

            drawCircle(
                color = Color(0xFF17201E).copy(alpha = 0.10f),
                radius = w * 0.240f,
                center = Offset(c.x, c.y + h * 0.040f)
            )
            drawCircle(Color.White.copy(alpha = 0.86f), w * 0.214f, c)
            drawCircle(
                Color(0xFFE4F8F2).copy(alpha = 0.94f),
                w * (0.198f + burst.value * 0.012f),
                c,
                style = Stroke(width = w * 0.015f)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color(0xFF8FE3CF), Color(0xFFFFD166), Color(0xFFFF7043), Color(0xFF8FE3CF)),
                    center = c
                ),
                startAngle = -90f + orbit * 0.05f,
                sweepAngle = 250f + burst.value * 52f,
                useCenter = false,
                topLeft = Offset(c.x - w * 0.225f, c.y - w * 0.225f),
                size = Size(w * 0.45f, w * 0.45f),
                style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
            )

            repeat(10) { index ->
                val angle = ((index * 36f + orbit * 0.42f) / 180f * PI).toFloat()
                val radius = w * (0.235f + (index % 2) * 0.030f + burst.value * 0.030f)
                val dot = Offset(c.x + cos(angle) * radius, c.y + sin(angle) * radius)
                val alpha = 0.26f + 0.24f * ((index % 3) / 2f)
                drawCircle(
                    color = listOf(Color(0xFFFFD166), Color(0xFF8FE3CF), Color(0xFFFF7043))[index % 3].copy(alpha = alpha),
                    radius = w * (0.005f + index % 2 * 0.004f),
                    center = dot
                )
            }

            val baseTop = h * 0.655f + shimmer * h * 0.006f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2E3A37).copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(w * 0.50f, h * 0.745f),
                    radius = w * 0.22f
                ),
                topLeft = Offset(w * 0.31f, h * 0.70f),
                size = Size(w * 0.38f, h * 0.070f)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF465752), Color(0xFF182422)),
                    startY = baseTop,
                    endY = baseTop + h * 0.12f
                ),
                topLeft = Offset(w * 0.34f, baseTop),
                size = Size(w * 0.32f, h * 0.080f),
                cornerRadius = CornerRadius(w * 0.040f, w * 0.040f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.30f),
                topLeft = Offset(w * 0.38f, baseTop + h * 0.012f),
                size = Size(w * 0.24f, h * 0.008f),
                cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
            )

            repeat(3) { index ->
                val trailScale = 1f + index * 0.045f + burst.value * 0.040f
                val trailAlpha = 0.12f - index * 0.026f
                val trail = Path().apply {
                    moveTo(c.x + flameLean * 1.2f, c.y - badgeRadius * 1.45f * trailScale + flameLift)
                    cubicTo(c.x + badgeRadius * 0.50f * trailScale, c.y - badgeRadius * 0.96f, c.x + badgeRadius * 0.98f * trailScale, c.y - badgeRadius * 0.54f, c.x + badgeRadius * 0.82f * trailScale, c.y + badgeRadius * 0.28f)
                    cubicTo(c.x + badgeRadius * 0.70f * trailScale, c.y + badgeRadius * 1.02f, c.x + badgeRadius * 0.18f, c.y + badgeRadius * 1.32f, c.x, c.y + badgeRadius * 1.34f)
                    cubicTo(c.x - badgeRadius * 0.82f * trailScale, c.y + badgeRadius * 1.14f, c.x - badgeRadius * 1.02f * trailScale, c.y + badgeRadius * 0.44f, c.x - badgeRadius * 0.70f * trailScale, c.y - badgeRadius * 0.18f)
                    cubicTo(c.x - badgeRadius * 0.44f * trailScale, c.y - badgeRadius * 0.62f, c.x - badgeRadius * 0.14f, c.y - badgeRadius * 0.74f, c.x + flameLean * 1.2f, c.y - badgeRadius * 1.45f * trailScale + flameLift)
                    close()
                }
                drawPath(trail, Color(0xFFFF7043).copy(alpha = trailAlpha))
            }

            val outerFlame = Path().apply {
                moveTo(c.x + flameLean, c.y - badgeRadius * 1.50f + flameLift)
                cubicTo(c.x + badgeRadius * (0.48f + sin(flicker) * 0.04f), c.y - badgeRadius * 0.98f, c.x + badgeRadius * 1.00f, c.y - badgeRadius * 0.56f, c.x + badgeRadius * 0.86f, c.y + badgeRadius * 0.28f)
                cubicTo(c.x + badgeRadius * 0.78f, c.y + badgeRadius * 1.05f, c.x + badgeRadius * 0.22f, c.y + badgeRadius * 1.38f, c.x, c.y + badgeRadius * 1.38f)
                cubicTo(c.x - badgeRadius * 0.88f, c.y + badgeRadius * 1.18f, c.x - badgeRadius * 1.08f, c.y + badgeRadius * 0.48f, c.x - badgeRadius * 0.72f, c.y - badgeRadius * 0.18f)
                cubicTo(c.x - badgeRadius * (0.46f - sin(flicker) * 0.03f), c.y - badgeRadius * 0.64f, c.x - badgeRadius * 0.16f, c.y - badgeRadius * 0.74f, c.x + flameLean, c.y - badgeRadius * 1.50f + flameLift)
                close()
            }
            drawPath(
                path = outerFlame,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFD166), Color(0xFFFF8A1F), Color(0xFFFF4A2F)),
                    startY = c.y - badgeRadius * 1.5f,
                    endY = c.y + badgeRadius * 1.5f
                )
            )
            drawPath(outerFlame, Color.White.copy(alpha = 0.26f), style = Stroke(width = w * 0.010f, cap = StrokeCap.Round))

            val innerFlame = Path().apply {
                moveTo(c.x + badgeRadius * 0.02f + flameLean * 0.45f, c.y - badgeRadius * 0.54f + flameLift * 0.25f)
                cubicTo(c.x + badgeRadius * 0.38f, c.y - badgeRadius * 0.06f, c.x + badgeRadius * 0.58f, c.y + badgeRadius * 0.30f, c.x + badgeRadius * 0.38f, c.y + badgeRadius * 0.78f)
                cubicTo(c.x + badgeRadius * 0.10f, c.y + badgeRadius * 1.15f, c.x - badgeRadius * 0.34f, c.y + badgeRadius * 0.98f, c.x - badgeRadius * 0.48f, c.y + badgeRadius * 0.55f)
                cubicTo(c.x - badgeRadius * 0.60f, c.y + badgeRadius * 0.18f, c.x - badgeRadius * 0.24f, c.y - badgeRadius * 0.20f, c.x + badgeRadius * 0.02f + flameLean * 0.45f, c.y - badgeRadius * 0.54f + flameLift * 0.25f)
                close()
            }
            drawPath(
                path = innerFlame,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF3A6), Color(0xFFFFD447), Color(0xFFFFA22F)),
                    startY = c.y - badgeRadius * 0.58f,
                    endY = c.y + badgeRadius * 1.12f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.48f), Color.Transparent),
                    center = Offset(c.x - badgeRadius * 0.34f, c.y - badgeRadius * 0.82f),
                    radius = badgeRadius * 0.54f
                ),
                center = Offset(c.x - badgeRadius * 0.30f, c.y - badgeRadius * 0.72f),
                radius = badgeRadius * 0.38f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.30f + 0.16f * shimmer),
                start = Offset(c.x - badgeRadius * 0.38f, c.y - badgeRadius * 0.72f),
                end = Offset(c.x - badgeRadius * 0.17f, c.y + badgeRadius * 0.62f),
                strokeWidth = w * 0.008f,
                cap = StrokeCap.Round
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.0f), Color.White.copy(alpha = 0.44f * burst.value), Color.White.copy(alpha = 0.0f)),
                    center = c,
                    radius = w * (0.18f + burst.value * 0.32f)
                ),
                center = c,
                radius = w * (0.18f + burst.value * 0.32f)
            )

            repeat(6) { index ->
                val x = cardLeft + w * 0.115f + index * w * 0.108f
                val y = cardTop + h * 0.560f
                val height = h * (0.020f + index * 0.006f + shimmer * 0.004f)
                drawRoundRect(
                    color = Color(0xFF37B99D).copy(alpha = 0.13f + index * 0.025f),
                    topLeft = Offset(x, y - height),
                    size = Size(w * 0.060f, height),
                    cornerRadius = CornerRadius(w * 0.016f, w * 0.016f)
                )
            }
        }
    }
}

@Composable
fun MicrophoneListeningWaveform(playSignal: Int) {
    val infinite = rememberInfiniteTransition(label = "micWaveLoop")
    val halo by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1120), RepeatMode.Reverse),
        label = "micHalo"
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(1320), RepeatMode.Restart),
        label = "micWavePhase"
    )
    val ring by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Restart),
        label = "micRing"
    )
    val scan by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Restart),
        label = "micScan"
    )
    val ping = remember { Animatable(0f) }

    LaunchedEffect(playSignal) {
        ping.snapTo(1f)
        ping.animateTo(0f, tween(640))
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(328.dp)) {
            val w = size.width
            val h = size.height
            val panelLeft = w * 0.10f
            val panelTop = h * 0.15f
            val panelSize = Size(w * 0.80f, h * 0.66f)
            val c = Offset(w * 0.5f, h * 0.47f)
            val r = w * 0.245f

            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8FE3CF).copy(alpha = 0.28f + ping.value * 0.14f),
                        Color.White.copy(alpha = 0.68f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = w * (0.55f + ping.value * 0.09f)
                ),
                topLeft = Offset(w * 0.02f, h * 0.02f),
                size = Size(w * 0.96f, h * 0.92f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )
            drawRoundRect(
                color = Color(0xFF21302D).copy(alpha = 0.10f),
                topLeft = Offset(panelLeft, panelTop + h * 0.020f),
                size = panelSize,
                cornerRadius = CornerRadius(w * 0.060f, w * 0.060f)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.96f), Color(0xFFEAF9F6).copy(alpha = 0.96f)),
                    startY = panelTop,
                    endY = panelTop + panelSize.height
                ),
                topLeft = Offset(panelLeft, panelTop),
                size = panelSize,
                cornerRadius = CornerRadius(w * 0.060f, w * 0.060f)
            )
            drawRoundRect(
                color = Color(0xFF37B99D).copy(alpha = 0.12f),
                topLeft = Offset(panelLeft + w * 0.075f, panelTop + h * 0.490f),
                size = Size(w * 0.65f, h * 0.090f),
                cornerRadius = CornerRadius(w * 0.030f, w * 0.030f)
            )
            drawCircle(Color.White.copy(alpha = 0.72f), r * 1.22f, c)
            drawCircle(Color(0xFFE4F8F2).copy(alpha = 0.90f), r * 1.00f, c, style = Stroke(width = w * 0.010f))
            drawArc(
                color = Color(0xFF8FE3CF).copy(alpha = 0.56f),
                startAngle = -212f + ring * 0.24f,
                sweepAngle = 132f,
                useCenter = false,
                topLeft = Offset(c.x - r * 1.18f, c.y - r * 1.18f),
                size = Size(r * 2.36f, r * 2.36f),
                style = Stroke(width = w * 0.024f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF37B99D).copy(alpha = 0.38f),
                startAngle = 16f - ring * 0.18f,
                sweepAngle = 104f + ping.value * 32f,
                useCenter = false,
                topLeft = Offset(c.x - r * 1.38f, c.y - r * 1.38f),
                size = Size(r * 2.76f, r * 2.76f),
                style = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
            )

            repeat(21) { index ->
                val x = panelLeft + w * 0.095f + index * w * 0.030f
                val amp = (sin(wavePhase + index * 0.72f) * 0.5f + 0.5f)
                val barH = h * (0.020f + amp * 0.082f + ping.value * 0.018f)
                val top = panelTop + h * 0.545f - barH
                val color = if (index in 8..13) Color(0xFF20C997) else Color(0xFF8FE3CF)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.95f), Color(0xFF37B99D).copy(alpha = 0.58f)),
                        startY = top,
                        endY = panelTop + h * 0.545f
                    ),
                    topLeft = Offset(x, top),
                    size = Size(w * 0.014f, barH),
                    cornerRadius = CornerRadius(w * 0.012f, w * 0.012f)
                )
            }

            repeat(12) { index ->
                val angle = ((index * 30f + ring * 0.15f) / 180f * PI).toFloat()
                val radius = r * (0.95f + (index % 2) * 0.12f)
                val dot = Offset(c.x + cos(angle) * radius, c.y + sin(angle) * radius)
                drawCircle(Color(0xFF37B99D).copy(alpha = 0.18f), w * 0.0045f, dot)
            }

            drawCircle(
                color = Color(0xFF17201E).copy(alpha = 0.13f),
                radius = w * (0.168f + ping.value * 0.010f),
                center = Offset(c.x, c.y + h * 0.018f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF4C5A56), Color(0xFF17201E)),
                    center = Offset(c.x - w * 0.055f, c.y - h * 0.080f),
                    radius = w * 0.23f
                ),
                radius = w * (0.145f * halo + ping.value * 0.006f),
                center = c
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(c.x - w * 0.045f, c.y - h * 0.070f),
                    radius = w * 0.105f
                ),
                radius = w * 0.102f,
                center = Offset(c.x - w * 0.038f, c.y - h * 0.050f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.90f),
                topLeft = Offset(c.x - w * 0.032f, c.y - h * 0.080f),
                size = Size(w * 0.064f, h * 0.122f),
                cornerRadius = CornerRadius(w * 0.030f, w * 0.030f)
            )
            drawRoundRect(
                color = Color(0xFF17201E),
                topLeft = Offset(c.x - w * 0.020f, c.y - h * 0.065f),
                size = Size(w * 0.040f, h * 0.088f),
                cornerRadius = CornerRadius(w * 0.020f, w * 0.020f)
            )
            drawLine(Color.White.copy(alpha = 0.90f), Offset(c.x, c.y + h * 0.052f), Offset(c.x, c.y + h * 0.105f), w * 0.010f, StrokeCap.Round)
            drawArc(
                color = Color.White.copy(alpha = 0.90f),
                startAngle = 28f,
                sweepAngle = 124f,
                useCenter = false,
                topLeft = Offset(c.x - w * 0.068f, c.y - h * 0.008f),
                size = Size(w * 0.136f, h * 0.110f),
                style = Stroke(width = w * 0.011f, cap = StrokeCap.Round)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.90f),
                topLeft = Offset(c.x - w * 0.064f, c.y + h * 0.112f),
                size = Size(w * 0.128f, h * 0.016f),
                cornerRadius = CornerRadius(w * 0.008f, w * 0.008f)
            )
            drawRoundRect(
                color = Color(0xFF8FE3CF).copy(alpha = 0.76f),
                topLeft = Offset(c.x - w * 0.090f + scan * w * 0.180f, c.y + h * 0.154f),
                size = Size(w * 0.030f, h * 0.008f),
                cornerRadius = CornerRadius(w * 0.004f, w * 0.004f)
            )
            drawArc(
                color = Color(0xFF17201E),
                startAngle = 24f,
                sweepAngle = 132f,
                useCenter = false,
                topLeft = Offset(c.x - w * 0.180f, h * 0.420f),
                size = Size(w * 0.360f, h * 0.245f),
                style = Stroke(width = w * 0.030f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF8FE3CF).copy(alpha = 0.96f),
                startAngle = 200f,
                sweepAngle = 20f + ping.value * 24f,
                useCenter = false,
                topLeft = Offset(c.x - w * 0.184f, h * 0.418f),
                size = Size(w * 0.368f, h * 0.250f),
                style = Stroke(width = w * 0.010f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF8FE3CF).copy(alpha = 0.96f),
                startAngle = -42f - ping.value * 16f,
                sweepAngle = 20f + ping.value * 24f,
                useCenter = false,
                topLeft = Offset(c.x - w * 0.184f, h * 0.418f),
                size = Size(w * 0.368f, h * 0.250f),
                style = Stroke(width = w * 0.010f, cap = StrokeCap.Round)
            )
            drawCircle(Color(0xFF8FE3CF).copy(alpha = 0.94f), w * 0.012f * halo, Offset(c.x, h * 0.716f))
            drawCircle(Color.White.copy(alpha = 0.70f), w * 0.004f, Offset(c.x - w * 0.004f, h * 0.712f))

            drawCircle(
                color = Color(0xFF8FE3CF).copy(alpha = 0.10f * ping.value),
                radius = w * (0.21f + ping.value * 0.26f),
                center = c,
                style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun CharacterIdleBreathingBlink(playSignal: Int) {
    val infinite = rememberInfiniteTransition(label = "idlePolish")
    val focusShift by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(5400), RepeatMode.Reverse),
        label = "idleFocusShift"
    )
    val headDrift by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(MotionTokens.characterIdleSpec, RepeatMode.Reverse),
        label = "idleHeadDrift"
    )
    val replayBounce = remember { Animatable(0f) }

    LaunchedEffect(playSignal) {
        replayBounce.snapTo(1f)
        replayBounce.animateTo(0f, MotionTokens.softSpring)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(328.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF8FE3CF).copy(alpha = 0.30f + replayBounce.value * 0.10f),
                        Color.White.copy(alpha = 0.82f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.50f, h * 0.46f),
                    radius = w * (0.56f + replayBounce.value * 0.08f)
                ),
                topLeft = Offset(w * 0.02f, h * 0.02f),
                size = Size(w * 0.96f, h * 0.92f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.70f),
                topLeft = Offset(w * 0.13f, h * 0.17f),
                size = Size(w * 0.74f, h * 0.66f),
                cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
            )
            repeat(5) { index ->
                val x = w * (0.20f + index * 0.15f)
                val y = h * (0.22f + sin(index + replayBounce.value * PI.toFloat()) * 0.010f)
                drawCircle(
                    color = listOf(Color(0xFF8FE3CF), Color(0xFFFFD166), Color(0xFFFFB6A8))[index % 3].copy(alpha = 0.34f),
                    radius = w * (0.010f + index % 2 * 0.004f),
                    center = Offset(x, y)
                )
            }
        }
        MakeMistakeCharacter(
            modifier = Modifier.graphicsLayer {
                translationY = -replayBounce.value * 14f
                scaleX = 1f + replayBounce.value * 0.025f
                scaleY = 1f + replayBounce.value * 0.025f
            },
            emotion = CharacterEmotion.Happy,
            headOffsetY = headDrift * 1.8f - replayBounce.value * 4f,
            headRotation = headDrift * 0.55f,
            bodyScaleX = 1f + replayBounce.value * 0.014f,
            bodyScaleY = 1f - replayBounce.value * 0.010f,
            eyeScale = 0.96f,
            eyeFocusX = focusShift,
            armOffset = headDrift * 0.18f,
            accentIntensity = replayBounce.value * 0.35f,
            isSpeaking = false,
            isListening = false,
            speakingIntensity = 0f,
            listeningLevel = 0f,
            enableIdleBreathing = true,
            enableBlinking = true
        )
    }
}

@Composable
fun CharacterTalkingMouthFlaps(playSignal: Int) {
    val infinite = rememberInfiniteTransition(label = "characterTalkScene")
    val talkPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1180), RepeatMode.Restart),
        label = "talkMouthPhase"
    )
    val bodyPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(1560), RepeatMode.Restart),
        label = "talkBodyPhase"
    )
    val phraseShape by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2360), RepeatMode.Restart),
        label = "talkPhraseShape"
    )
    val replay = remember { Animatable(0f) }

    LaunchedEffect(playSignal) {
        replay.snapTo(-0.30f)
        replay.animateTo(1f, tween(180, easing = EaseOutBack))
        replay.animateTo(0f, MotionTokens.gentleSpring)
    }

    val mouth = when (talkPhase) {
        in 0.00f..0.10f -> CharacterMouth.Smile
        in 0.10f..0.26f -> CharacterMouth.SmallOpen
        in 0.26f..0.40f -> CharacterMouth.WideOpen
        in 0.40f..0.54f -> CharacterMouth.SmallOpen
        in 0.54f..0.68f -> CharacterMouth.Ooh
        in 0.68f..0.82f -> CharacterMouth.SmallOpen
        in 0.82f..0.93f -> CharacterMouth.Closed
        else -> CharacterMouth.Smile
    }
    val mouthOpen = when (mouth) {
        CharacterMouth.Closed -> 0.05f
        CharacterMouth.Smile -> 0.18f
        CharacterMouth.SmallOpen -> 0.45f
        CharacterMouth.WideOpen -> 0.82f
        CharacterMouth.Ooh -> 0.62f
    }
    val engaged = if (phraseShape < 0.82f) 1f else (1f - (phraseShape - 0.82f) / 0.18f).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CharacterReactionBackdrop(accent = Color(0xFF8FE3CF), pulse = replay.value.coerceIn(0f, 1f))
        Canvas(Modifier.size(328.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(w * 0.58f, h * 0.16f),
                size = Size(w * 0.28f, h * 0.15f),
                cornerRadius = CornerRadius(w * 0.045f, w * 0.045f)
            )
            val tail = Path().apply {
                moveTo(w * 0.62f, h * 0.30f)
                lineTo(w * 0.54f, h * 0.36f)
                lineTo(w * 0.66f, h * 0.31f)
                close()
            }
            drawPath(tail, Color.White.copy(alpha = 0.92f))
            repeat(3) { index ->
                drawCircle(
                    color = Color(0xFF37B99D).copy(alpha = 0.45f + index * 0.12f),
                    radius = w * 0.012f,
                    center = Offset(w * (0.65f + index * 0.055f), h * 0.235f)
                )
            }
        }
        MakeMistakeCharacter(
            modifier = Modifier.graphicsLayer {
                translationY = sin(bodyPhase) * 3.5f - replay.value.coerceIn(0f, 1f) * 8f
                rotationZ = sin(bodyPhase * 1.3f) * 0.8f
                scaleX = 1f + replay.value.coerceIn(0f, 1f) * 0.014f
                scaleY = 1f - replay.value.coerceAtMost(0f) * 0.020f
            },
            emotion = CharacterEmotion.Happy,
            mouthOverride = mouth,
            headOffsetY = sin(bodyPhase + 0.65f) * 2.3f - replay.value * 2f,
            headRotation = sin(bodyPhase * 1.18f) * 1.15f,
            bodyScaleX = 1f + sin(bodyPhase + 0.8f) * 0.006f,
            bodyScaleY = 1f - sin(bodyPhase + 0.8f) * 0.006f,
            eyeScale = 0.98f + engaged * 0.03f,
            eyeFocusX = sin(bodyPhase * 0.5f) * 0.28f,
            browOffset = -engaged * 1.4f,
            mouthScale = 1f + mouthOpen * 0.08f,
            mouthOpenness = mouthOpen,
            armOffset = sin(bodyPhase + 1.1f) * 0.32f,
            accentIntensity = replay.value.coerceIn(0f, 1f),
            isSpeaking = true,
            speakingIntensity = 0.88f,
            enableIdleBreathing = true,
            enableBlinking = true
        )
    }
}

@Composable
fun CharacterSurprisedReaction(playSignal: Int) {
    val pop = remember { Animatable(0f) }

    LaunchedEffect(playSignal) {
        pop.snapTo(0f)
        pop.animateTo(1f, MotionTokens.surpriseSpec)
        delay(260)
        pop.animateTo(0f, MotionTokens.characterReactionSpec)
    }
    val anticipation = (-pop.value).coerceIn(0f, 1f)
    val surprise = pop.value.coerceIn(0f, 1f)
    val settling = (1f - surprise).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CharacterReactionBackdrop(accent = Color(0xFFFFD166), pulse = surprise)
        Canvas(Modifier.size(328.dp)) {
            val w = size.width
            val h = size.height
            repeat(5) { index ->
                val angle = -140f + index * 70f
                val radians = angle / 180f * PI.toFloat()
                val start = Offset(w * 0.50f + cos(radians) * w * 0.26f, h * 0.45f + sin(radians) * h * 0.19f)
                val end = Offset(w * 0.50f + cos(radians) * w * (0.31f + surprise * 0.03f), h * 0.45f + sin(radians) * h * (0.23f + surprise * 0.03f))
                drawLine(
                    color = Color(0xFFFFD166).copy(alpha = 0.18f + surprise * 0.42f),
                    start = start,
                    end = end,
                    strokeWidth = w * 0.010f,
                    cap = StrokeCap.Round
                )
            }
        }
        MakeMistakeCharacter(
            modifier = Modifier.graphicsLayer {
                translationY = surprise * 12f - anticipation * 5f
                scaleX = 1f - surprise * 0.025f + anticipation * 0.030f
                scaleY = 1f - surprise * 0.025f - anticipation * 0.035f
                rotationZ = -surprise * 1.7f + settling * 0.35f
            },
            emotion = if (surprise > 0.18f) CharacterEmotion.Surprised else CharacterEmotion.Encouraging,
            mouthOverride = if (surprise > 0.24f) CharacterMouth.Ooh else CharacterMouth.Smile,
            surpriseProgress = surprise,
            headOffsetY = -surprise * 5f + anticipation * 3f,
            headRotation = -surprise * 1.1f,
            bodyScaleX = 1f + anticipation * 0.026f - surprise * 0.016f,
            bodyScaleY = 1f - anticipation * 0.030f + surprise * 0.012f,
            eyeScale = 1f + surprise * 0.20f,
            browOffset = -surprise * 3.2f,
            mouthScale = 1f + surprise * 0.18f,
            mouthOpenness = surprise,
            armOffset = surprise * 0.55f,
            accentIntensity = surprise,
            enableIdleBreathing = true,
            enableBlinking = false
        )
    }
}

@Composable
fun CharacterEncouragingNod(playSignal: Int) {
    val nod = remember { Animatable(0f) }
    val sparkle = remember { Animatable(0f) }

    LaunchedEffect(playSignal) {
        sparkle.snapTo(0f)
        nod.snapTo(0f)
        nod.animateTo(1f, MotionTokens.nodSpec)
        delay(120)
        nod.animateTo(-0.16f, tween(150, easing = EaseOutBack))
        nod.animateTo(0f, MotionTokens.gentleSpring)
        sparkle.snapTo(1f)
        sparkle.animateTo(0f, tween(760))
    }
    val nodDown = nod.value.coerceIn(0f, 1f)
    val anticipation = (-nod.value).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CharacterReactionBackdrop(accent = Color(0xFF37B99D), pulse = sparkle.value)
        Canvas(Modifier.size(328.dp)) {
            val w = size.width
            val h = size.height
            repeat(4) { index ->
                val x = w * (0.24f + index * 0.17f)
                val y = h * (0.24f + (index % 2) * 0.08f)
                val r = w * (0.018f + sparkle.value * 0.010f)
                drawLine(
                    color = Color(0xFFFFD166).copy(alpha = 0.34f + sparkle.value * 0.36f),
                    start = Offset(x - r, y),
                    end = Offset(x + r, y),
                    strokeWidth = w * 0.007f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFFD166).copy(alpha = 0.34f + sparkle.value * 0.36f),
                    start = Offset(x, y - r),
                    end = Offset(x, y + r),
                    strokeWidth = w * 0.007f,
                    cap = StrokeCap.Round
                )
            }
        }
        MakeMistakeCharacter(
            modifier = Modifier.graphicsLayer {
                translationY = nodDown * 7f - anticipation * 4f - sparkle.value * 5f
                scaleX = 1f + sparkle.value * 0.012f
                scaleY = 1f - nodDown * 0.010f + sparkle.value * 0.012f
            },
            emotion = CharacterEmotion.Encouraging,
            mouthOverride = CharacterMouth.Smile,
            nodProgress = nodDown,
            headOffsetY = nodDown * 8f - anticipation * 5f,
            headRotation = nodDown * 1.0f - anticipation * 0.55f,
            bodyScaleX = 1f + nodDown * 0.008f,
            bodyScaleY = 1f - nodDown * 0.008f,
            eyeScale = 0.94f,
            browOffset = -sparkle.value * 1.2f,
            mouthScale = 1f + sparkle.value * 0.04f,
            mouthOpenness = 0.22f + sparkle.value * 0.18f,
            armOffset = nodDown * 0.28f,
            accentIntensity = sparkle.value,
            enableIdleBreathing = true,
            enableBlinking = true
        )
    }
}

@Composable
fun CharacterListeningPose(playSignal: Int) {
    val infinite = rememberInfiniteTransition(label = "characterListenScene")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(1320), RepeatMode.Restart),
        label = "listenPhase"
    )
    val replay = remember { Animatable(0f) }

    LaunchedEffect(playSignal) {
        replay.snapTo(1f)
        replay.animateTo(0f, MotionTokens.gentleSpring)
    }

    val level = (sin(phase) * 0.5f + 0.5f).coerceIn(0f, 1f)
    val microReact = (sin(phase * 0.5f + 0.8f) * 0.5f + 0.5f).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CharacterReactionBackdrop(accent = Color(0xFF8FE3CF), pulse = replay.value)
        Canvas(Modifier.size(328.dp)) {
            val w = size.width
            val h = size.height
            val baseX = w * 0.18f
            val baseY = h * 0.68f
            repeat(9) { index ->
                val amp = sin(phase + index * 0.72f) * 0.5f + 0.5f
                val barHeight = h * (0.030f + amp * 0.075f + replay.value * 0.016f)
                drawRoundRect(
                    color = Color(0xFF37B99D).copy(alpha = 0.34f + amp * 0.34f),
                    topLeft = Offset(baseX + index * w * 0.038f, baseY - barHeight),
                    size = Size(w * 0.017f, barHeight),
                    cornerRadius = CornerRadius(w * 0.009f, w * 0.009f)
                )
            }
        }
        MakeMistakeCharacter(
            modifier = Modifier.graphicsLayer {
                translationY = -replay.value * 8f
                scaleX = 1f + level * 0.004f
                scaleY = 1f + level * 0.004f
            },
            emotion = CharacterEmotion.Listening,
            mouthOverride = CharacterMouth.SmallOpen,
            headOffsetY = -microReact * 2.0f,
            headRotation = sin(phase * 0.5f) * 0.50f,
            bodyScaleX = 1f + level * 0.004f,
            bodyScaleY = 1f - level * 0.003f,
            eyeScale = 0.98f,
            eyeFocusX = sin(phase * 0.33f) * 0.20f,
            browOffset = -level * 0.8f,
            mouthScale = 0.92f + level * 0.06f,
            mouthOpenness = 0.18f + level * 0.26f,
            armOffset = level * 0.25f,
            accentIntensity = level,
            isListening = true,
            listeningLevel = level,
            enableIdleBreathing = true,
            enableBlinking = true
        )
    }
}

@Composable
private fun CharacterReactionBackdrop(accent: Color, pulse: Float) {
    Canvas(Modifier.size(328.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.24f + pulse * 0.14f),
                    Color.White.copy(alpha = 0.82f),
                    Color.Transparent
                ),
                center = Offset(w * 0.50f, h * 0.46f),
                radius = w * (0.56f + pulse * 0.08f)
            ),
            topLeft = Offset(w * 0.02f, h * 0.02f),
            size = Size(w * 0.96f, h * 0.92f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.70f),
            topLeft = Offset(w * 0.13f, h * 0.17f),
            size = Size(w * 0.74f, h * 0.66f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        repeat(5) { index ->
            drawCircle(
                color = listOf(Color(0xFF8FE3CF), Color(0xFFFFD166), Color(0xFFFFB6A8))[index % 3].copy(alpha = 0.28f),
                radius = w * (0.010f + index % 2 * 0.004f),
                center = Offset(w * (0.20f + index * 0.15f), h * 0.22f)
            )
        }
    }
}
