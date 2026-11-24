package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.player.EqPreset
import com.example.player.EqualizerSettings
import com.example.player.ReverbOption
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EqualizerSheet(
    settings: EqualizerSettings,
    isPlaying: Boolean,
    trackTitle: String?,
    accentColor: Color,
    onSetEnabled: (Boolean) -> Unit,
    onSetPreset: (EqPreset) -> Unit,
    onSetBandLevel: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onSetLoudnessGain: (Int) -> Unit,
    onSetReverb: (ReverbOption) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val activeAccent = if (settings.enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            activeAccent.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                // Studio Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = activeAccent.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = activeAccent
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Studio Equalizer",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val badgeText = when {
                                    !settings.enabled -> "BYPASS"
                                    settings.hardwareAttached && isPlaying -> "LIVE FX"
                                    else -> "ARMED"
                                }
                                val badgeColor = when {
                                    !settings.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                    settings.hardwareAttached && isPlaying -> ArcadeMint
                                    else -> NeonAmber
                                }
                                Surface(
                                    color = badgeColor.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = trackTitle ?: "Applies automatically on next playback",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = onSetEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = activeAccent
                            ),
                            modifier = Modifier.testTag("eq_power_switch")
                        )
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.testTag("eq_reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Reset equalizer to flat"
                            )
                        }
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.testTag("eq_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close equalizer"
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Frequency Response Curve Card
                    EqResponseCurveCard(
                        settings = settings,
                        isPlaying = isPlaying,
                        accentColor = activeAccent
                    )

                    // Presets Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SOUND PROFILES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EqPreset.entries.forEach { preset ->
                                val selected = settings.preset == preset
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSetPreset(preset) },
                                    label = { Text(preset.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = activeAccent.copy(alpha = 0.24f),
                                        selectedLabelColor = activeAccent
                                    ),
                                    modifier = Modifier.testTag("eq_preset_${preset.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // 5-Band Studio Fader Console
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                activeAccent.copy(alpha = if (settings.enabled) 0.35f else 0.14f),
                                RoundedCornerShape(22.dp)
                            )
                            .alpha(if (settings.enabled) 1f else 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Tune,
                                        contentDescription = null,
                                        tint = activeAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GRAPHIC BANDS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${formatDb(settings.minBandMb)} to ${formatDb(settings.maxBandMb)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                settings.bandLevelsMb.forEachIndexed { index, levelMb ->
                                    val freqLabel = settings.bandLabels.getOrElse(index) { "B${index + 1}" }
                                    VerticalEqFader(
                                        bandIndex = index,
                                        freqLabel = freqLabel,
                                        levelMb = levelMb,
                                        minMb = settings.minBandMb,
                                        maxMb = settings.maxBandMb,
                                        enabled = settings.enabled,
                                        accentColor = activeAccent,
                                        onChangeLevel = { newMb -> onSetBandLevel(index, newMb) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }

                    // Spatial & Bass FX Rack
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                RoundedCornerShape(22.dp)
                            )
                            .alpha(if (settings.enabled) 1f else 0.55f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.SurroundSound,
                                    contentDescription = null,
                                    tint = StreamCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HARDWARE AUDIO FX",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RotaryFxDialCard(
                                    title = "Bass Boost",
                                    value = settings.bassBoostStrength,
                                    maxValue = 1000,
                                    accentColor = PulseCoral,
                                    enabled = settings.enabled,
                                    onValueChange = onSetBassBoost,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("eq_bass_dial")
                                )
                                RotaryFxDialCard(
                                    title = "3D Surround",
                                    value = settings.virtualizerStrength,
                                    maxValue = 1000,
                                    accentColor = StreamCyan,
                                    enabled = settings.enabled,
                                    onValueChange = onSetVirtualizer,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("eq_virtualizer_dial")
                                )
                            }

                            // Preamp / Loudness Boost Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Preamp / Loudness Boost",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = formatDb(settings.loudnessGainMb),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ArcadeMint
                                    )
                                }
                                Slider(
                                    value = settings.loudnessGainMb.toFloat(),
                                    onValueChange = { onSetLoudnessGain(it.roundToInt()) },
                                    valueRange = 0f..1200f,
                                    enabled = settings.enabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ArcadeMint,
                                        activeTrackColor = ArcadeMint
                                    ),
                                    modifier = Modifier.testTag("eq_loudness_slider")
                                )
                            }

                            // Room / Reverb Presets
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Acoustic Space Reverb",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReverbOption.entries.forEach { option ->
                                        val selected = settings.reverb == option
                                        FilterChip(
                                            selected = selected,
                                            enabled = settings.enabled,
                                            onClick = { onSetReverb(option) },
                                            label = { Text(option.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonAmber.copy(alpha = 0.22f),
                                                selectedLabelColor = NeonAmber
                                            ),
                                            modifier = Modifier.testTag("eq_reverb_${option.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EqResponseCurveCard(
    settings: EqualizerSettings,
    isPlaying: Boolean,
    accentColor: Color
) {
    val shimmerTransition = rememberInfiniteTransition(label = "eq_curve_pulse")
    val pulsePhase by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    val animatedNormalized = settings.bandLevelsMb.map { mb ->
        val range = (settings.maxBandMb - settings.minBandMb).coerceAtLeast(1).toFloat()
        val norm = ((mb - settings.minBandMb).toFloat() / range).coerceIn(0f, 1f)
        val animated by animateFloatAsState(
            targetValue = if (settings.enabled) norm else 0.5f,
            animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
            label = "band_anim"
        )
        animated
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val zeroLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                accentColor.copy(alpha = 0.32f),
                RoundedCornerShape(20.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FREQUENCY RESPONSE CURVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = settings.preset.label.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) {
                val padH = 18f
                val padV = 14f
                val plotW = size.width - padH * 2f
                val plotH = size.height - padV * 2f

                // Horizontal dB gridlines (+max, +half, 0, -half, -max)
                for (step in 0..4) {
                    val y = padV + (plotH * step / 4f)
                    drawLine(
                        color = if (step == 2) zeroLineColor else gridColor,
                        start = Offset(padH, y),
                        end = Offset(size.width - padH, y),
                        strokeWidth = if (step == 2) 2f else 1f
                    )
                }

                // Subtle live harmonic analyzer bars behind the curve when playing
                if (isPlaying && settings.enabled) {
                    val barCount = 24
                    val stepW = plotW / barCount
                    for (i in 0 until barCount) {
                        val bandApprox = ((i.toFloat() / (barCount - 1)) * (animatedNormalized.size - 1))
                        val lo = bandApprox.toInt().coerceIn(0, animatedNormalized.lastIndex)
                        val hi = (lo + 1).coerceAtMost(animatedNormalized.lastIndex)
                        val frac = bandApprox - lo
                        val gain = animatedNormalized[lo] * (1f - frac) + animatedNormalized[hi] * frac
                        val wave = (0.35f + 0.65f * ((sin(pulsePhase + i * 0.65f) + 1f) * 0.5f))
                        val barHeight = (plotH * 0.55f) * (0.25f + gain * 0.75f) * wave
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.14f),
                            topLeft = Offset(padH + i * stepW + 2f, size.height - padV - barHeight),
                            size = Size((stepW - 4f).coerceAtLeast(3f), barHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }

                val points = animatedNormalized.mapIndexed { idx, norm ->
                    val x = if (animatedNormalized.size <= 1) padH + plotW * 0.5f
                    else padH + plotW * (idx.toFloat() / (animatedNormalized.size - 1))
                    val y = padV + plotH * (1f - norm)
                    Offset(x, y)
                }

                // Vertical band guide lines
                points.forEach { pt ->
                    drawLine(
                        color = gridColor.copy(alpha = 0.45f),
                        start = Offset(pt.x, padV),
                        end = Offset(pt.x, size.height - padV),
                        strokeWidth = 1f
                    )
                }

                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.lastIndex) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val midX = (p0.x + p1.x) * 0.5f
                            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, size.height - padV)
                        lineTo(points.first().x, size.height - padV)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.36f),
                                accentColor.copy(alpha = 0.02f)
                            ),
                            startY = padV,
                            endY = size.height - padV
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = accentColor,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                }

                // Nodes
                points.forEach { pt ->
                    drawCircle(
                        color = accentColor.copy(alpha = 0.28f),
                        radius = 12f,
                        center = pt
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = 7f,
                        center = pt
                    )
                    drawCircle(
                        color = accentColor,
                        radius = 4.5f,
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalEqFader(
    bandIndex: Int,
    freqLabel: String,
    levelMb: Int,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    accentColor: Color,
    onChangeLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val range = (maxMb - minMb).coerceAtLeast(1).toFloat()
    val norm = ((levelMb - minMb).toFloat() / range).coerceIn(0f, 1f)
    val zeroNorm = ((0 - minMb).toFloat() / range).coerceIn(0f, 1f)
    val isBoost = levelMb >= 0
    val barColor = if (isBoost) accentColor else StreamCyan
    val trackBg = MaterialTheme.colorScheme.surface
    val centerMarkColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    fun yToMb(y: Float, height: Float): Int {
        val pad = 16f
        val usable = (height - pad * 2f).coerceAtLeast(1f)
        val frac = (1f - ((y - pad) / usable)).coerceIn(0f, 1f)
        val raw = (minMb + frac * range).roundToInt()
        // Snap near 0 dB within +/- 60 mB for crisp zeroing
        return if (kotlin.math.abs(raw) <= 60) 0 else raw.coerceIn(minMb, maxMb)
    }

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .testTag("eq_band_fader_$bandIndex"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDbCompact(levelMb),
            style = MaterialTheme.typography.labelSmall,
            color = if (levelMb == 0) MaterialTheme.colorScheme.onSurfaceVariant else barColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .pointerInput(enabled, minMb, maxMb, bandIndex) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        onChangeLevel(yToMb(offset.y, size.height.toFloat()))
                    }
                }
                .pointerInput(enabled, minMb, maxMb, bandIndex) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        onChangeLevel(yToMb(change.position.y, size.height.toFloat()))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pad = 16f
                val usableH = (size.height - pad * 2f).coerceAtLeast(1f)
                val cx = size.width * 0.5f
                val trackW = 10.dp.toPx()

                // Base recessed rail
                drawRoundRect(
                    color = trackBg,
                    topLeft = Offset(cx - trackW * 0.5f, pad),
                    size = Size(trackW, usableH),
                    cornerRadius = CornerRadius(trackW * 0.5f, trackW * 0.5f)
                )

                val zeroY = pad + usableH * (1f - zeroNorm)
                val thumbY = pad + usableH * (1f - norm)

                // Zero center tick
                drawLine(
                    color = centerMarkColor,
                    start = Offset(cx - 14.dp.toPx(), zeroY),
                    end = Offset(cx + 14.dp.toPx(), zeroY),
                    strokeWidth = 2f
                )

                // Bipolar fill from 0 dB to thumbY
                val topY = minOf(zeroY, thumbY)
                val fillH = kotlin.math.abs(zeroY - thumbY)
                if (fillH > 1f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(cx - trackW * 0.5f, topY),
                        size = Size(trackW, fillH),
                        cornerRadius = CornerRadius(trackW * 0.5f, trackW * 0.5f)
                    )
                }

                // Thumb cap
                val capW = 28.dp.toPx()
                val capH = 16.dp.toPx()
                drawCircle(
                    color = barColor.copy(alpha = 0.25f),
                    radius = 18.dp.toPx(),
                    center = Offset(cx, thumbY)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(cx - capW * 0.5f, thumbY - capH * 0.5f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawLine(
                    color = barColor,
                    start = Offset(cx - capW * 0.3f, thumbY),
                    end = Offset(cx + capW * 0.3f, thumbY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = freqLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun RotaryFxDialCard(
    title: String,
    value: Int,
    maxValue: Int,
    accentColor: Color,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = ((value.toFloat() / maxValue.coerceAtLeast(1)) * 100f).roundToInt().coerceIn(0, 100)
    val sweepFraction by animateFloatAsState(
        targetValue = (value.toFloat() / maxValue.coerceAtLeast(1)).coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 500f),
        label = "dial_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.surface

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .pointerInput(enabled, value) {
                        if (!enabled) return@pointerInput
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = ((-dragAmount.y + dragAmount.x) * 3.5f).roundToInt()
                            onValueChange((value + delta).coerceIn(0, maxValue))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 9.dp.toPx()
                    val pad = stroke * 0.5f + 4f
                    val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
                    val topLeft = Offset(pad, pad)

                    drawArc(
                        color = trackColor,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    if (sweepFraction > 0.005f) {
                        drawArc(
                            color = accentColor,
                            startAngle = 135f,
                            sweepAngle = 270f * sweepFraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (pct > 0) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { if (enabled) onValueChange((value - 100).coerceAtLeast(0)) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease $title",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Surface(
                    onClick = { if (enabled) onValueChange((value + 100).coerceAtMost(maxValue)) },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase $title",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDb(mb: Int): String {
    val db = mb / 100.0
    val prefix = if (mb > 0) "+" else ""
    return String.format(Locale.US, "%s%.1f dB", prefix, db)
}

private fun formatDbCompact(mb: Int): String {
    val db = mb / 100.0
    val prefix = if (mb > 0) "+" else ""
    return String.format(Locale.US, "%s%.1f", prefix, db)
}
