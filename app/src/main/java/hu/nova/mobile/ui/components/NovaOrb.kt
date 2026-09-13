package hu.nova.mobile.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hu.nova.mobile.domain.model.VoiceState

/**
 * NOVA's animated orb, the centerpiece of the Home screen. Pulses gently when idle,
 * pulses faster while listening, rotates while thinking, and ripples while speaking -
 * a single, restrained animation per state rather than layered neon glow effects.
 */
@Composable
fun NovaOrb(
    voiceState: VoiceState,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val transition = rememberInfiniteTransition(label = "nova-orb")

    val pulseDurationMs = when (voiceState) {
        VoiceState.IDLE -> 2800
        VoiceState.LISTENING -> 900
        VoiceState.THINKING -> 1200
        VoiceState.SPEAKING -> 700
    }

    val pulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (voiceState == VoiceState.THINKING) 1400 else 6000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier.size(size)) {
        val radius = (this.size.minDimension / 2f) * pulse
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        val brush = Brush.sweepGradient(
            colors = listOf(primary, secondary, primary),
        )

        rotate(degrees = rotation) {
            drawCircle(brush = brush, radius = radius, center = center)
        }
        drawCircle(color = primary.copy(alpha = 0.15f), radius = radius * 1.25f, center = center)
    }
}
