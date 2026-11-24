package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan
import kotlinx.coroutines.delay

private data class Brick(
    val row: Int,
    val col: Int,
    val hp: Int,
    val color: Color
)

@Composable
fun BrickBreakerGame(
    bestScore: Int,
    onScoreUpdated: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = 5
    val cols = 7
    val paddleWidth = 0.24f
    val paddleHeight = 0.025f
    val paddleY = 0.90f
    val ballRadius = 0.018f

    fun buildBricks(levelNum: Int): List<Brick> {
        val palette = listOf(PulseCoral, NeonAmber, ArcadeMint, StreamCyan, Color(0xFFA855F7))
        return buildList {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val hp = if (r == 0 && levelNum > 1) 2 else if (r == 0) 2 else 1
                    add(Brick(r, c, hp, palette[r % palette.size]))
                }
            }
        }
    }

    var level by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var paddleX by remember { mutableFloatStateOf(0.5f) }
    var ballX by remember { mutableFloatStateOf(0.5f) }
    var ballY by remember { mutableFloatStateOf(0.84f) }
    var velX by remember { mutableFloatStateOf(0.009f) }
    var velY by remember { mutableFloatStateOf(-0.012f) }
    var bricks by remember { mutableStateOf(buildBricks(1)) }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    fun resetBall() {
        ballX = paddleX
        ballY = 0.84f
        val spdBoost = 1f + (level - 1) * 0.12f
        velX = 0.0085f * spdBoost * (if ((score + lives) % 2 == 0) 1f else -1f)
        velY = -0.012f * spdBoost
    }

    fun restartAll() {
        level = 1
        score = 0
        lives = 3
        paddleX = 0.5f
        bricks = buildBricks(1)
        isPaused = false
        isGameOver = false
        resetBall()
    }

    LaunchedEffect(isPaused, isGameOver, level) {
        while (!isPaused && !isGameOver) {
            delay(16L)
            if (isPaused || isGameOver) break

            var nx = ballX + velX
            var ny = ballY + velY
            var vx = velX
            var vy = velY

            if (nx - ballRadius <= 0f) {
                nx = ballRadius
                vx = kotlin.math.abs(vx)
            } else if (nx + ballRadius >= 1f) {
                nx = 1f - ballRadius
                vx = -kotlin.math.abs(vx)
            }
            if (ny - ballRadius <= 0f) {
                ny = ballRadius
                vy = kotlin.math.abs(vy)
            }

            // Paddle collision
            val pLeft = paddleX - paddleWidth / 2f
            val pRight = paddleX + paddleWidth / 2f
            if (vy > 0f && ny + ballRadius >= paddleY && ny - ballRadius <= paddleY + paddleHeight &&
                nx >= pLeft - ballRadius && nx <= pRight + ballRadius
            ) {
                val hitOffset = ((nx - paddleX) / (paddleWidth / 2f)).coerceIn(-1f, 1f)
                val speed = kotlin.math.hypot(vx, vy).coerceAtLeast(0.014f)
                vx = hitOffset * speed * 0.78f
                vy = -kotlin.math.sqrt((speed * speed - vx * vx).coerceAtLeast(0.0001f))
                ny = paddleY - ballRadius
            }

            // Brick collision
            val brickW = 1f / cols
            val brickH = 0.052f
            val topMargin = 0.08f
            var hitIndex = -1
            for (i in bricks.indices) {
                val b = bricks[i]
                val bx1 = b.col * brickW + 0.008f
                val bx2 = (b.col + 1) * brickW - 0.008f
                val by1 = topMargin + b.row * (brickH + 0.012f)
                val by2 = by1 + brickH
                if (nx + ballRadius >= bx1 && nx - ballRadius <= bx2 &&
                    ny + ballRadius >= by1 && ny - ballRadius <= by2
                ) {
                    hitIndex = i
                    val overlapLeft = kotlin.math.abs((nx + ballRadius) - bx1)
                    val overlapRight = kotlin.math.abs(bx2 - (nx - ballRadius))
                    val overlapTop = kotlin.math.abs((ny + ballRadius) - by1)
                    val overlapBottom = kotlin.math.abs(by2 - (ny - ballRadius))
                    val minOverlap = minOf(overlapLeft, overlapRight, overlapTop, overlapBottom)
                    if (minOverlap == overlapLeft || minOverlap == overlapRight) {
                        vx = -vx
                    } else {
                        vy = -vy
                    }
                    break
                }
            }

            if (hitIndex >= 0) {
                val target = bricks[hitIndex]
                val updated = bricks.toMutableList()
                if (target.hp > 1) {
                    updated[hitIndex] = target.copy(hp = target.hp - 1)
                    score += 15
                } else {
                    updated.removeAt(hitIndex)
                    score += 25
                }
                bricks = updated
                onScoreUpdated(score)

                if (updated.isEmpty()) {
                    level += 1
                    bricks = buildBricks(level)
                    resetBall()
                    continue
                }
            }

            if (ny > 1.02f) {
                lives -= 1
                if (lives <= 0) {
                    isGameOver = true
                    onScoreUpdated(score)
                } else {
                    resetBall()
                }
                continue
            }

            ballX = nx
            ballY = ny
            velX = vx
            velY = vy
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScorePill(label = "SCORE", value = score, accent = StreamCyan)
            ScorePill(label = "LVL $level • ♥$lives", value = maxOf(bestScore, score), accent = PulseCoral)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(
                    onClick = { isPaused = !isPaused },
                    enabled = !isGameOver
                ) {
                    Icon(
                        if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause"
                    )
                }
                FilledTonalIconButton(onClick = { restartAll() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Restart")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, StreamCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val normX = (change.position.x / size.width.toFloat())
                            .coerceIn(paddleWidth / 2f, 1f - paddleWidth / 2f)
                        paddleX = normX
                    }
                }
                .testTag("brick_breaker_canvas")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val brickW = w / cols
                val brickH = h * 0.052f
                val topMargin = h * 0.08f

                bricks.forEach { b ->
                    val left = b.col * brickW + w * 0.008f
                    val top = topMargin + b.row * (brickH + h * 0.012f)
                    val bw = brickW - w * 0.016f
                    drawRoundRect(
                        color = if (b.hp > 1) b.color else b.color.copy(alpha = 0.85f),
                        topLeft = Offset(left, top),
                        size = Size(bw, brickH),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    if (b.hp > 1) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.45f),
                            topLeft = Offset(left + 4f, top + 4f),
                            size = Size(bw - 8f, 4f),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }

                // Paddle
                val pW = paddleWidth * w
                val pH = paddleHeight * h
                drawRoundRect(
                    color = ArcadeMint,
                    topLeft = Offset(paddleX * w - pW / 2f, paddleY * h),
                    size = Size(pW, pH),
                    cornerRadius = CornerRadius(pH / 2f, pH / 2f)
                )

                // Ball
                drawCircle(
                    color = Color.White,
                    radius = ballRadius * minOf(w, h),
                    center = Offset(ballX * w, ballY * h)
                )
            }

            if (isGameOver || isPaused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.68f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isGameOver) "GAME OVER" else "PAUSED",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (isGameOver) PulseCoral else StreamCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Score: $score • Level $level",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { if (isGameOver) restartAll() else isPaused = false },
                            colors = ButtonDefaults.buttonColors(containerColor = StreamCyan, contentColor = Color.Black)
                        ) {
                            Text(if (isGameOver) "Play Again" else "Resume")
                        }
                    }
                }
            }
        }

        Text(
            text = "Drag horizontally anywhere on the arena to steer the neon paddle",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
