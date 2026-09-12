package hu.nova.mobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import hu.nova.mobile.R
import hu.nova.mobile.domain.model.VoiceState
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource

@Composable
fun VoiceStateIndicator(voiceState: VoiceState, modifier: Modifier = Modifier) {
    val (label, color) = when (voiceState) {
        VoiceState.IDLE -> stringResource(R.string.home_status_idle) to MaterialTheme.colorScheme.onSurfaceVariant
        VoiceState.LISTENING -> stringResource(R.string.home_status_listening) to MaterialTheme.colorScheme.primary
        VoiceState.THINKING -> stringResource(R.string.home_status_thinking) to MaterialTheme.colorScheme.secondary
        VoiceState.SPEAKING -> stringResource(R.string.home_status_speaking) to MaterialTheme.colorScheme.primary
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "  $label",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
