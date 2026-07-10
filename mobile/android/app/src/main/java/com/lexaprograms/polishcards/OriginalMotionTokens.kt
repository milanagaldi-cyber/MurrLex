package com.lexaprograms.polishcards

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.DurationBasedAnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object MotionTokens {
    val softSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val snappySpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val gentleSpring: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val characterIdleSpec: DurationBasedAnimationSpec<Float> = tween(
        durationMillis = 2400,
        easing = EaseInOutCubic
    )

    val characterReactionSpec: DurationBasedAnimationSpec<Float> = tween(
        durationMillis = 640,
        easing = EaseOutCubic
    )

    val characterMouthSpec: DurationBasedAnimationSpec<Float> = tween(
        durationMillis = 1180,
        easing = EaseInOutCubic
    )

    val blinkCloseSpec: DurationBasedAnimationSpec<Float> = tween(
        durationMillis = 54,
        easing = EaseOutCubic
    )

    val blinkOpenSpec: DurationBasedAnimationSpec<Float> = tween(
        durationMillis = 112,
        easing = EaseOutCubic
    )

    val blinkSpec: DurationBasedAnimationSpec<Float> = keyframes {
        durationMillis = 166
        0f at 0
        1f at 54 using EaseOutCubic
        0f at 166 using EaseOutCubic
    }

    val nodSpec: DurationBasedAnimationSpec<Float> = keyframes {
        durationMillis = 360
        0f at 0
        -0.22f at 110 using EaseOutCubic
        1f at 280 using EaseOutCubic
        1f at 360 using EaseOutBack
    }

    val surpriseSpec: DurationBasedAnimationSpec<Float> = keyframes {
        durationMillis = 270
        0f at 0
        -0.18f at 105 using EaseInOutCubic
        1f at 245 using EaseOutBack
        1f at 270 using EaseOutBack
    }
}
