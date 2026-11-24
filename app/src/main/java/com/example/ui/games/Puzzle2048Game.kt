package com.example.ui.games

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan
import kotlin.math.abs
import kotlin.random.Random

private enum class MoveDir { UP, DOWN, LEFT, RIGHT }

@Composable
fun Puzzle2048Game(
    bestScore: Int,
    onScoreUpdated: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    fun spawnTile(board: List<Int>): List<Int> {
        val empty = board.indices.filter { board[it] == 0 }
        if (empty.isEmpty()) return board
        val chosen = empty[Random.nextInt(empty.size)]
        val value = if (Random.nextInt(10) == 0) 4 else 2
        return board.toMutableList().also { it[chosen] = value }
    }

    fun initialBoard(): List<Int> {
        var b = List(16) { 0 }
        b = spawnTile(b)
        b = spawnTile(b)
        return b
    }

    var board by remember { mutableStateOf(initialBoard()) }
    var previousBoard by remember { mutableStateOf<List<Int>?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var previousScore by remember { mutableIntStateOf(0) }
    var hasWonAcknowledged by remember { mutableStateOf(false) }

    fun compressLine(line: List<Int>): Pair<List<Int>, Int> {
        val nonZero = line.filter { it != 0 }
        val result = mutableListOf<Int>()
        var gained = 0
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val merged = nonZero[i] * 2
                result.add(merged)
                gained += merged
                i += 2
            } else {
                result.add(nonZero[i])
                i += 1
            }
        }
        while (result.size < 4) result.add(0)
        return result to gained
    }

    fun canMove(b: List<Int>): Boolean {
        if (0 in b) return true
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                val v = b[r * 4 + c]
                if (c + 1 < 4 && b[r * 4 + c + 1] == v) return true
                if (r + 1 < 4 && b[(r + 1) * 4 + c] == v) return true
            }
        }
        return false
    }

    fun performMove(dir: MoveDir) {
        val next = board.toMutableList()
        var gained = 0

        for (i in 0 until 4) {
            val line = when (dir) {
                MoveDir.LEFT -> (0 until 4).map { c -> board[i * 4 + c] }
                MoveDir.RIGHT -> (3 downTo 0).map { c -> board[i * 4 + c] }
                MoveDir.UP -> (0 until 4).map { r -> board[r * 4 + i] }
                MoveDir.DOWN -> (3 downTo 0).map { r -> board[r * 4 + i] }
            }
            val (merged, lineScore) = compressLine(line)
            gained += lineScore
            when (dir) {
                MoveDir.LEFT -> for (c in 0 until 4) next[i * 4 + c] = merged[c]
                MoveDir.RIGHT -> for (c in 0 until 4) next[i * 4 + (3 - c)] = merged[c]
                MoveDir.UP -> for (r in 0 until 4) next[r * 4 + i] = merged[r]
                MoveDir.DOWN -> for (r in 0 until 4) next[(3 - r) * 4 + i] = merged[r]
            }
        }

        if (next != board) {
            previousBoard = board
            previousScore = score
            board = spawnTile(next)
            score += gained
            onScoreUpdated(score)
        }
    }

    fun restart() {
        board = initialBoard()
        previousBoard = null
        score = 0
        previousScore = 0
        hasWonAcknowledged = false
    }

    val reached2048 = !hasWonAcknowledged && board.any { it >= 2048 }
    val gameOver = !canMove(board)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScorePill(label = "SCORE", value = score, accent = NeonAmber)
            ScorePill(label = "BEST", value = maxOf(bestScore, score), accent = ArcadeMint)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(
                    onClick = {
                        previousBoard?.let {
                            board = it
                            score = previousScore
                            previousBoard = null
                        }
                    },
                    enabled = previousBoard != null,
                    modifier = Modifier.testTag("puzzle_undo_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                }
                FilledTonalIconButton(
                    onClick = { restart() },
                    modifier = Modifier.testTag("puzzle_reset_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Restart")
                }
            }
        }

        Box(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, NeonAmber.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(10.dp)
                .pointerInput(Unit) {
                    var drag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { drag = Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            drag += amount
                        },
                        onDragEnd = {
                            if (abs(drag.x) > 36f || abs(drag.y) > 36f) {
                                if (abs(drag.x) > abs(drag.y)) {
                                    performMove(if (drag.x > 0) MoveDir.RIGHT else MoveDir.LEFT)
                                } else {
                                    performMove(if (drag.y > 0) MoveDir.DOWN else MoveDir.UP)
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (c in 0 until 4) {
                            val value = board[r * 4 + c]
                            TileCell(
                                value = value,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }

            if (reached2048 || gameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (reached2048) "2048 ACHIEVED!" else "NO MOVES LEFT",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (reached2048) NeonAmber else PulseCoral
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Final Score: $score",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (reached2048) {
                                Button(
                                    onClick = { hasWonAcknowledged = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ArcadeMint, contentColor = Color.Black)
                                ) {
                                    Text("Keep Playing")
                                }
                            }
                            Button(
                                onClick = { restart() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonAmber, contentColor = Color.Black)
                            ) {
                                Text("New Game")
                            }
                        }
                    }
                }
            }
        }

        // Swipe + tap controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(onClick = { performMove(MoveDir.LEFT) }, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Swipe Left")
            }
            FilledTonalIconButton(onClick = { performMove(MoveDir.UP) }, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Swipe Up")
            }
            FilledTonalIconButton(onClick = { performMove(MoveDir.DOWN) }, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Swipe Down")
            }
            FilledTonalIconButton(onClick = { performMove(MoveDir.RIGHT) }, shape = CircleShape, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Swipe Right")
            }
        }
    }
}

@Composable
private fun TileCell(value: Int, modifier: Modifier = Modifier) {
    val bgColor = when (value) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        2 -> Color(0xFF283248)
        4 -> Color(0xFF32415E)
        8 -> StreamCyan.copy(alpha = 0.78f)
        16 -> StreamCyan
        32 -> ArcadeMint.copy(alpha = 0.85f)
        64 -> ArcadeMint
        128 -> NeonAmber.copy(alpha = 0.82f)
        256 -> NeonAmber
        512 -> PulseCoral.copy(alpha = 0.85f)
        1024 -> PulseCoral
        else -> Color(0xFFA855F7)
    }
    val textColor = when {
        value == 0 -> Color.Transparent
        value >= 8 -> Color(0xFF090D14)
        else -> Color.White
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (value > 0) {
                Text(
                    text = value.toString(),
                    fontSize = if (value >= 1024) 20.sp else if (value >= 128) 24.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}
