package com.example.ui.games.cityracer

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CityRacerProgress
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CityRacerGame(
    progress: CityRacerProgress,
    onSaveProgress: (CityRacerProgress) -> Unit,
    onExitToArcade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val audio = remember { CityRacerAudio() }
    val engine = remember {
        CityRacerEngine(
            initialProgress = progress,
            audio = audio,
            onProgressChanged = onSaveProgress
        )
    }

    LaunchedEffect(progress) {
        engine.syncProgress(progress)
    }

    var hud by remember { mutableStateOf(engine.buildHudSnapshot()) }
    var tickNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(hud.phase) {
        if (hud.phase == GameShellPhase.PLAYING) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { frameNanos ->
                    if (lastNanos != 0L) {
                        val rawDt = (frameNanos - lastNanos) / 1_000_000_000f
                        engine.step(rawDt)
                    }
                    lastNanos = frameNanos
                    tickNanos = frameNanos
                    hud = engine.buildHudSnapshot()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
    ) {
        when (hud.phase) {
            GameShellPhase.TITLE -> {
                CityRacerTitleScreen(
                    hud = hud,
                    onStart = {
                        engine.resetWorld(GameShellPhase.PLAYING)
                        hud = engine.buildHudSnapshot()
                    },
                    onOpenGarage = {
                        engine.showGarageModal = true
                        hud = engine.buildHudSnapshot()
                    },
                    onBack = onExitToArcade
                )
            }

            else -> {
                // Isolated 60fps Canvas viewport
                CityRacerViewportCanvas(
                    engine = engine,
                    frameTick = tickNanos,
                    modifier = Modifier.fillMaxSize()
                )

                // Low-occlusion perimeter HUD
                CityRacerHudOverlay(
                    hud = hud,
                    engine = engine,
                    onRefreshHud = { hud = engine.buildHudSnapshot() },
                    onToggleMute = {
                        audio.isMuted = !audio.isMuted
                        hud = engine.buildHudSnapshot()
                    },
                    onPause = {
                        engine.phase = GameShellPhase.PAUSED
                        hud = engine.buildHudSnapshot()
                    }
                )

                if (hud.phase == GameShellPhase.PAUSED) {
                    CityRacerPauseModal(
                        hud = hud,
                        onResume = {
                            engine.phase = GameShellPhase.PLAYING
                            hud = engine.buildHudSnapshot()
                        },
                        onOpenGarage = {
                            engine.showGarageModal = true
                            hud = engine.buildHudSnapshot()
                        },
                        onRestart = {
                            engine.resetWorld(GameShellPhase.PLAYING)
                            hud = engine.buildHudSnapshot()
                        },
                        onQuitToTitle = {
                            engine.phase = GameShellPhase.TITLE
                            hud = engine.buildHudSnapshot()
                        }
                    )
                }

                if (hud.phase == GameShellPhase.BUSTED) {
                    CityRacerBustedModal(
                        banner = hud.banner,
                        cash = hud.cash,
                        onRespawn = {
                            engine.resetWorld(GameShellPhase.PLAYING)
                            hud = engine.buildHudSnapshot()
                        },
                        onQuitToTitle = {
                            engine.phase = GameShellPhase.TITLE
                            hud = engine.buildHudSnapshot()
                        }
                    )
                }
            }
        }

        if (hud.showGarageModal) {
            GarageUpgradeDialog(
                hud = hud,
                onUpgradeEngine = {
                    engine.upgradeEngine()
                    hud = engine.buildHudSnapshot()
                },
                onUpgradeNitro = {
                    engine.upgradeNitro()
                    hud = engine.buildHudSnapshot()
                },
                onUpgradeArmor = {
                    engine.upgradeArmor()
                    hud = engine.buildHudSnapshot()
                },
                onBuyRide = { spec ->
                    engine.buyAndDeliverRide(spec)
                    engine.showGarageModal = false
                    hud = engine.buildHudSnapshot()
                },
                onDismiss = {
                    engine.showGarageModal = false
                    hud = engine.buildHudSnapshot()
                }
            )
        }
    }
}

@Composable
private fun CityRacerTitleScreen(
    hud: CityRacerHudState,
    onStart: () -> Unit,
    onOpenGarage: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_city_racer_hero),
            contentDescription = "Suncoast Heat City Racing",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xF0080B12),
                            Color(0xBA080B12),
                            Color(0x33080B12)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, ArcadeMint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "BANK $${hud.cash}  •  WINS ${hud.racesWon}  •  JACKS ${hud.carsStolen}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ArcadeMint,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                TextButton(onClick = onBack) {
                    Text("Exit", color = Color.White)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(0.86f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = PulseCoral.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "OPEN CITY STREET RACING SANDBOX",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseCoral,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "SUNCOAST\nHEAT",
                    fontSize = 42.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge
                )

                Text(
                    text = "Jack any street ride, outrun 5-star police pursuits, deliver stolen whips to the Chop Shop, run Taxi fares, and race rivals for big cash.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD6DEED)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = PulseCoral),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("city_racer_start_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START HUSTLE", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = onOpenGarage,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("city_racer_garage_button")
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = null, tint = NeonAmber)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Garage", color = Color.White)
                    }
                }
            }

            Text(
                text = "STEER LEFT/RIGHT · GAS & BRAKE · NOS BOOST · JACK CAR · PAUSE",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA8B3C7)
            )
        }
    }
}

@Composable
private fun CityRacerViewportCanvas(
    engine: CityRacerEngine,
    frameTick: Long,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Read frameTick so Canvas invalidates every frame without recomposing HUD needlessly
        @Suppress("UNUSED_VARIABLE")
        val t = frameTick

        val shakeX = if (engine.shakeAmount > 0.2f) (Random.nextFloat() - 0.5f) * engine.shakeAmount else 0f
        val shakeY = if (engine.shakeAmount > 0.2f) (Random.nextFloat() - 0.5f) * engine.shakeAmount else 0f
        val camLeft = engine.camX - size.width * 0.5f + shakeX
        val camTop = engine.camY - size.height * 0.5f + shakeY

        // City Asphalt Base
        drawRect(color = Color(0xFF141821))

        // Road Grid
        val roadColor = Color(0xFF202634)
        val laneColor = Color(0x45FBBF24)
        val sidewalkColor = Color(0xFF2A3244)
        for (rc in SuncoastCityMap.roadCenters) {
            val sx = rc - camLeft
            val sy = rc - camTop
            val rw = SuncoastCityMap.ROAD_WIDTH
            drawRect(sidewalkColor, Offset(sx - rw * 0.5f - 10f, -camTop), Size(rw + 20f, SuncoastCityMap.WORLD_SIZE))
            drawRect(sidewalkColor, Offset(-camLeft, sy - rw * 0.5f - 10f), Size(SuncoastCityMap.WORLD_SIZE, rw + 20f))
            drawRect(roadColor, Offset(sx - rw * 0.5f, -camTop), Size(rw, SuncoastCityMap.WORLD_SIZE))
            drawRect(roadColor, Offset(-camLeft, sy - rw * 0.5f), Size(SuncoastCityMap.WORLD_SIZE, rw))
            drawLine(laneColor, Offset(sx, -camTop), Offset(sx, SuncoastCityMap.WORLD_SIZE - camTop), strokeWidth = 2.5f)
            drawLine(laneColor, Offset(-camLeft, sy), Offset(SuncoastCityMap.WORLD_SIZE - camLeft, sy), strokeWidth = 2.5f)
        }

        // Skidmarks
        for (skid in engine.skidMarks) {
            drawLine(
                color = Color.Black.copy(alpha = skid.alpha.coerceIn(0f, 0.7f)),
                start = Offset(skid.x1 - camLeft, skid.y1 - camTop),
                end = Offset(skid.x2 - camLeft, skid.y2 - camTop),
                strokeWidth = 5f
            )
        }

        // Static Activity Beacons
        for (beacon in SuncoastCityMap.staticActivities) {
            val bx = beacon.x - camLeft
            val by = beacon.y - camTop
            if (bx in -120f..(size.width + 120f) && by in -120f..(size.height + 120f)) {
                drawCircle(beacon.accentColor.copy(alpha = 0.16f), radius = beacon.radius, center = Offset(bx, by))
                drawCircle(beacon.accentColor, radius = beacon.radius, center = Offset(bx, by), style = Stroke(width = 3f))
                drawCircle(beacon.accentColor, radius = 8f, center = Offset(bx, by))
            }
        }

        // Cash Stashes
        for (stash in engine.cashStashes) {
            val sx = stash.x - camLeft
            val sy = stash.y - camTop
            if (sx in -60f..(size.width + 60f) && sy in -60f..(size.height + 60f)) {
                drawCircle(ArcadeMint.copy(alpha = 0.28f), radius = 22f, center = Offset(sx, sy))
                drawRoundRect(
                    color = ArcadeMint,
                    topLeft = Offset(sx - 10f, sy - 7f),
                    size = Size(20f, 14f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }

        // Active Race / Trial / Taxi Checkpoint Target
        val activeTarget = when (val m = engine.mission) {
            is ActiveMissionState.StreetRace -> m.checkpoints[m.playerCheckpointIdx % m.checkpoints.size]
            is ActiveMissionState.TimeTrial -> m.checkpoints[m.currentIdx]
            is ActiveMissionState.TaxiFare -> m.destination
            is ActiveMissionState.FreeRoam -> null
        }
        activeTarget?.let { cp ->
            val cx = cp.x - camLeft
            val cy = cp.y - camTop
            drawCircle(NeonAmber.copy(alpha = 0.25f), radius = 64f, center = Offset(cx, cy))
            drawCircle(NeonAmber, radius = 64f, center = Offset(cx, cy), style = Stroke(width = 5f))
            drawCircle(Color.White, radius = 12f, center = Offset(cx, cy))
        }

        // Buildings (cull to viewport)
        for (b in SuncoastCityMap.buildings) {
            val bx = b.x - camLeft
            val by = b.y - camTop
            if (bx + b.w < -40f || bx > size.width + 40f || by + b.h < -40f || by > size.height + 40f) continue

            // Shadow
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(bx + 10f, by + 12f),
                size = Size(b.w, b.h),
                cornerRadius = CornerRadius(10f, 10f)
            )
            // Base wall
            drawRoundRect(
                color = Color(0xFF0E131E),
                topLeft = Offset(bx, by),
                size = Size(b.w, b.h),
                cornerRadius = CornerRadius(10f, 10f)
            )
            // Elevated roof
            drawRoundRect(
                color = b.roofColor,
                topLeft = Offset(bx - b.heightOffset * 0.2f, by - b.heightOffset * 0.45f),
                size = Size(b.w, b.h),
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = b.trimColor.copy(alpha = 0.42f),
                topLeft = Offset(bx - b.heightOffset * 0.2f, by - b.heightOffset * 0.45f),
                size = Size(b.w, b.h),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 2.2f)
            )
        }

        // Palm Trees along avenues
        for (palm in SuncoastCityMap.palmPositions) {
            val px = palm.x - camLeft
            val py = palm.y - camTop
            if (px !in -40f..(size.width + 40f) || py !in -40f..(size.height + 40f)) continue
            drawCircle(Color(0xFF0B3B2B), radius = 15f, center = Offset(px, py))
            drawCircle(ArcadeMint.copy(alpha = 0.35f), radius = 9f, center = Offset(px - 2f, py - 2f))
        }

        // Vehicles
        for (v in engine.vehicles) {
            val vx = v.x - camLeft
            val vy = v.y - camTop
            if (vx !in -80f..(size.width + 80f) || vy !in -80f..(size.height + 80f)) continue
            drawVehicleSprite(v, vx, vy)
        }

        // Player on foot
        if (engine.activeVehicle == null) {
            val px = engine.playerX - camLeft
            val py = engine.playerY - camTop
            rotate(degrees = engine.playerAngle * 180f / PI.toFloat(), pivot = Offset(px, py)) {
                drawCircle(Color.Black.copy(alpha = 0.5f), radius = 13f, center = Offset(px + 2f, py + 3f))
                drawCircle(ArcadeMint, radius = 11f, center = Offset(px, py))
                drawCircle(Color.White, radius = 4f, center = Offset(px + 5f, py))
            }
        }

        // Pooled particles
        for (p in engine.particles) {
            if (!p.active) continue
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * (0.5f + 0.5f * alpha),
                center = Offset(p.x - camLeft, p.y - camTop)
            )
        }

        // Waypoint Arrow pointing to active race/trial/taxi target
        activeTarget?.let { target ->
            val dx = target.x - engine.playerX
            val dy = target.y - engine.playerY
            val ang = atan2(dy, dx)
            val arrowDist = 72f
            val ax = size.width * 0.5f + cos(ang) * arrowDist
            val ay = size.height * 0.5f + sin(ang) * arrowDist
            rotate(degrees = ang * 180f / PI.toFloat(), pivot = Offset(ax, ay)) {
                val path = Path().apply {
                    moveTo(ax + 14f, ay)
                    lineTo(ax - 8f, ay - 9f)
                    lineTo(ax - 4f, ay)
                    lineTo(ax - 8f, ay + 9f)
                    close()
                }
                drawPath(path, color = NeonAmber)
            }
        }
    }
}

private fun DrawScope.drawVehicleSprite(v: WorldVehicle, sx: Float, sy: Float) {
    val deg = v.angle * 180f / PI.toFloat()
    val halfL = v.spec.length * 0.5f
    val halfW = v.spec.width * 0.5f

    rotate(degrees = deg, pivot = Offset(sx, sy)) {
        // Drop shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(sx - halfL + 4f, sy - halfW + 5f),
            size = Size(v.spec.length, v.spec.width),
            cornerRadius = CornerRadius(7f, 7f)
        )
        // Body
        val bodyColor = if (v.hitFlash > 0f) Color.White else v.customColor
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(sx - halfL, sy - halfW),
            size = Size(v.spec.length, v.spec.width),
            cornerRadius = CornerRadius(7f, 7f)
        )
        // Windshield
        drawRoundRect(
            color = Color(0xFF0B1220),
            topLeft = Offset(sx - halfL * 0.25f, sy - halfW * 0.72f),
            size = Size(v.spec.length * 0.42f, v.spec.width * 0.72f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Headlights
        drawCircle(Color(0xFFFFF3B0), radius = 3f, center = Offset(sx + halfL - 2f, sy - halfW * 0.6f))
        drawCircle(Color(0xFFFFF3B0), radius = 3f, center = Offset(sx + halfL - 2f, sy + halfW * 0.6f))
        // Police lightbar
        if (v.spec.isPolice) {
            val flashRed = ((v.sirenPhase.toInt()) % 2 == 0)
            drawRect(
                color = if (flashRed || !v.isPoliceActive) Color(0xFFFF2E4C) else Color(0xFF38BDF8),
                topLeft = Offset(sx - 3f, sy - halfW * 0.7f),
                size = Size(6f, halfW * 0.7f)
            )
            drawRect(
                color = if (!flashRed || !v.isPoliceActive) Color(0xFF38BDF8) else Color(0xFFFF2E4C),
                topLeft = Offset(sx - 3f, sy),
                size = Size(6f, halfW * 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CityRacerHudOverlay(
    hud: CityRacerHudState,
    engine: CityRacerEngine,
    onRefreshHud: () -> Unit,
    onToggleMute: () -> Unit,
    onPause: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        // Top Perimeter Status Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Top-Left: Cash, Wanted Stars, Car Stats
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    color = Color.Black.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, ArcadeMint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "$${hud.cash}",
                            style = MaterialTheme.typography.titleLarge,
                            color = ArcadeMint,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${hud.districtName} • ${hud.vehicleName} (${hud.speedMph} MPH)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                        if (hud.isInVehicle) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { hud.armorRatio },
                                color = if (hud.armorRatio > 0.35f) StreamCyan else PulseCoral,
                                trackColor = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.width(130.dp).height(4.dp)
                            )
                        }
                    }
                }

                if (hud.wantedStars > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.border(1.dp, PulseCoral, RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(5) { idx ->
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (idx < hud.wantedStars) NeonAmber else Color.Gray.copy(alpha = 0.35f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WANTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = PulseCoral
                            )
                        }
                    }
                }

                if (hud.driftCashPending > 0) {
                    Surface(color = NeonAmber, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "DRIFT +$${hud.driftCashPending}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Top-Right: Minimap Radar + Mute / Pause
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (hud.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute audio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                            .testTag("city_racer_pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Pause game",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                CityMinimapRadar(
                    engine = engine,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(1.5.dp, StreamCyan.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                )
            }
        }

        // Active Mission or Event Banner
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when (val m = hud.mission) {
                is ActiveMissionState.StreetRace -> {
                    Surface(
                        color = PulseCoral.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${m.raceName} • POS ${hud.racePlace}/${hud.raceTotalRacers} • LAP ${m.currentLap}/${m.totalLaps}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
                is ActiveMissionState.TimeTrial -> {
                    Surface(
                        color = StreamCyan.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${m.trialName} • ${"%.1f".format(m.timeRemainingSec)}s • Gate ${m.currentIdx + 1}/${m.checkpoints.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
                is ActiveMissionState.TaxiFare -> {
                    Surface(
                        color = NeonAmber.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "TAXI TO ${m.districtName.uppercase()} • ${m.timeRemainingSec.toInt()}s • $${m.fareCash}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
                is ActiveMissionState.FreeRoam -> Unit
            }

            hud.banner?.let { b ->
                Surface(
                    color = Color.Black.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(
                        1.dp,
                        if (b.isPositive) ArcadeMint else PulseCoral,
                        RoundedCornerShape(12.dp)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = b.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (b.isPositive) ArcadeMint else PulseCoral
                        )
                        Text(
                            text = b.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Bottom Touch Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Left: Steering Controls
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HoldControlButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    label = "LEFT",
                    accent = Color.White,
                    onHoldChange = { engine.inputLeft = it },
                    testTag = "racer_steer_left"
                )
                HoldControlButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    label = "RIGHT",
                    accent = Color.White,
                    onHoldChange = { engine.inputRight = it },
                    testTag = "racer_steer_right"
                )
            }

            // Center: JACK / EXIT Car action button
            val jackHighlight = hud.canJackNearbyCar || !hud.isInVehicle
            Surface(
                onClick = {
                    engine.justPressedJack = true
                    onRefreshHud()
                },
                color = when {
                    hud.canJackNearbyCar -> NeonAmber
                    hud.isInVehicle -> Color.Black.copy(alpha = 0.72f)
                    else -> Color.Black.copy(alpha = 0.55f)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(54.dp)
                    .border(
                        1.5.dp,
                        if (jackHighlight) NeonAmber else Color.White.copy(alpha = 0.35f),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("racer_jack_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (hud.isInVehicle) Icons.AutoMirrored.Filled.ExitToApp else Icons.Filled.DirectionsCar,
                        contentDescription = "Jack or exit car",
                        tint = if (hud.canJackNearbyCar) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            hud.canJackNearbyCar -> "JACK"
                            hud.isInVehicle -> "EXIT"
                            else -> "JACK"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hud.canJackNearbyCar) Color.Black else Color.White
                    )
                }
            }

            // Right: NOS + BRAKE + GAS
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                HoldControlButton(
                    icon = Icons.Filled.Bolt,
                    label = "NOS",
                    accent = StreamCyan,
                    progress = hud.nitroRatio,
                    onHoldChange = { engine.inputNitro = it },
                    testTag = "racer_nitro"
                )
                HoldControlButton(
                    icon = null,
                    label = "BRK",
                    accent = PulseCoral,
                    onHoldChange = { engine.inputBrake = it },
                    testTag = "racer_brake"
                )
                HoldControlButton(
                    icon = null,
                    label = "GAS",
                    accent = ArcadeMint,
                    onHoldChange = { engine.inputGas = it },
                    testTag = "racer_gas"
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HoldControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    accent: Color,
    progress: Float? = null,
    onHoldChange: (Boolean) -> Unit,
    testTag: String
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width = 58.dp, height = 62.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (pressed) accent.copy(alpha = 0.36f) else Color.Black.copy(alpha = 0.68f))
            .border(1.5.dp, accent.copy(alpha = if (pressed) 1f else 0.55f), RoundedCornerShape(16.dp))
            .pointerInteropFilter { event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        pressed = true
                        onHoldChange(true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                        pressed = false
                        onHoldChange(false)
                        true
                    }
                    else -> true
                }
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(26.dp))
            }
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = accent)
            if (progress != null) {
                Spacer(modifier = Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.width(38.dp).height(3.dp)
                )
            }
        }
    }
}

@Composable
private fun CityMinimapRadar(engine: CityRacerEngine, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val scale = size.width / SuncoastCityMap.WORLD_SIZE
        for (rc in SuncoastCityMap.roadCenters) {
            val p = rc * scale
            drawLine(Color.White.copy(alpha = 0.14f), Offset(p, 0f), Offset(p, size.height), 2f)
            drawLine(Color.White.copy(alpha = 0.14f), Offset(0f, p), Offset(size.width, p), 2f)
        }
        for (b in SuncoastCityMap.staticActivities) {
            drawCircle(b.accentColor, radius = 3.2f, center = Offset(b.x * scale, b.y * scale))
        }
        for (v in engine.vehicles) {
            if (v.isPoliceActive) {
                drawCircle(PulseCoral, radius = 2.5f, center = Offset(v.x * scale, v.y * scale))
            }
        }
        drawCircle(
            color = ArcadeMint,
            radius = 4f,
            center = Offset(engine.playerX * scale, engine.playerY * scale)
        )
    }
}

@Composable
private fun CityRacerPauseModal(
    hud: CityRacerHudState,
    onResume: () -> Unit,
    onOpenGarage: () -> Unit,
    onRestart: () -> Unit,
    onQuitToTitle: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text("Hustle Paused") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Cash: $${hud.cash}")
                Text("Races Won: ${hud.racesWon} • Cars Jacked: ${hud.carsStolen}")
                Text("Tips: Drive into Coral/Amber rings for Street Races, Cyan for Time Trials, Mint for Pay & Spray, or Harbor Dock for Chop Shop.")
            }
        },
        confirmButton = {
            Button(onClick = onResume) { Text("Resume") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onOpenGarage) { Text("Garage") }
                TextButton(onClick = onRestart) { Text("Reset") }
                TextButton(onClick = onQuitToTitle) { Text("Title") }
            }
        }
    )
}

@Composable
private fun CityRacerBustedModal(
    banner: EventBanner?,
    cash: Int,
    onRespawn: () -> Unit,
    onQuitToTitle: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(banner?.title ?: "BUSTED!", color = PulseCoral) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(banner?.subtitle ?: "You got caught by Suncoast PD.")
                Text("Remaining Bankroll: $$cash")
            }
        },
        confirmButton = {
            Button(
                onClick = onRespawn,
                colors = ButtonDefaults.buttonColors(containerColor = ArcadeMint, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back to Streets")
            }
        },
        dismissButton = {
            TextButton(onClick = onQuitToTitle) { Text("Title Screen") }
        }
    )
}

@Composable
private fun GarageUpgradeDialog(
    hud: CityRacerHudState,
    onUpgradeEngine: () -> Unit,
    onUpgradeNitro: () -> Unit,
    onUpgradeArmor: () -> Unit,
    onBuyRide: (VehicleSpec) -> Unit,
    onDismiss: () -> Unit
) {
    val purchasable = remember {
        listOf(VehicleSpec.BANSHEE_GT, VehicleSpec.SABRE_V8, VehicleSpec.TAXI_CAB, VehicleSpec.ARMORED_VAN)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Garage & Tuning ($${hud.cash})") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("Performance Tunes", style = MaterialTheme.typography.titleMedium, color = NeonAmber)
                }
                item {
                    val cost = hud.engineLevel * 450
                    UpgradeRow("Engine Tune (Lv ${hud.engineLevel}/5)", if (hud.engineLevel >= 5) "MAX" else "$$cost", hud.engineLevel < 5 && hud.cash >= cost, onUpgradeEngine)
                }
                item {
                    val cost = hud.nitroLevel * 400
                    UpgradeRow("Nitro Tank (Lv ${hud.nitroLevel}/5)", if (hud.nitroLevel >= 5) "MAX" else "$$cost", hud.nitroLevel < 5 && hud.cash >= cost, onUpgradeNitro)
                }
                item {
                    val cost = hud.armorLevel * 350
                    UpgradeRow("Armor Plating (Lv ${hud.armorLevel}/5)", if (hud.armorLevel >= 5) "MAX" else "$$cost", hud.armorLevel < 5 && hud.cash >= cost, onUpgradeArmor)
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Order Custom Ride Delivery", style = MaterialTheme.typography.titleMedium, color = StreamCyan)
                }
                items(purchasable) { spec ->
                    val cost = spec.chopValue + 200
                    UpgradeRow(
                        title = "${spec.displayName} (${(spec.maxSpeed * 0.28f).toInt()} MPH)",
                        costLabel = "$$cost",
                        enabled = hud.cash >= cost,
                        onClick = { onBuyRide(spec) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun UpgradeRow(
    title: String,
    costLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(costLabel)
            }
        }
    }
}
