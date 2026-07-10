package com.lexaprograms.polishcards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

enum class CharacterEmotion {
    Neutral,
    Happy,
    Encouraging,
    Surprised,
    Listening,
    Thinking,
    SadSoft
}

enum class CharacterMouth {
    Closed,
    Smile,
    SmallOpen,
    WideOpen,
    Ooh
}

data class CharacterMotionState(
    val emotion: CharacterEmotion,
    val mouth: CharacterMouth,
    val speakingProgress: Float,
    val listeningLevel: Float,
    val isBlinking: Boolean,
    val nodProgress: Float,
    val surpriseProgress: Float,
    val headOffsetY: Float,
    val headRotation: Float,
    val eyeScale: Float,
    val eyeFocusX: Float,
    val browOffset: Float,
    val mouthScale: Float,
    val mouthOpenness: Float,
    val armOffset: Float,
    val accentIntensity: Float
)

data class CharacterPack(
    val bodyTop: Color,
    val bodyBottom: Color,
    val face: Color,
    val cheek: Color,
    val ink: Color,
    val accent: Color,
    val shadow: Color
)

val DefaultCharacterPack = CharacterPack(
    bodyTop = Color(0xFF8FE3CF),
    bodyBottom = Color(0xFF37B99D),
    face = Color(0xFFFFF8DF),
    cheek = Color(0xFFFFB6A8),
    ink = Color(0xFF26322F),
    accent = Color(0xFFFFD166),
    shadow = Color(0xFF17201E)
)

@Composable
fun MakeMistakeCharacter(
    modifier: Modifier = Modifier,
    emotion: CharacterEmotion = CharacterEmotion.Neutral,
    mouthOverride: CharacterMouth? = null,
    isSpeaking: Boolean = false,
    isListening: Boolean = false,
    speakingIntensity: Float = 0f,
    listeningLevel: Float = 0f,
    nodProgress: Float = 0f,
    surpriseProgress: Float = 0f,
    headOffsetY: Float = 0f,
    headRotation: Float = 0f,
    bodyScaleX: Float = 1f,
    bodyScaleY: Float = 1f,
    eyeScale: Float = 1f,
    eyeFocusX: Float = 0f,
    browOffset: Float = 0f,
    mouthScale: Float = 1f,
    mouthOpenness: Float = 0f,
    armOffset: Float = 0f,
    accentIntensity: Float = 0f,
    enableIdleBreathing: Boolean = true,
    enableBlinking: Boolean = true,
    characterPack: CharacterPack = DefaultCharacterPack
) {
    val idle = rememberInfiniteTransition(label = "characterIdle")
    val breath by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "characterBreath"
    )
    val floatPhase by idle.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Restart),
        label = "characterFloat"
    )
    val talkPhase by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
        label = "characterTalk"
    )
    val blink = remember { Animatable(0f) }

    LaunchedEffect(enableBlinking) {
        blink.snapTo(0f)
        if (enableBlinking) {
            while (true) {
                delay(2600)
                blink.animateTo(1f, MotionTokens.blinkCloseSpec)
                blink.animateTo(0f, MotionTokens.blinkOpenSpec)
                delay(1700)
                blink.animateTo(1f, MotionTokens.blinkCloseSpec)
                blink.animateTo(0f, MotionTokens.blinkOpenSpec)
            }
        }
    }

    val resolvedSurprise = surpriseProgress.coerceIn(0f, 1f)
        .coerceAtLeast(if (emotion == CharacterEmotion.Surprised) 1f else 0f)
    val resolvedNod = nodProgress.coerceIn(0f, 1f)
        .coerceAtLeast(if (emotion == CharacterEmotion.Encouraging) breath else 0f)
    val mouth = mouthOverride ?: when {
        isListening -> CharacterMouth.SmallOpen
        isSpeaking && speakingIntensity > 0.70f && talkPhase > 0.45f -> CharacterMouth.WideOpen
        isSpeaking && talkPhase > 0.45f -> CharacterMouth.SmallOpen
        resolvedSurprise > 0.35f -> CharacterMouth.Ooh
        emotion == CharacterEmotion.SadSoft -> CharacterMouth.Closed
        else -> CharacterMouth.Smile
    }
    val state = CharacterMotionState(
        emotion = emotion,
        mouth = mouth,
        speakingProgress = if (isSpeaking) talkPhase * speakingIntensity.coerceIn(0f, 1f) else 0f,
        listeningLevel = listeningLevel.coerceIn(0f, 1f),
        isBlinking = enableBlinking && blink.value > 0.5f,
        nodProgress = resolvedNod,
        surpriseProgress = resolvedSurprise,
        headOffsetY = headOffsetY,
        headRotation = headRotation,
        eyeScale = eyeScale,
        eyeFocusX = eyeFocusX.coerceIn(-1f, 1f),
        browOffset = browOffset,
        mouthScale = mouthScale,
        mouthOpenness = mouthOpenness.coerceIn(0f, 1f),
        armOffset = armOffset,
        accentIntensity = accentIntensity.coerceIn(0f, 1f)
    )

    CharacterCanvas(
        modifier = modifier.size(286.dp),
        state = state,
        pack = characterPack,
        breath = if (enableIdleBreathing) breath else 0f,
        floatPhase = if (enableIdleBreathing) floatPhase else 0f,
        bodyScaleX = bodyScaleX,
        bodyScaleY = bodyScaleY
    )
}

@Composable
private fun CharacterCanvas(
    modifier: Modifier,
    state: CharacterMotionState,
    pack: CharacterPack,
    breath: Float,
    floatPhase: Float,
    bodyScaleX: Float,
    bodyScaleY: Float
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w * 0.5f, h * 0.54f)
        val bob = sin(floatPhase) * h * 0.018f
        val scaleX = 1f + breath * 0.012f
        val scaleY = 1f - breath * 0.008f

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(pack.shadow.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.88f),
                radius = w * 0.32f
            ),
            topLeft = Offset(w * 0.22f, h * 0.81f),
            size = Size(w * 0.56f, h * 0.10f)
        )

        translate(top = bob) {
            scale(scaleX = scaleX * bodyScaleX, scaleY = scaleY * bodyScaleY, pivot = c) {
                drawCharacterBody(pack)
                drawCharacterArms(pack, breath, state.armOffset)
                drawCharacterFace(pack, state, breath)
                if (state.emotion == CharacterEmotion.Listening || state.listeningLevel > 0f) {
                    drawListeningMarks(pack, state.listeningLevel, state.accentIntensity)
                }
                if (state.emotion == CharacterEmotion.Thinking) {
                    drawThinkingDot(pack, breath)
                }
            }
        }
    }
}

private fun DrawScope.drawCharacterBody(pack: CharacterPack) {
    val w = size.width
    val h = size.height
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(pack.bodyTop, pack.bodyBottom),
            startY = h * 0.27f,
            endY = h * 0.83f
        ),
        topLeft = Offset(w * 0.23f, h * 0.25f),
        size = Size(w * 0.54f, h * 0.58f),
        cornerRadius = CornerRadius(w * 0.24f, w * 0.24f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.26f),
        topLeft = Offset(w * 0.32f, h * 0.32f),
        size = Size(w * 0.16f, h * 0.09f),
        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
    )
    drawCircle(pack.accent, radius = w * 0.035f, center = Offset(w * 0.62f, h * 0.75f))
}

private fun DrawScope.drawCharacterArms(pack: CharacterPack, breath: Float, armOffset: Float) {
    val w = size.width
    val h = size.height
    rotate(degrees = -7f - breath * 3f - armOffset * 4f, pivot = Offset(w * 0.27f, h * 0.55f)) {
        drawRoundRect(
            color = pack.bodyBottom,
            topLeft = Offset(w * 0.11f, h * (0.52f + armOffset * 0.012f)),
            size = Size(w * 0.20f, h * 0.075f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
    }
    rotate(degrees = 8f + breath * 3f + armOffset * 4f, pivot = Offset(w * 0.73f, h * 0.55f)) {
        drawRoundRect(
            color = pack.bodyBottom,
            topLeft = Offset(w * 0.69f, h * (0.52f + armOffset * 0.012f)),
            size = Size(w * 0.20f, h * 0.075f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
    }
}

private fun DrawScope.drawCharacterFace(pack: CharacterPack, state: CharacterMotionState, breath: Float) {
    val w = size.width
    val h = size.height
    val faceTop = h * (0.30f + state.nodProgress * 0.012f - state.surpriseProgress * 0.010f) + state.headOffsetY
    val pivot = Offset(w * 0.50f, h * 0.47f + state.headOffsetY)
    rotate(degrees = state.headRotation, pivot = pivot) {
        drawRoundRect(
            color = pack.face,
            topLeft = Offset(w * 0.285f, faceTop),
            size = Size(w * 0.43f, h * 0.34f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
        )
        drawCircle(pack.cheek.copy(alpha = 0.42f), radius = w * 0.029f, center = Offset(w * 0.38f, h * 0.50f + state.headOffsetY))
        drawCircle(pack.cheek.copy(alpha = 0.42f), radius = w * 0.029f, center = Offset(w * 0.62f, h * 0.50f + state.headOffsetY))

        drawEyebrows(pack, state)
        drawEyes(pack, state)
        drawMouth(pack, state)

        drawCircle(
            color = Color.White.copy(alpha = 0.28f),
            radius = w * (0.018f + breath * 0.004f),
            center = Offset(w * 0.42f, h * 0.37f + state.headOffsetY)
        )
    }
}

private fun DrawScope.drawEyebrows(pack: CharacterPack, state: CharacterMotionState) {
    val w = size.width
    val h = size.height
    val eyebrowLift = when (state.emotion) {
        CharacterEmotion.Surprised -> h * 0.018f
        CharacterEmotion.Encouraging -> h * 0.006f
        CharacterEmotion.SadSoft -> -h * 0.002f
        else -> 0f
    } + h * 0.018f * state.surpriseProgress + state.browOffset
    val leftTilt = if (state.emotion == CharacterEmotion.SadSoft) 8f else -8f
    val rightTilt = if (state.emotion == CharacterEmotion.SadSoft) -8f else 8f
    rotate(leftTilt, Offset(w * 0.41f, h * 0.405f + state.headOffsetY - eyebrowLift)) {
        drawLine(
            color = pack.ink,
            start = Offset(w * 0.36f, h * 0.405f + state.headOffsetY - eyebrowLift),
            end = Offset(w * 0.45f, h * 0.405f + state.headOffsetY - eyebrowLift),
            strokeWidth = w * 0.012f,
            cap = StrokeCap.Round
        )
    }
    rotate(rightTilt, Offset(w * 0.59f, h * 0.405f + state.headOffsetY - eyebrowLift)) {
        drawLine(
            color = pack.ink,
            start = Offset(w * 0.55f, h * 0.405f + state.headOffsetY - eyebrowLift),
            end = Offset(w * 0.64f, h * 0.405f + state.headOffsetY - eyebrowLift),
            strokeWidth = w * 0.012f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawEyes(pack: CharacterPack, state: CharacterMotionState) {
    val w = size.width
    val h = size.height
    val eyeY = h * 0.445f + state.headOffsetY
    val eyeRadius = w * (0.025f + 0.007f * state.surpriseProgress) * state.eyeScale.coerceIn(0.72f, 1.28f)
    val focusX = w * 0.010f * state.eyeFocusX

    if (state.isBlinking) {
        drawLine(pack.ink, Offset(w * 0.37f, eyeY), Offset(w * 0.45f, eyeY), w * 0.012f, StrokeCap.Round)
        drawLine(pack.ink, Offset(w * 0.55f, eyeY), Offset(w * 0.63f, eyeY), w * 0.012f, StrokeCap.Round)
    } else {
        drawCircle(pack.ink, eyeRadius, Offset(w * 0.41f + focusX, eyeY))
        drawCircle(pack.ink, eyeRadius, Offset(w * 0.59f + focusX, eyeY))
        drawCircle(Color.White.copy(alpha = 0.86f), eyeRadius * 0.30f, Offset(w * 0.402f + focusX, eyeY - eyeRadius * 0.34f))
        drawCircle(Color.White.copy(alpha = 0.86f), eyeRadius * 0.30f, Offset(w * 0.582f + focusX, eyeY - eyeRadius * 0.34f))
    }
}

private fun DrawScope.drawMouth(pack: CharacterPack, state: CharacterMotionState) {
    val w = size.width
    val h = size.height
    val y = h * 0.545f + state.headOffsetY
    val mouthScale = state.mouthScale.coerceIn(0.75f, 1.35f)
    val openness = state.mouthOpenness
    when (state.mouth) {
        CharacterMouth.Closed -> drawLine(
            color = pack.ink,
            start = Offset(w * (0.455f - openness * 0.010f), y),
            end = Offset(w * (0.545f + openness * 0.010f), y),
            strokeWidth = w * 0.012f,
            cap = StrokeCap.Round
        )
        CharacterMouth.Smile -> {
            val path = Path().apply {
                moveTo(w * (0.435f - (mouthScale - 1f) * 0.020f), y - h * 0.004f)
                quadraticBezierTo(w * 0.50f, y + h * (0.055f + openness * 0.018f), w * (0.565f + (mouthScale - 1f) * 0.020f), y - h * 0.004f)
            }
            drawPath(path, pack.ink, style = Stroke(width = w * 0.014f, cap = StrokeCap.Round))
        }
        CharacterMouth.SmallOpen -> drawOval(
            color = pack.ink,
            topLeft = Offset(w * (0.50f - 0.030f * mouthScale), y - h * 0.008f),
            size = Size(w * 0.06f * mouthScale, h * (0.036f + state.speakingProgress * 0.025f + openness * 0.020f))
        )
        CharacterMouth.WideOpen -> drawRoundRect(
            color = pack.ink,
            topLeft = Offset(w * (0.50f - 0.045f * mouthScale), y - h * 0.012f),
            size = Size(w * 0.09f * mouthScale, h * (0.070f + openness * 0.024f)),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
        )
        CharacterMouth.Ooh -> drawCircle(pack.ink, radius = w * (0.028f + openness * 0.010f) * mouthScale, center = Offset(w * 0.5f, y + h * 0.010f))
    }
}

private fun DrawScope.drawListeningMarks(pack: CharacterPack, listeningLevel: Float, accentIntensity: Float) {
    val w = size.width
    val h = size.height
    val level = listeningLevel.coerceIn(0f, 1f)
    val accent = accentIntensity.coerceIn(0f, 1f)
    repeat(3) { index ->
        val radius = w * (0.34f + index * 0.055f + level * 0.020f + accent * 0.020f)
        drawArc(
            color = pack.accent.copy(alpha = 0.32f - index * 0.08f + level * 0.12f + accent * 0.20f),
            startAngle = -36f,
            sweepAngle = 72f,
            useCenter = false,
            topLeft = Offset(w * 0.5f - radius, h * 0.47f - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = w * 0.010f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawThinkingDot(pack: CharacterPack, breath: Float) {
    val w = size.width
    val h = size.height
    repeat(3) { index ->
        val lift = sin(breath * PI.toFloat() + index) * h * 0.008f
        drawCircle(
            color = pack.accent.copy(alpha = 0.45f + index * 0.12f),
            radius = w * (0.012f + index * 0.004f),
            center = Offset(w * (0.67f + index * 0.045f), h * (0.29f - index * 0.045f) + lift)
        )
    }
}
