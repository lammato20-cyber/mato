package com.example.ui.games

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

private enum class SimonPhase { IDLE, SHOWING_SEQUENCE, USER_TURN, GAME_OVER }

@Composable
fun SimonPulseGame(
    bestScore: Int,
    onScoreUpdated: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val padColors = remember { listOf(PulseCoral, ArcadeMint, StreamCyan, NeonAmber) }
    val padLabels = remember { listOf("PULSE", "MINT", "CYAN", "GOLD") }
    val padFreqs = remember { listOf(523.25, 659.25, 783.99, 1046.50) }

    var sequence by remember { mutableStateOf<List<Int>>(emptyList()) }
    var userIndex by remember { mutableIntStateOf(0) }
    var activePad by remember { mutableStateOf<Int?>(null) }
    var phase by remember { mutableStateOf(SimonPhase.IDLE) }
    var roundScore by remember { mutableIntStateOf(0) }

    fun playTone(padIndex: Int, durationMs: Int = 220) {
        scope.launch(Dispatchers.Default) {
            runCatching {
                val sampleRate = 22050
                val count = (sampleRate * durationMs) / 1000
                val samples = ShortArray(count)
                val freq = padFreqs[padIndex]
                for (i in 0 until count) {
                    val env = if (i < 300) i / 300f else if (i > count - 600) (count - i) / 600f else 1f
                    samples[i] = (sin(2.0 * Math.PI * i * freq / sampleRate) * 0.45 * env * Short.MAX_VALUE).toInt().toShort()
                }
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                delay(durationMs.toLong() + 40L)
                track.stop()
                track.release()
            }
        }
    }

    fun startNewGame() {
        roundScore = 0
        userIndex = 0
        sequence = listOf(Random.nextInt(4))
        phase = SimonPhase.SHOWING_SEQUENCE
    }

    LaunchedEffect(phase, sequence) {
        if (phase == SimonPhase.SHOWING_SEQUENCE) {
            delay(450L)
            val stepDelay = (360L - (sequence.size * 12L)).coerceAtLeast(170L)
            for (pad in sequence) {
                activePad = pad
                playTone(pad, stepDelay.toInt())
                delay(stepDelay)
                activePad = null
                delay(140L)
            }
            userIndex = 0
            phase = SimonPhase.USER_TURN
        }
    }

    fun onPadPressed(pad: Int) {
        if (phase != SimonPhase.USER_TURN) return
        scope.launch {
            activePad = pad
            playTone(pad, 180)
            delay(180L)
            if (activePad == pad) activePad = null
        }

        if (pad == sequence[userIndex]) {
            val nextIdx = userIndex + 1
            if (nextIdx >= sequence.size) {
                roundScore = sequence.size
                onScoreUpdated(roundScore)
                sequence = sequence + Random.nextInt(4)
                phase = SimonPhase.SHOWING_SEQUENCE
            } else {
                userIndex = nextIdx
            }
        } else {
            phase = SimonPhase.GAME_OVER
            onScoreUpdated(roundScore)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScorePill(label = "STREAK", value = roundScore, accent = PulseCoral)
            ScorePill(label = "BEST", value = maxOf(bestScore, roundScore), accent = ArcadeMint)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when (phase) {
                    SimonPhase.IDLE -> "READY TO PLAY"
                    SimonPhase.SHOWING_SEQUENCE -> "WATCH & LISTEN..."
                    SimonPhase.USER_TURN -> "YOUR TURN (${userIndex + 1}/${sequence.size})"
                    SimonPhase.GAME_OVER -> "MISSED BEAT!"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = when (phase) {
                    SimonPhase.USER_TURN -> ArcadeMint
                    SimonPhase.GAME_OVER -> PulseCoral
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the glowing synth pads in the exact order",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            for (row in 0..1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    for (col in 0..1) {
                        val idx = row * 2 + col
                        val isLit = activePad == idx
                        val baseColor = padColors[idx]
                        val bgColor by animateColorAsState(
                            targetValue = if (isLit) baseColor else baseColor.copy(alpha = 0.24f),
                            animationSpec = tween(120),
                            label = "pad_$idx"
                        )
                        Surface(
                            onClick = { onPadPressed(idx) },
                            color = bgColor,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .border(
                                    width = if (isLit) 3.dp else 1.5.dp,
                                    color = baseColor,
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .testTag("simon_pad_$idx")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = padLabels[idx],
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isLit) Color.Black else baseColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { startNewGame() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("simon_start_button")
        ) {
            Text(
                text = if (phase == SimonPhase.IDLE) "Start Sequence" else "Restart Game",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
