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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private enum class SnakeDir { UP, DOWN, LEFT, RIGHT }
private data class GridPoint(val x: Int, val y: Int)

@Composable
fun SnakeGame(
    bestScore: Int,
    onScoreUpdated: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = 18
    var snake by remember {
        mutableStateOf(listOf(GridPoint(9, 9), GridPoint(8, 9), GridPoint(7, 9)))
    }
    var direction by remember { mutableStateOf(SnakeDir.RIGHT) }
    var pendingDir by remember { mutableStateOf(SnakeDir.RIGHT) }
    var food by remember { mutableStateOf(GridPoint(13, 9)) }
    var bonusFood by remember { mutableStateOf<GridPoint?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    fun spawnFood(occupied: List<GridPoint>): GridPoint {
        var candidate: GridPoint
        do {
            candidate = GridPoint(Random.nextInt(gridSize), Random.nextInt(gridSize))
        } while (candidate in occupied)
        return candidate
    }

    fun resetGame() {
        val initial = listOf(GridPoint(9, 9), GridPoint(8, 9), GridPoint(7, 9))
        snake = initial
        direction = SnakeDir.RIGHT
        pendingDir = SnakeDir.RIGHT
        food = spawnFood(initial)
        bonusFood = null
        score = 0
        isPaused = false
        isGameOver = false
    }

    fun changeDirection(newDir: SnakeDir) {
        val opposite = when (direction) {
            SnakeDir.UP -> SnakeDir.DOWN
            SnakeDir.DOWN -> SnakeDir.UP
            SnakeDir.LEFT -> SnakeDir.RIGHT
            SnakeDir.RIGHT -> SnakeDir.LEFT
        }
        if (newDir != opposite) {
            pendingDir = newDir
        }
    }

    LaunchedEffect(isPaused, isGameOver, score) {
        while (!isPaused && !isGameOver) {
            val tickMs = (165L - (score / 20) * 6L).coerceAtLeast(75L)
            delay(tickMs)
            if (isPaused || isGameOver) break

            direction = pendingDir
            val head = snake.first()
            val next = when (direction) {
                SnakeDir.UP -> GridPoint(head.x, head.y - 1)
                SnakeDir.DOWN -> GridPoint(head.x, head.y + 1)
                SnakeDir.LEFT -> GridPoint(head.x - 1, head.y)
                SnakeDir.RIGHT -> GridPoint(head.x + 1, head.y)
            }

            if (next.x !in 0 until gridSize || next.y !in 0 until gridSize || next in snake.dropLast(1)) {
                isGameOver = true
                onScoreUpdated(score)
                break
            }

            val ateFood = next == food
            val ateBonus = next == bonusFood
            val grown = listOf(next) + if (ateFood || ateBonus) snake else snake.dropLast(1)
            snake = grown

            if (ateFood) {
                score += 10
                onScoreUpdated(score)
                food = spawnFood(grown)
                if (bonusFood == null && Random.nextInt(5) == 0) {
                    bonusFood = spawnFood(grown + food)
                }
            } else if (ateBonus) {
                score += 30
                onScoreUpdated(score)
                bonusFood = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Score HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScorePill(label = "SCORE", value = score, accent = ArcadeMint)
            ScorePill(label = "BEST", value = maxOf(bestScore, score), accent = NeonAmber)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(
                    onClick = { isPaused = !isPaused },
                    enabled = !isGameOver,
                    modifier = Modifier.testTag("snake_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause"
                    )
                }
                FilledTonalIconButton(
                    onClick = { resetGame() },
                    modifier = Modifier.testTag("snake_reset_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Restart")
                }
            }
        }

        // Game Canvas Board
        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, ArcadeMint.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    var totalDrag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { totalDrag = Offset.Zero },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                            if (abs(totalDrag.x) > 32f || abs(totalDrag.y) > 32f) {
                                if (abs(totalDrag.x) > abs(totalDrag.y)) {
                                    changeDirection(if (totalDrag.x > 0) SnakeDir.RIGHT else SnakeDir.LEFT)
                                } else {
                                    changeDirection(if (totalDrag.y > 0) SnakeDir.DOWN else SnakeDir.UP)
                                }
                                totalDrag = Offset.Zero
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val cell = size.width / gridSize
                // Subtle grid lines
                val gridColor = Color.White.copy(alpha = 0.05f)
                for (i in 1 until gridSize) {
                    val pos = i * cell
                    drawLine(gridColor, Offset(pos, 0f), Offset(pos, size.height), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, pos), Offset(size.width, pos), strokeWidth = 1f)
                }

                // Food orb
                drawCircle(
                    color = PulseCoral,
                    radius = cell * 0.4f,
                    center = Offset((food.x + 0.5f) * cell, (food.y + 0.5f) * cell)
                )

                // Bonus orb
                bonusFood?.let { bf ->
                    drawCircle(
                        color = NeonAmber,
                        radius = cell * 0.44f,
                        center = Offset((bf.x + 0.5f) * cell, (bf.y + 0.5f) * cell)
                    )
                }

                // Snake segments
                snake.forEachIndexed { idx, pt ->
                    val segColor = if (idx == 0) ArcadeMint else ArcadeMint.copy(alpha = (0.9f - idx * 0.02f).coerceAtLeast(0.45f))
                    val pad = cell * 0.08f
                    drawRoundRect(
                        color = segColor,
                        topLeft = Offset(pt.x * cell + pad, pt.y * cell + pad),
                        size = Size(cell - pad * 2, cell - pad * 2),
                        cornerRadius = CornerRadius(cell * 0.28f, cell * 0.28f)
                    )
                }
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
                            color = if (isGameOver) PulseCoral else ArcadeMint
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Score: $score",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { if (isGameOver) resetGame() else isPaused = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ArcadeMint, contentColor = Color.Black),
                            modifier = Modifier.testTag("snake_overlay_action")
                        ) {
                            Text(if (isGameOver) "Play Again" else "Resume")
                        }
                    }
                }
            }
        }

        // Tactile D-Pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DPadButton(
                icon = Icons.Filled.KeyboardArrowUp,
                description = "Up",
                testTag = "snake_up",
                onClick = { changeDirection(SnakeDir.UP) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                DPadButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    description = "Left",
                    testTag = "snake_left",
                    onClick = { changeDirection(SnakeDir.LEFT) }
                )
                DPadButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    description = "Right",
                    testTag = "snake_right",
                    onClick = { changeDirection(SnakeDir.RIGHT) }
                )
            }
            DPadButton(
                icon = Icons.Filled.KeyboardArrowDown,
                description = "Down",
                testTag = "snake_down",
                onClick = { changeDirection(SnakeDir.DOWN) }
            )
        }
    }
}

@Composable
private fun DPadButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    testTag: String,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .size(50.dp)
            .testTag(testTag)
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun ScorePill(label: String, value: Int, accent: Color) {
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
