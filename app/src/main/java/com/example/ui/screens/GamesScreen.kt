package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Grid4x4
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CityRacerProgress
import com.example.data.GameType
import com.example.data.HighScores
import com.example.ui.games.BrickBreakerGame
import com.example.ui.games.Puzzle2048Game
import com.example.ui.games.SimonPulseGame
import com.example.ui.games.SnakeGame
import com.example.ui.games.cityracer.CityRacerGame
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan

@Composable
fun GamesScreen(
    activeGame: GameType?,
    highScores: HighScores,
    cityRacerProgress: CityRacerProgress,
    onSaveCityRacerProgress: (CityRacerProgress) -> Unit,
    onLaunchGame: (GameType) -> Unit,
    onCloseGame: () -> Unit,
    onReportScore: (GameType, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeGame == GameType.CITY_RACER) {
        BackHandler { onCloseGame() }
        CityRacerGame(
            progress = cityRacerProgress,
            onSaveProgress = onSaveCityRacerProgress,
            onExitToArcade = onCloseGame,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (activeGame != null) {
        BackHandler { onCloseGame() }
        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCloseGame,
                    modifier = Modifier.testTag("game_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeGame.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = activeGame.genre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeGame) {
                    GameType.CITY_RACER -> Unit
                    GameType.SNAKE -> SnakeGame(
                        bestScore = highScores.snakeBest,
                        onScoreUpdated = { onReportScore(GameType.SNAKE, it) }
                    )
                    GameType.BRICK_BREAKER -> BrickBreakerGame(
                        bestScore = highScores.brickBreakerBest,
                        onScoreUpdated = { onReportScore(GameType.BRICK_BREAKER, it) }
                    )
                    GameType.PUZZLE_2048 -> Puzzle2048Game(
                        bestScore = highScores.puzzle2048Best,
                        onScoreUpdated = { onReportScore(GameType.PUZZLE_2048, it) }
                    )
                    GameType.SIMON_PULSE -> SimonPulseGame(
                        bestScore = highScores.simonBest,
                        onScoreUpdated = { onReportScore(GameType.SIMON_PULSE, it) }
                    )
                }
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 165.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ArcadeHeroBanner(
                cityCash = cityRacerProgress.cash,
                racesWon = cityRacerProgress.racesWon,
                onQuickPlay = { onLaunchGame(GameType.CITY_RACER) }
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Open City & Arcade Deck",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(GameType.entries, key = { it.id }) { game ->
            GameTileCard(
                game = game,
                bestScore = highScores.bestFor(game),
                onClick = { onLaunchGame(game) }
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun ArcadeHeroBanner(
    cityCash: Int,
    racesWon: Int,
    onQuickPlay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .clickable { onQuickPlay() }
            .testTag("arcade_hero_card")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_city_racer_hero),
                contentDescription = "Suncoast Heat open city racing",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = NeonAmber.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CASH $$cityCash • $racesWon WINS",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonAmber
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Suncoast Heat",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Street race, steal cars, dodge cops & hustle cash",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        onClick = onQuickPlay,
                        shape = CircleShape,
                        color = ArcadeMint,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play),
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameTileCard(
    game: GameType,
    bestScore: Int,
    onClick: () -> Unit
) {
    val (accent, icon) = gameStyleFor(game)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                accent.copy(alpha = 0.35f),
                RoundedCornerShape(20.dp)
            )
            .testTag("game_card_${game.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = game.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = game.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (game == GameType.CITY_RACER) "CASH: $$bestScore" else "BEST: $bestScore",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonAmber
                )
                Surface(
                    color = accent,
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play ${game.title}",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun gameStyleFor(game: GameType): Pair<Color, ImageVector> = when (game) {
    GameType.CITY_RACER -> PulseCoral to Icons.Filled.DirectionsCar
    GameType.SNAKE -> ArcadeMint to Icons.Filled.SportsEsports
    GameType.BRICK_BREAKER -> StreamCyan to Icons.Filled.TouchApp
    GameType.PUZZLE_2048 -> NeonAmber to Icons.Filled.Grid4x4
    GameType.SIMON_PULSE -> PulseCoral to Icons.Filled.GraphicEq
}
