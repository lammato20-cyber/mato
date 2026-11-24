package com.example.ui.games.cityracer

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan

enum class VehicleSpec(
    val displayName: String,
    val maxSpeed: Float,
    val acceleration: Float,
    val handling: Float,
    val driftGrip: Float,
    val maxArmor: Float,
    val chopValue: Int,
    val baseColor: Color,
    val length: Float,
    val width: Float,
    val isPolice: Boolean = false,
    val isTaxi: Boolean = false
) {
    BANSHEE_GT(
        displayName = "Banshee GT",
        maxSpeed = 620f,
        acceleration = 430f,
        handling = 3.5f,
        driftGrip = 0.88f,
        maxArmor = 100f,
        chopValue = 1200,
        baseColor = PulseCoral,
        length = 46f,
        width = 24f
    ),
    SABRE_V8(
        displayName = "Sabre V8",
        maxSpeed = 540f,
        acceleration = 380f,
        handling = 3.1f,
        driftGrip = 0.80f,
        maxArmor = 125f,
        chopValue = 750,
        baseColor = Color(0xFFFF7A00),
        length = 48f,
        width = 25f
    ),
    TAXI_CAB(
        displayName = "Suncoast Cab",
        maxSpeed = 480f,
        acceleration = 340f,
        handling = 3.4f,
        driftGrip = 0.86f,
        maxArmor = 115f,
        chopValue = 350,
        baseColor = NeonAmber,
        length = 44f,
        width = 23f,
        isTaxi = true
    ),
    COMET_S(
        displayName = "Comet S",
        maxSpeed = 505f,
        acceleration = 370f,
        handling = 3.8f,
        driftGrip = 0.90f,
        maxArmor = 95f,
        chopValue = 480,
        baseColor = StreamCyan,
        length = 42f,
        width = 22f
    ),
    ARMORED_VAN(
        displayName = "Securicar Van",
        maxSpeed = 415f,
        acceleration = 270f,
        handling = 2.4f,
        driftGrip = 0.92f,
        maxArmor = 220f,
        chopValue = 1600,
        baseColor = Color(0xFF94A3B8),
        length = 56f,
        width = 28f
    ),
    ENFORCER_PD(
        displayName = "Enforcer PD",
        maxSpeed = 595f,
        acceleration = 440f,
        handling = 3.6f,
        driftGrip = 0.89f,
        maxArmor = 150f,
        chopValue = 900,
        baseColor = Color(0xFF1E293B),
        length = 47f,
        width = 24f,
        isPolice = true
    )
}

data class Vec2(val x: Float, val y: Float)

data class CityBuilding(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val roofColor: Color,
    val trimColor: Color,
    val heightOffset: Float
)

data class WorldVehicle(
    val id: Int,
    val spec: VehicleSpec,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angle: Float = 0f,
    var speed: Float = 0f,
    var armor: Float = spec.maxArmor,
    var customColor: Color = spec.baseColor,
    var isPlayerOwned: Boolean = false,
    var isTraffic: Boolean = true,
    var isRival: Boolean = false,
    var isPoliceActive: Boolean = false,
    var aiTargetIndex: Int = 0,
    var laneDirIndex: Int = 0,
    var sirenPhase: Float = 0f,
    var hitFlash: Float = 0f
)

enum class ActivityKind {
    STREET_RACE,
    TIME_TRIAL,
    CHOP_SHOP,
    PAY_AND_SPRAY,
    CASH_STASH,
    TAXI_FARE
}

data class CityActivityBeacon(
    val id: String,
    val kind: ActivityKind,
    val title: String,
    val rewardLabel: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val accentColor: Color,
    val checkpoints: List<Vec2> = emptyList(),
    val prizeFirst: Int = 0,
    val timeLimitSec: Float = 0f
)

data class RacerParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float,
    var color: Color,
    var active: Boolean = false
)

data class SkidMark(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    var alpha: Float
)

sealed interface ActiveMissionState {
    data object FreeRoam : ActiveMissionState

    data class StreetRace(
        val raceName: String,
        val checkpoints: List<Vec2>,
        val playerCheckpointIdx: Int,
        val totalLaps: Int,
        val currentLap: Int,
        val rivalProgress: List<Int>,
        val prizeFirst: Int,
        val elapsedSec: Float
    ) : ActiveMissionState

    data class TimeTrial(
        val trialName: String,
        val checkpoints: List<Vec2>,
        val currentIdx: Int,
        val timeRemainingSec: Float,
        val prizeCash: Int
    ) : ActiveMissionState

    data class TaxiFare(
        val destination: Vec2,
        val districtName: String,
        val timeRemainingSec: Float,
        val fareCash: Int
    ) : ActiveMissionState
}

enum class GameShellPhase {
    TITLE,
    PLAYING,
    PAUSED,
    EVENT_RESULT,
    BUSTED
}

data class EventBanner(
    val title: String,
    val subtitle: String,
    val cashDelta: Int = 0,
    val isPositive: Boolean = true
)

data class CityRacerHudState(
    val phase: GameShellPhase = GameShellPhase.TITLE,
    val cash: Int = 600,
    val racesWon: Int = 0,
    val carsStolen: Int = 0,
    val engineLevel: Int = 1,
    val nitroLevel: Int = 1,
    val armorLevel: Int = 1,
    val speedMph: Int = 0,
    val vehicleName: String = "On Foot",
    val isInVehicle: Boolean = false,
    val armorRatio: Float = 1f,
    val nitroRatio: Float = 1f,
    val wantedStars: Int = 0,
    val wantedCooldownRatio: Float = 0f,
    val driftCashPending: Int = 0,
    val canJackNearbyCar: Boolean = false,
    val nearbyCarName: String = "",
    val mission: ActiveMissionState = ActiveMissionState.FreeRoam,
    val districtName: String = "Downtown Neon",
    val racePlace: Int = 1,
    val raceTotalRacers: Int = 4,
    val banner: EventBanner? = null,
    val isMuted: Boolean = false,
    val showGarageModal: Boolean = false
)

object SuncoastCityMap {
    const val WORLD_SIZE = 3200f
    const val BLOCK_STEP = 400f
    const val ROAD_WIDTH = 136f

    // Grid road center coordinates (8 roads across 0..3200)
    val roadCenters = listOf(200f, 600f, 1000f, 1400f, 1800f, 2200f, 2600f, 3000f)

    val buildings: List<CityBuilding> by lazy {
        val list = mutableListOf<CityBuilding>()
        val palettes = listOf(
            Color(0xFF171E2E) to PulseCoral,
            Color(0xFF1A2236) to StreamCyan,
            Color(0xFF1B2638) to ArcadeMint,
            Color(0xFF211E32) to NeonAmber
        )
        for (gx in 0 until roadCenters.size - 1) {
            for (gy in 0 until roadCenters.size - 1) {
                val left = roadCenters[gx] + ROAD_WIDTH * 0.5f + 22f
                val top = roadCenters[gy] + ROAD_WIDTH * 0.5f + 22f
                val right = roadCenters[gx + 1] - ROAD_WIDTH * 0.5f - 22f
                val bottom = roadCenters[gy + 1] - ROAD_WIDTH * 0.5f - 22f
                val bw = right - left
                val bh = bottom - top

                // Leave harbor crane plaza & central park open for gameplay activities
                if ((gx == 3 && gy == 3) || (gx == 5 && gy == 5)) continue

                val style = palettes[(gx * 3 + gy * 5) % palettes.size]
                val splitHoriz = (gx + gy) % 2 == 0
                if (splitHoriz) {
                    val gap = 22f
                    val h1 = bh * 0.47f
                    list.add(CityBuilding(left, top, bw, h1, style.first, style.second, 12f + ((gx + gy) % 4) * 5f))
                    val style2 = palettes[(gx + gy + 1) % palettes.size]
                    list.add(CityBuilding(left, top + h1 + gap, bw, bh - h1 - gap, style2.first, style2.second, 10f + (gx % 3) * 6f))
                } else {
                    val gap = 22f
                    val w1 = bw * 0.47f
                    list.add(CityBuilding(left, top, w1, bh, style.first, style.second, 14f + (gy % 4) * 5f))
                    val style2 = palettes[(gx + gy + 2) % palettes.size]
                    list.add(CityBuilding(left + w1 + gap, top, bw - w1 - gap, bh, style2.first, style2.second, 10f + (gy % 3) * 6f))
                }
            }
        }
        list
    }

    val palmPositions: List<Vec2> by lazy {
        buildList {
            for (rx in roadCenters) {
                for (ry in roadCenters) {
                    add(Vec2(rx - ROAD_WIDTH * 0.5f - 10f, ry - ROAD_WIDTH * 0.5f - 10f))
                    add(Vec2(rx + ROAD_WIDTH * 0.5f + 10f, ry + ROAD_WIDTH * 0.5f + 10f))
                }
            }
        }
    }

    val staticActivities: List<CityActivityBeacon> by lazy {
        listOf(
            CityActivityBeacon(
                id = "race_downtown_gp",
                kind = ActivityKind.STREET_RACE,
                title = "Downtown Midnight GP",
                rewardLabel = "$1,200 PRIZE",
                x = 1400f,
                y = 1000f,
                radius = 58f,
                accentColor = PulseCoral,
                checkpoints = listOf(
                    Vec2(1800f, 1000f),
                    Vec2(2200f, 1000f),
                    Vec2(2200f, 1400f),
                    Vec2(2200f, 1800f),
                    Vec2(1800f, 1800f),
                    Vec2(1400f, 1800f),
                    Vec2(1400f, 1400f),
                    Vec2(1400f, 1000f)
                ),
                prizeFirst = 1200
            ),
            CityActivityBeacon(
                id = "race_palms_sprint",
                kind = ActivityKind.STREET_RACE,
                title = "Palms Strip Sprint",
                rewardLabel = "$2,000 PRIZE",
                x = 600f,
                y = 600f,
                radius = 58f,
                accentColor = NeonAmber,
                checkpoints = listOf(
                    Vec2(600f, 1400f),
                    Vec2(600f, 2200f),
                    Vec2(1400f, 2200f),
                    Vec2(1400f, 1400f),
                    Vec2(1000f, 1000f),
                    Vec2(1000f, 600f),
                    Vec2(600f, 600f)
                ),
                prizeFirst = 2000
            ),
            CityActivityBeacon(
                id = "race_harbor_drift",
                kind = ActivityKind.STREET_RACE,
                title = "Harbor High-Stakes Cup",
                rewardLabel = "$3,000 PRIZE",
                x = 2600f,
                y = 1800f,
                radius = 58f,
                accentColor = Color(0xFFA855F7),
                checkpoints = listOf(
                    Vec2(3000f, 1800f),
                    Vec2(3000f, 2600f),
                    Vec2(2200f, 2600f),
                    Vec2(1800f, 2600f),
                    Vec2(1800f, 2200f),
                    Vec2(2600f, 2200f),
                    Vec2(2600f, 1800f)
                ),
                prizeFirst = 3000
            ),
            CityActivityBeacon(
                id = "trial_neon_blitz",
                kind = ActivityKind.TIME_TRIAL,
                title = "Neon Highway Blitz",
                rewardLabel = "$850 TIME TRIAL",
                x = 1800f,
                y = 600f,
                radius = 54f,
                accentColor = StreamCyan,
                checkpoints = listOf(
                    Vec2(2600f, 600f),
                    Vec2(2600f, 1000f),
                    Vec2(2600f, 1400f),
                    Vec2(1800f, 1400f),
                    Vec2(1800f, 600f)
                ),
                prizeFirst = 850,
                timeLimitSec = 40f
            ),
            CityActivityBeacon(
                id = "chop_harbor",
                kind = ActivityKind.CHOP_SHOP,
                title = "Harbor Chop Shop",
                rewardLabel = "SELL STOLEN CARS",
                x = 2400f,
                y = 2400f,
                radius = 74f,
                accentColor = NeonAmber
            ),
            CityActivityBeacon(
                id = "pay_spray_central",
                kind = ActivityKind.PAY_AND_SPRAY,
                title = "Pay & Spray",
                rewardLabel = "$150 REPAIR & CLEAR HEAT",
                x = 1600f,
                y = 1600f,
                radius = 70f,
                accentColor = ArcadeMint
            ),
            CityActivityBeacon(
                id = "pay_spray_north",
                kind = ActivityKind.PAY_AND_SPRAY,
                title = "Northside Pay & Spray",
                rewardLabel = "$150 REPAIR & CLEAR HEAT",
                x = 1000f,
                y = 600f,
                radius = 64f,
                accentColor = ArcadeMint
            )
        )
    }

    fun districtFor(x: Float, y: Float): String = when {
        x < 1600f && y < 1600f -> "Palms Strip"
        x >= 1600f && y < 1600f -> "Downtown Neon"
        x < 1600f && y >= 1600f -> "Industrial Yard"
        else -> "Harbor Docks"
    }
}
