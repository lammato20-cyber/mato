package com.example.ui.games.cityracer

import androidx.compose.ui.graphics.Color
import com.example.data.CityRacerProgress
import com.example.ui.theme.ArcadeMint
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.StreamCyan
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class CityRacerEngine(
    initialProgress: CityRacerProgress,
    private val audio: CityRacerAudio,
    private val onProgressChanged: (CityRacerProgress) -> Unit
) {
    var progress: CityRacerProgress = initialProgress
        private set

    var phase: GameShellPhase = GameShellPhase.TITLE
    var showGarageModal: Boolean = false

    // Input state (continuous held vs edge actions)
    var inputGas = false
    var inputBrake = false
    var inputLeft = false
    var inputRight = false
    var inputNitro = false
    var justPressedJack = false

    // Player state
    var playerX = 1400f
    var playerY = 1200f
    var playerAngle = 0f
    var footSpeed = 190f
    var activeVehicle: WorldVehicle? = null
    var nitroRatio = 1f
    var wantedStars = 0
    var wantedCooldownSec = 0f
    var driftCashPending = 0
    private var driftAccum = 0f

    // Camera + game feel
    var camX = 1400f
    var camY = 1200f
    var shakeAmount = 0f
    var hitstopTimer = 0f
    var banner: EventBanner? = null
    private var bannerTimer = 0f

    // World entities
    val vehicles = mutableListOf<WorldVehicle>()
    val cashStashes = mutableListOf<CityActivityBeacon>()
    val particles = Array(96) {
        RacerParticle(0f, 0f, 0f, 0f, 0f, 1f, 4f, Color.White, false)
    }
    private var particleCursor = 0
    val skidMarks = mutableListOf<SkidMark>()

    var mission: ActiveMissionState = ActiveMissionState.FreeRoam
    private var nextVehicleId = 1
    private var activityCooldownSec = 0f

    init {
        resetWorld(keepPhase = GameShellPhase.TITLE)
    }

    fun syncProgress(newProgress: CityRacerProgress) {
        progress = newProgress
    }

    fun resetWorld(keepPhase: GameShellPhase = GameShellPhase.PLAYING) {
        phase = keepPhase
        vehicles.clear()
        cashStashes.clear()
        skidMarks.clear()
        particles.forEach { it.active = false }
        mission = ActiveMissionState.FreeRoam
        wantedStars = 0
        wantedCooldownSec = 0f
        driftCashPending = 0
        driftAccum = 0f
        nitroRatio = 1f
        shakeAmount = 0f
        hitstopTimer = 0f
        activityCooldownSec = 1.5f

        // Spawn player's starter ride near Downtown Midnight GP
        val starter = WorldVehicle(
            id = nextVehicleId++,
            spec = VehicleSpec.SABRE_V8,
            x = 1400f,
            y = 1140f,
            angle = -PI.toFloat() / 2f,
            armor = VehicleSpec.SABRE_V8.maxArmor + (progress.armorLevel - 1) * 25f,
            isPlayerOwned = true,
            isTraffic = false
        )
        vehicles.add(starter)
        activeVehicle = starter
        playerX = starter.x
        playerY = starter.y
        playerAngle = starter.angle
        camX = playerX
        camY = playerY

        // Spawn parked showcase vehicles & roaming city traffic
        spawnCityTraffic()
        spawnCashStashes()
    }

    private fun spawnCityTraffic() {
        val specs = listOf(
            VehicleSpec.COMET_S,
            VehicleSpec.TAXI_CAB,
            VehicleSpec.SABRE_V8,
            VehicleSpec.BANSHEE_GT,
            VehicleSpec.ARMORED_VAN,
            VehicleSpec.ENFORCER_PD
        )
        val rc = SuncoastCityMap.roadCenters
        // Parked exotic near Palms Strip
        vehicles.add(
            WorldVehicle(
                id = nextVehicleId++,
                spec = VehicleSpec.BANSHEE_GT,
                x = 650f,
                y = 660f,
                angle = 0f,
                isTraffic = false
            )
        )
        // Roaming traffic across grid lanes
        for (i in 0 until 22) {
            val spec = when {
                i % 7 == 0 -> VehicleSpec.ENFORCER_PD
                i % 5 == 0 -> VehicleSpec.ARMORED_VAN
                i % 3 == 0 -> VehicleSpec.TAXI_CAB
                else -> specs[i % 4]
            }
            val horizontal = i % 2 == 0
            val roadCoord = rc[i % rc.size]
            val along = 250f + (i * 130f) % 2700f
            val dir = if (i % 4 < 2) 1 else -1
            val laneOffset = if (dir > 0) 26f else -26f
            val vxPos = if (horizontal) along else roadCoord + laneOffset
            val vyPos = if (horizontal) roadCoord + laneOffset else along
            val ang = if (horizontal) (if (dir > 0) 0f else PI.toFloat()) else (if (dir > 0) PI.toFloat() / 2f else -PI.toFloat() / 2f)

            vehicles.add(
                WorldVehicle(
                    id = nextVehicleId++,
                    spec = spec,
                    x = vxPos,
                    y = vyPos,
                    angle = ang,
                    speed = if (spec.isPolice) 190f else 165f + (i % 4) * 20f,
                    isTraffic = true,
                    laneDirIndex = if (horizontal) (if (dir > 0) 0 else 2) else (if (dir > 0) 1 else 3)
                )
            )
        }
    }

    private fun spawnCashStashes() {
        val spots = listOf(
            Vec2(1580f, 1540f),
            Vec2(2460f, 2320f),
            Vec2(800f, 1200f),
            Vec2(2000f, 800f),
            Vec2(1200f, 2400f),
            Vec2(2800f, 1200f),
            Vec2(400f, 2800f),
            Vec2(2200f, 400f)
        )
        spots.forEachIndexed { idx, pt ->
            cashStashes.add(
                CityActivityBeacon(
                    id = "stash_$idx",
                    kind = ActivityKind.CASH_STASH,
                    title = "Cash Stash",
                    rewardLabel = "+$125",
                    x = pt.x,
                    y = pt.y,
                    radius = 28f,
                    accentColor = ArcadeMint,
                    prizeFirst = 125 + (idx % 3) * 50
                )
            )
        }
    }

    fun step(rawDt: Float) {
        if (phase != GameShellPhase.PLAYING) return

        val dt = min(rawDt, 0.05f)
        if (activityCooldownSec > 0f) activityCooldownSec -= dt

        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) banner = null
        }

        if (hitstopTimer > 0f) {
            hitstopTimer -= dt
            shakeAmount = (shakeAmount - dt * 45f).coerceAtLeast(0f)
            return
        }

        shakeAmount = (shakeAmount - dt * 28f).coerceAtLeast(0f)

        // Handle edge-triggered JACK / EXIT action
        if (justPressedJack) {
            justPressedJack = false
            handleJackOrExit()
        }

        val currentCar = activeVehicle
        if (currentCar != null) {
            stepPlayerVehicle(currentCar, dt)
        } else {
            stepPlayerOnFoot(dt)
        }

        stepAiVehicles(dt)
        stepPoliceAndWanted(dt)
        stepActivities(dt)
        stepParticlesAndSkids(dt)

        // Smooth camera follow with lookahead
        val targetCamX = playerX + cos(playerAngle) * ((currentCar?.speed ?: 0f) * 0.18f)
        val targetCamY = playerY + sin(playerAngle) * ((currentCar?.speed ?: 0f) * 0.18f)
        val camLerp = (dt * 7.5f).coerceIn(0f, 1f)
        camX += (targetCamX - camX) * camLerp
        camY += (targetCamY - camY) * camLerp
    }

    private fun handleJackOrExit() {
        val currentCar = activeVehicle
        if (currentCar != null) {
            // Exit vehicle onto sidewalk/lane beside car
            activeVehicle = null
            currentCar.speed *= 0.4f
            currentCar.isTraffic = false
            playerX = (currentCar.x + cos(currentCar.angle + PI.toFloat() / 2f) * 34f)
                .coerceIn(30f, SuncoastCityMap.WORLD_SIZE - 30f)
            playerY = (currentCar.y + sin(currentCar.angle + PI.toFloat() / 2f) * 34f)
                .coerceIn(30f, SuncoastCityMap.WORLD_SIZE - 30f)
            return
        }

        val nearby = findNearestHijackableVehicle(95f) ?: return
        audio.playCarJack()
        val wasTraffic = nearby.isTraffic
        nearby.isTraffic = false
        nearby.isPoliceActive = false
        nearby.isPlayerOwned = true
        nearby.hitFlash = 0.25f
        activeVehicle = nearby
        playerX = nearby.x
        playerY = nearby.y
        playerAngle = nearby.angle
        spawnBurst(nearby.x, nearby.y, NeonAmber, 12)

        if (wasTraffic) {
            val newProgress = progress.copy(carsStolen = progress.carsStolen + 1)
            progress = newProgress
            onProgressChanged(newProgress)
            if (nearby.spec.isPolice) {
                raiseWanted(2)
                showBanner("JACKED COP CRUISER!", "2-Star Police Pursuit!", isPositive = false)
            } else {
                raiseWanted(1)
                showBanner("CAR JACKED: ${nearby.spec.displayName}", "Take it to Harbor Chop Shop for $${nearby.spec.chopValue}")
            }
        } else {
            showBanner("ENTERED ${nearby.spec.displayName}", "Ready to roll")
        }
    }

    fun findNearestHijackableVehicle(maxDist: Float = 95f): WorldVehicle? {
        if (activeVehicle != null) return null
        return vehicles
            .filter { !it.isRival }
            .minByOrNull { hypot(it.x - playerX, it.y - playerY) }
            ?.takeIf { hypot(it.x - playerX, it.y - playerY) <= maxDist }
    }

    private fun stepPlayerOnFoot(dt: Float) {
        val turnRate = 4.2f
        if (inputLeft) playerAngle -= turnRate * dt
        if (inputRight) playerAngle += turnRate * dt

        val moveSpeed = when {
            inputGas -> if (inputNitro && nitroRatio > 0.05f) {
                nitroRatio = (nitroRatio - dt * 0.35f).coerceAtLeast(0f)
                footSpeed * 1.45f
            } else {
                nitroRatio = (nitroRatio + dt * 0.2f).coerceAtMost(1f)
                footSpeed
            }
            inputBrake -> -footSpeed * 0.6f
            else -> {
                nitroRatio = (nitroRatio + dt * 0.2f).coerceAtMost(1f)
                0f
            }
        }

        if (moveSpeed != 0f) {
            val nx = (playerX + cos(playerAngle) * moveSpeed * dt).coerceIn(25f, SuncoastCityMap.WORLD_SIZE - 25f)
            val ny = (playerY + sin(playerAngle) * moveSpeed * dt).coerceIn(25f, SuncoastCityMap.WORLD_SIZE - 25f)
            if (!collidesWithBuildings(nx, ny, 12f)) {
                playerX = nx
                playerY = ny
            }
        }
    }

    private fun stepPlayerVehicle(car: WorldVehicle, dt: Float) {
        val engineMult = 1f + (progress.engineLevel - 1) * 0.11f
        val maxSpd = car.spec.maxSpeed * engineMult
        val accel = car.spec.acceleration * engineMult

        val usingNitro = inputNitro && nitroRatio > 0.03f && (inputGas || abs(car.speed) > 40f)
        if (usingNitro) {
            if (nitroRatio > 0.96f) audio.playNitroBurst()
            val drainRate = 0.34f / (1f + (progress.nitroLevel - 1) * 0.18f)
            nitroRatio = (nitroRatio - dt * drainRate).coerceAtLeast(0f)
            car.speed += accel * 1.65f * dt
            // Emit nitro flame behind car
            val rearX = car.x - cos(car.angle) * (car.spec.length * 0.55f)
            val rearY = car.y - sin(car.angle) * (car.spec.length * 0.55f)
            spawnParticle(
                rearX, rearY,
                -cos(car.angle) * 140f + Random.nextFloat() * 40f - 20f,
                -sin(car.angle) * 140f + Random.nextFloat() * 40f - 20f,
                0.24f, 6f, StreamCyan
            )
        } else {
            val regenRate = 0.09f * (1f + (progress.nitroLevel - 1) * 0.12f)
            nitroRatio = (nitroRatio + dt * regenRate).coerceAtMost(1f)
        }

        when {
            inputGas -> car.speed += accel * dt
            inputBrake -> {
                if (car.speed > 20f) {
                    car.speed -= accel * 1.7f * dt
                } else {
                    car.speed -= accel * 0.85f * dt
                }
            }
            else -> {
                // Natural rolling drag
                car.speed *= (1f - dt * 1.9f).coerceIn(0f, 1f)
                if (abs(car.speed) < 8f) car.speed = 0f
            }
        }

        val effectiveMax = if (usingNitro) maxSpd * 1.28f else maxSpd
        car.speed = car.speed.coerceIn(-maxSpd * 0.42f, effectiveMax)

        // Steering scaled by speed
        val speedFactor = (abs(car.speed) / 140f).coerceIn(0.25f, 1f)
        val reverseFlip = if (car.speed < -10f) -1f else 1f
        val steer = ((if (inputRight) 1f else 0f) - (if (inputLeft) 1f else 0f)) *
            car.spec.handling * speedFactor * reverseFlip
        if (abs(car.speed) > 12f) {
            car.angle += steer * dt
        }

        // Lateral velocity & drift feel
        val desiredVx = cos(car.angle) * car.speed
        val desiredVy = sin(car.angle) * car.speed
        val gripLerp = ((1f - car.spec.driftGrip) * 28f + 4.5f) * dt
        car.vx += (desiredVx - car.vx) * gripLerp.coerceIn(0.05f, 1f)
        car.vy += (desiredVy - car.vy) * gripLerp.coerceIn(0.05f, 1f)

        // Detect drifting for skidmarks & Drift Cash combo
        val slip = hypot(car.vx - desiredVx, car.vy - desiredVy)
        if (slip > 95f && abs(car.speed) > 180f) {
            val oldX = car.x
            val oldY = car.y
            val newX = car.x + car.vx * dt
            val newY = car.y + car.vy * dt
            skidMarks.add(SkidMark(oldX, oldY, newX, newY, 0.55f))
            if (skidMarks.size > 160) skidMarks.removeAt(0)
            spawnParticle(
                car.x - cos(car.angle) * 18f,
                car.y - sin(car.angle) * 18f,
                Random.nextFloat() * 30f - 15f,
                Random.nextFloat() * 30f - 15f,
                0.35f, 7f, Color.White.copy(alpha = 0.5f)
            )
            driftAccum += dt * 45f
            if (driftAccum >= 10f) {
                val addCash = (driftAccum / 10f).toInt() * 10
                driftCashPending += addCash
                driftAccum %= 10f
            }
        } else if (driftCashPending > 0 && slip < 55f) {
            awardCash(driftCashPending, "DRIFT BONUS +$$driftCashPending")
            driftCashPending = 0
        }

        // Move & collide with buildings
        val nextX = (car.x + car.vx * dt).coerceIn(24f, SuncoastCityMap.WORLD_SIZE - 24f)
        val nextY = (car.y + car.vy * dt).coerceIn(24f, SuncoastCityMap.WORLD_SIZE - 24f)
        val radius = car.spec.width * 0.55f

        var hitBuilding = false
        if (!collidesWithBuildings(nextX, car.y, radius)) {
            car.x = nextX
        } else {
            car.vx = -car.vx * 0.35f
            car.speed = -car.speed * 0.3f
            hitBuilding = true
        }
        if (!collidesWithBuildings(car.x, nextY, radius)) {
            car.y = nextY
        } else {
            car.vy = -car.vy * 0.35f
            car.speed = -car.speed * 0.3f
            hitBuilding = true
        }

        if (hitBuilding && abs(car.speed) > 90f) {
            onVehicleImpact(car, 6f, heavy = abs(car.speed) > 250f)
        }

        if (car.hitFlash > 0f) car.hitFlash = (car.hitFlash - dt).coerceAtLeast(0f)
        playerX = car.x
        playerY = car.y
        playerAngle = car.angle
    }

    private fun stepAiVehicles(dt: Float) {
        val playerCar = activeVehicle
        val rc = SuncoastCityMap.roadCenters

        for (v in vehicles) {
            if (v === playerCar) continue
            if (v.hitFlash > 0f) v.hitFlash = (v.hitFlash - dt).coerceAtLeast(0f)

            when {
                v.isRival && mission is ActiveMissionState.StreetRace -> {
                    val race = mission as ActiveMissionState.StreetRace
                    val target = race.checkpoints[v.aiTargetIndex % race.checkpoints.size]
                    val dx = target.x - v.x
                    val dy = target.y - v.y
                    val dist = hypot(dx, dy)
                    val desiredAng = atan2(dy, dx)
                    val diff = normalizeAngle(desiredAng - v.angle)
                    v.angle += diff.coerceIn(-3.2f * dt, 3.2f * dt)
                    val targetSpeed = v.spec.maxSpeed * (0.76f + (v.id % 3) * 0.05f)
                    v.speed += (targetSpeed - v.speed) * (dt * 2.5f)
                    v.x += cos(v.angle) * v.speed * dt
                    v.y += sin(v.angle) * v.speed * dt
                    if (dist < 85f) {
                        v.aiTargetIndex += 1
                    }
                }
                v.isPoliceActive -> {
                    v.sirenPhase += dt * 10f
                    val dx = playerX - v.x
                    val dy = playerY - v.y
                    val dist = hypot(dx, dy).coerceAtLeast(1f)
                    val desiredAng = atan2(dy, dx)
                    val diff = normalizeAngle(desiredAng - v.angle)
                    v.angle += diff.coerceIn(-3.1f * dt, 3.1f * dt)
                    val chaseSpd = (360f + wantedStars * 42f).coerceAtMost(v.spec.maxSpeed * 0.94f)
                    v.speed += (chaseSpd - v.speed) * (dt * 2.2f)
                    val nx = (v.x + cos(v.angle) * v.speed * dt).coerceIn(28f, SuncoastCityMap.WORLD_SIZE - 28f)
                    val ny = (v.y + sin(v.angle) * v.speed * dt).coerceIn(28f, SuncoastCityMap.WORLD_SIZE - 28f)
                    if (!collidesWithBuildings(nx, v.y, 14f)) v.x = nx else v.angle += 1.4f
                    if (!collidesWithBuildings(v.x, ny, 14f)) v.y = ny else v.angle += 1.4f

                    if (dist < 420f) audio.playSirenBlip()
                }
                v.isTraffic -> {
                    // Cruise along city roads and turn at intersections
                    val nx = v.x + cos(v.angle) * v.speed * dt
                    val ny = v.y + sin(v.angle) * v.speed * dt
                    if (nx < 80f || nx > SuncoastCityMap.WORLD_SIZE - 80f ||
                        ny < 80f || ny > SuncoastCityMap.WORLD_SIZE - 80f ||
                        collidesWithBuildings(nx, ny, 15f)
                    ) {
                        v.angle += PI.toFloat() / 2f
                    } else {
                        v.x = nx
                        v.y = ny
                        // Occasional clean 90-degree turn on road center intersections
                        if (Random.nextFloat() < dt * 0.35f) {
                            val nearX = rc.any { abs(v.x - it) < 14f }
                            val nearY = rc.any { abs(v.y - it) < 14f }
                            if (nearX && nearY) {
                                v.angle += if (Random.nextBoolean()) PI.toFloat() / 2f else -PI.toFloat() / 2f
                            }
                        }
                    }
                }
            }

            // Player <-> AI Vehicle collision
            if (playerCar != null) {
                val dx = playerCar.x - v.x
                val dy = playerCar.y - v.y
                val dist = hypot(dx, dy)
                val minDist = (playerCar.spec.width + v.spec.width) * 0.56f
                if (dist in 0.1f..minDist) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val overlap = minDist - dist
                    playerCar.x += nx * overlap * 0.6f
                    playerCar.y += ny * overlap * 0.6f
                    v.x -= nx * overlap * 0.4f
                    v.y -= ny * overlap * 0.4f

                    val relSpeed = abs(playerCar.speed - v.speed)
                    if (relSpeed > 90f) {
                        playerCar.speed *= 0.62f
                        v.speed *= 0.5f
                        onVehicleImpact(playerCar, 8f, heavy = relSpeed > 230f)
                        if (v.spec.isPolice || v.isPoliceActive) {
                            raiseWanted(1)
                        }
                    }
                }
            }
        }
    }

    private fun stepPoliceAndWanted(dt: Float) {
        if (wantedStars <= 0) return

        // Activate nearby police cruisers to pursue
        var activeCops = vehicles.count { it.isPoliceActive }
        for (v in vehicles) {
            if (v.spec.isPolice && v !== activeVehicle && !v.isPoliceActive && activeCops < wantedStars) {
                if (hypot(v.x - playerX, v.y - playerY) < 1100f) {
                    v.isPoliceActive = true
                    activeCops++
                }
            }
        }
        // Spawn additional cruiser if needed
        if (activeCops < wantedStars && vehicles.size < 34) {
            val angle = Random.nextFloat() * PI.toFloat() * 2f
            val sx = (playerX + cos(angle) * 620f).coerceIn(100f, SuncoastCityMap.WORLD_SIZE - 100f)
            val sy = (playerY + sin(angle) * 620f).coerceIn(100f, SuncoastCityMap.WORLD_SIZE - 100f)
            if (!collidesWithBuildings(sx, sy, 20f)) {
                vehicles.add(
                    WorldVehicle(
                        id = nextVehicleId++,
                        spec = VehicleSpec.ENFORCER_PD,
                        x = sx,
                        y = sy,
                        angle = atan2(playerY - sy, playerX - sx),
                        speed = 260f,
                        isTraffic = false,
                        isPoliceActive = true
                    )
                )
            }
        }

        // Check if busted (on foot or wrecked/slowed near active cop)
        val nearestCopDist = vehicles
            .filter { it.isPoliceActive && it !== activeVehicle }
            .minOfOrNull { hypot(it.x - playerX, it.y - playerY) } ?: 9999f

        if (nearestCopDist < 48f && (activeVehicle == null || abs(activeVehicle?.speed ?: 0f) < 60f)) {
            wantedCooldownSec -= dt * 2f
            if (nearestCopDist < 34f && abs(activeVehicle?.speed ?: 0f) < 35f) {
                triggerBusted()
                return
            }
        }

        // Lose wanted stars if out of police sight for 10s
        if (nearestCopDist > 680f) {
            wantedCooldownSec += dt
            if (wantedCooldownSec >= 10f) {
                wantedStars = 0
                wantedCooldownSec = 0f
                vehicles.forEach { it.isPoliceActive = false }
                showBanner("HEAT EVADED!", "Cops lost your trail")
            }
        } else {
            wantedCooldownSec = (wantedCooldownSec - dt * 0.5f).coerceAtLeast(0f)
        }
    }

    private fun stepActivities(dt: Float) {
        // 1. Collect Cash Stashes
        val iter = cashStashes.iterator()
        while (iter.hasNext()) {
            val stash = iter.next()
            if (hypot(stash.x - playerX, stash.y - playerY) < stash.radius + 18f) {
                awardCash(stash.prizeFirst, "CASH STASH +$${stash.prizeFirst}")
                spawnBurst(stash.x, stash.y, ArcadeMint, 14)
                iter.remove()
            }
        }
        if (cashStashes.size < 4) {
            spawnCashStashes()
        }

        // 2. Active Mission Progression
        when (val m = mission) {
            is ActiveMissionState.FreeRoam -> {
                if (activityCooldownSec <= 0f) {
                    checkFreeRoamBeacons()
                }
            }
            is ActiveMissionState.StreetRace -> {
                val target = m.checkpoints[m.playerCheckpointIdx % m.checkpoints.size]
                val elapsed = m.elapsedSec + dt
                val rivals = vehicles.filter { it.isRival }
                val rivalProg = rivals.map { it.aiTargetIndex }
                if (hypot(target.x - playerX, target.y - playerY) < 78f) {
                    audio.playCashChime()
                    spawnBurst(target.x, target.y, PulseCoral, 14)
                    val nextIdx = m.playerCheckpointIdx + 1
                    val totalGates = m.checkpoints.size * m.totalLaps
                    if (nextIdx >= totalGates) {
                        val place = 1 + rivalProg.count { it >= totalGates }
                        finishStreetRace(m.raceName, place, m.prizeFirst)
                    } else {
                        val lap = (nextIdx / m.checkpoints.size) + 1
                        mission = m.copy(
                            playerCheckpointIdx = nextIdx,
                            currentLap = lap,
                            rivalProgress = rivalProg,
                            elapsedSec = elapsed
                        )
                    }
                } else {
                    mission = m.copy(rivalProgress = rivalProg, elapsedSec = elapsed)
                }
            }
            is ActiveMissionState.TimeTrial -> {
                val rem = m.timeRemainingSec - dt
                if (rem <= 0f) {
                    mission = ActiveMissionState.FreeRoam
                    activityCooldownSec = 2f
                    showBanner("TIME TRIAL FAILED", "Too slow! Try Nitro Boost", isPositive = false)
                } else {
                    val target = m.checkpoints[m.currentIdx]
                    if (hypot(target.x - playerX, target.y - playerY) < 76f) {
                        audio.playCashChime()
                        spawnBurst(target.x, target.y, StreamCyan, 14)
                        if (m.currentIdx + 1 >= m.checkpoints.size) {
                            awardCash(m.prizeCash, "BLITZ CLEARED! +$${m.prizeCash}")
                            audio.playRaceWin()
                            mission = ActiveMissionState.FreeRoam
                            activityCooldownSec = 3f
                        } else {
                            mission = m.copy(currentIdx = m.currentIdx + 1, timeRemainingSec = rem)
                        }
                    } else {
                        mission = m.copy(timeRemainingSec = rem)
                    }
                }
            }
            is ActiveMissionState.TaxiFare -> {
                val rem = m.timeRemainingSec - dt
                if (rem <= 0f) {
                    mission = ActiveMissionState.FreeRoam
                    showBanner("PASSENGER BAILED", "Taxi fare expired", isPositive = false)
                } else if (hypot(m.destination.x - playerX, m.destination.y - playerY) < 76f &&
                    abs(activeVehicle?.speed ?: 0f) < 120f
                ) {
                    awardCash(m.fareCash, "TAXI FARE PAID +$${m.fareCash}")
                    spawnBurst(m.destination.x, m.destination.y, NeonAmber, 16)
                    mission = ActiveMissionState.FreeRoam
                    activityCooldownSec = 2.5f
                } else {
                    mission = m.copy(timeRemainingSec = rem)
                }
            }
        }
    }

    private fun checkFreeRoamBeacons() {
        val car = activeVehicle
        for (beacon in SuncoastCityMap.staticActivities) {
            if (hypot(beacon.x - playerX, beacon.y - playerY) <= beacon.radius) {
                when (beacon.kind) {
                    ActivityKind.STREET_RACE -> {
                        if (car != null) startStreetRace(beacon)
                    }
                    ActivityKind.TIME_TRIAL -> {
                        if (car != null) {
                            mission = ActiveMissionState.TimeTrial(
                                trialName = beacon.title,
                                checkpoints = beacon.checkpoints,
                                currentIdx = 0,
                                timeRemainingSec = beacon.timeLimitSec,
                                prizeCash = beacon.prizeFirst
                            )
                            activityCooldownSec = 2f
                            showBanner(beacon.title, "Hit all ${beacon.checkpoints.size} gates before time runs out!")
                        }
                    }
                    ActivityKind.PAY_AND_SPRAY -> {
                        if (car != null && (wantedStars > 0 || car.armor < car.spec.maxArmor)) {
                            if (progress.cash >= 150) {
                                val nextCash = progress.cash - 150
                                val updated = progress.copy(cash = nextCash)
                                progress = updated
                                onProgressChanged(updated)
                                car.armor = car.spec.maxArmor + (progress.armorLevel - 1) * 25f
                                car.customColor = listOf(PulseCoral, ArcadeMint, StreamCyan, NeonAmber, Color(0xFFA855F7)).random()
                                wantedStars = 0
                                wantedCooldownSec = 0f
                                vehicles.forEach { it.isPoliceActive = false }
                                audio.playCashChime()
                                spawnBurst(car.x, car.y, ArcadeMint, 18)
                                activityCooldownSec = 4f
                                showBanner("PAY & SPRAY COMPLETE", "Armor 100% & Wanted Heat Cleared (-$150)")
                            } else {
                                activityCooldownSec = 3f
                                showBanner("NEED $150 FOR RESPRAY", "Grab cash stashes or steal a ride", isPositive = false)
                            }
                        }
                    }
                    ActivityKind.CHOP_SHOP -> {
                        if (car != null && car.spec.chopValue > 0 && abs(car.speed) < 160f) {
                            val payout = (car.spec.chopValue * (car.armor / car.spec.maxArmor).coerceIn(0.4f, 1.1f)).toInt()
                            awardCash(payout, "CHOP SHOP SOLD ${car.spec.displayName} (+$$payout)")
                            spawnBurst(car.x, car.y, NeonAmber, 20)
                            vehicles.remove(car)
                            activeVehicle = null
                            playerX = beacon.x - 90f
                            playerY = beacon.y
                            activityCooldownSec = 3f
                        }
                    }
                    else -> {}
                }
                break
            }
        }

        // Auto-offer Taxi Fare if driving a Taxi Cab
        if (car != null && car.spec.isTaxi && mission is ActiveMissionState.FreeRoam && Random.nextFloat() < 0.015f) {
            val rc = SuncoastCityMap.roadCenters
            val dest = Vec2(rc.random(), rc.random())
            if (hypot(dest.x - playerX, dest.y - playerY) > 650f) {
                val fare = 240 + Random.nextInt(4) * 60
                mission = ActiveMissionState.TaxiFare(
                    destination = dest,
                    districtName = SuncoastCityMap.districtFor(dest.x, dest.y),
                    timeRemainingSec = 45f,
                    fareCash = fare
                )
                showBanner("TAXI FARE PICKED UP!", "Drop off in ${SuncoastCityMap.districtFor(dest.x, dest.y)} for $$fare")
            }
        }
    }

    private fun startStreetRace(beacon: CityActivityBeacon) {
        vehicles.removeAll { it.isRival }
        val car = activeVehicle ?: return
        car.x = beacon.x
        car.y = beacon.y
        val firstTarget = beacon.checkpoints.firstOrNull() ?: return
        car.angle = atan2(firstTarget.y - car.y, firstTarget.x - car.x)
        car.speed = 60f

        val rivalSpecs = listOf(VehicleSpec.BANSHEE_GT, VehicleSpec.SABRE_V8, VehicleSpec.COMET_S)
        rivalSpecs.forEachIndexed { idx, spec ->
            val offsetAngle = car.angle + PI.toFloat() / 2f
            val lateral = (idx - 1) * 34f
            vehicles.add(
                WorldVehicle(
                    id = nextVehicleId++,
                    spec = spec,
                    x = car.x + cos(offsetAngle) * lateral - cos(car.angle) * 28f,
                    y = car.y + sin(offsetAngle) * lateral - sin(car.angle) * 28f,
                    angle = car.angle,
                    speed = 90f,
                    customColor = listOf(StreamCyan, NeonAmber, Color(0xFFA855F7))[idx],
                    isTraffic = false,
                    isRival = true,
                    aiTargetIndex = 0
                )
            )
        }

        mission = ActiveMissionState.StreetRace(
            raceName = beacon.title,
            checkpoints = beacon.checkpoints,
            playerCheckpointIdx = 0,
            totalLaps = 2,
            currentLap = 1,
            rivalProgress = listOf(0, 0, 0),
            prizeFirst = beacon.prizeFirst,
            elapsedSec = 0f
        )
        activityCooldownSec = 3f
        audio.playCarJack()
        showBanner("${beacon.title} — GO!", "2 Laps • 1st Place wins $${beacon.prizeFirst}")
    }

    private fun finishStreetRace(name: String, place: Int, prizeFirst: Int) {
        vehicles.removeAll { it.isRival }
        mission = ActiveMissionState.FreeRoam
        activityCooldownSec = 4f
        val payout = when (place) {
            1 -> prizeFirst
            2 -> (prizeFirst * 0.45f).toInt()
            3 -> (prizeFirst * 0.2f).toInt()
            else -> 75
        }
        val won = place == 1
        val newProg = progress.copy(
            cash = progress.cash + payout,
            racesWon = progress.racesWon + if (won) 1 else 0
        )
        progress = newProg
        onProgressChanged(newProg)
        audio.playRaceWin()
        spawnBurst(playerX, playerY, NeonAmber, 24)
        val placeLabel = when (place) {
            1 -> "1ST PLACE!"
            2 -> "2ND PLACE"
            3 -> "3RD PLACE"
            else -> "4TH PLACE"
        }
        showBanner("$placeLabel — $name", "Earned +$$payout", cashDelta = payout)
    }

    private fun onVehicleImpact(car: WorldVehicle, baseDmg: Float, heavy: Boolean) {
        audio.playCrashImpact(heavy)
        car.hitFlash = 0.2f
        shakeAmount = if (heavy) 16f else 8f
        if (heavy) hitstopTimer = 0.045f
        spawnBurst(car.x, car.y, PulseCoral, if (heavy) 14 else 6)

        if (car === activeVehicle) {
            val armorMitigation = 1f + (progress.armorLevel - 1) * 0.2f
            car.armor = (car.armor - baseDmg / armorMitigation).coerceAtLeast(0f)
            if (car.armor <= 0f) {
                triggerBusted(wrecked = true)
            }
        }
    }

    private fun triggerBusted(wrecked: Boolean = false) {
        val fine = (progress.cash * 0.15f).toInt().coerceIn(0, 250)
        val updated = progress.copy(cash = (progress.cash - fine).coerceAtLeast(0))
        progress = updated
        onProgressChanged(updated)
        phase = GameShellPhase.BUSTED
        vehicles.removeAll { it.isRival }
        mission = ActiveMissionState.FreeRoam
        banner = EventBanner(
            title = if (wrecked) "RIDE WRECKED!" else "BUSTED BY PD!",
            subtitle = if (fine > 0) "Hospital / Bail fee -$fine" else "Back on the streets",
            cashDelta = -fine,
            isPositive = false
        )
    }

    private fun raiseWanted(delta: Int) {
        wantedStars = (wantedStars + delta).coerceIn(0, 5)
        wantedCooldownSec = 0f
    }

    private fun awardCash(amount: Int, subtitle: String) {
        val updated = progress.copy(cash = progress.cash + amount)
        progress = updated
        onProgressChanged(updated)
        audio.playCashChime()
        showBanner("+$amount CASH", subtitle, cashDelta = amount)
    }

    private fun showBanner(title: String, subtitle: String, cashDelta: Int = 0, isPositive: Boolean = true) {
        banner = EventBanner(title, subtitle, cashDelta, isPositive)
        bannerTimer = 3.2f
    }

    fun upgradeEngine(): Boolean {
        if (progress.engineLevel >= 5) return false
        val cost = progress.engineLevel * 450
        if (progress.cash < cost) return false
        val updated = progress.copy(cash = progress.cash - cost, engineLevel = progress.engineLevel + 1)
        progress = updated
        onProgressChanged(updated)
        audio.playCashChime()
        return true
    }

    fun upgradeNitro(): Boolean {
        if (progress.nitroLevel >= 5) return false
        val cost = progress.nitroLevel * 400
        if (progress.cash < cost) return false
        val updated = progress.copy(cash = progress.cash - cost, nitroLevel = progress.nitroLevel + 1)
        progress = updated
        onProgressChanged(updated)
        audio.playCashChime()
        return true
    }

    fun upgradeArmor(): Boolean {
        if (progress.armorLevel >= 5) return false
        val cost = progress.armorLevel * 350
        if (progress.cash < cost) return false
        val updated = progress.copy(cash = progress.cash - cost, armorLevel = progress.armorLevel + 1)
        progress = updated
        onProgressChanged(updated)
        audio.playCashChime()
        return true
    }

    fun buyAndDeliverRide(spec: VehicleSpec): Boolean {
        val cost = spec.chopValue + 200
        if (progress.cash < cost) return false
        val updated = progress.copy(cash = progress.cash - cost)
        progress = updated
        onProgressChanged(updated)
        val delivered = WorldVehicle(
            id = nextVehicleId++,
            spec = spec,
            x = playerX,
            y = playerY,
            angle = playerAngle,
            armor = spec.maxArmor + (progress.armorLevel - 1) * 25f,
            isPlayerOwned = true,
            isTraffic = false
        )
        vehicles.add(delivered)
        activeVehicle = delivered
        audio.playCarJack()
        showBanner("DELIVERED: ${spec.displayName}", "Fresh ride ready to race!")
        return true
    }

    private fun stepParticlesAndSkids(dt: Float) {
        for (p in particles) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
            }
        }
        val skidIt = skidMarks.iterator()
        while (skidIt.hasNext()) {
            val s = skidIt.next()
            s.alpha -= dt * 0.14f
            if (s.alpha <= 0f) skidIt.remove()
        }
    }

    private fun spawnBurst(x: Float, y: Float, color: Color, count: Int) {
        repeat(count) {
            val ang = Random.nextFloat() * PI.toFloat() * 2f
            val spd = 50f + Random.nextFloat() * 140f
            spawnParticle(x, y, cos(ang) * spd, sin(ang) * spd, 0.42f, 5.5f, color)
        }
    }

    private fun spawnParticle(x: Float, y: Float, vx: Float, vy: Float, life: Float, size: Float, color: Color) {
        val p = particles[particleCursor]
        particleCursor = (particleCursor + 1) % particles.size
        p.x = x
        p.y = y
        p.vx = vx
        p.vy = vy
        p.life = life
        p.maxLife = life
        p.size = size
        p.color = color
        p.active = true
    }

    private fun collidesWithBuildings(cx: Float, cy: Float, radius: Float): Boolean {
        for (b in SuncoastCityMap.buildings) {
            val closestX = cx.coerceIn(b.x, b.x + b.w)
            val closestY = cy.coerceIn(b.y, b.y + b.h)
            val dx = cx - closestX
            val dy = cy - closestY
            if (dx * dx + dy * dy < radius * radius) return true
        }
        return false
    }

    private fun normalizeAngle(a: Float): Float {
        var res = a
        while (res > PI.toFloat()) res -= (2.0 * PI).toFloat()
        while (res < -PI.toFloat()) res += (2.0 * PI).toFloat()
        return res
    }

    fun buildHudSnapshot(): CityRacerHudState {
        val car = activeVehicle
        val nearby = findNearestHijackableVehicle(95f)
        val armorRatio = if (car != null) (car.armor / (car.spec.maxArmor + (progress.armorLevel - 1) * 25f)).coerceIn(0f, 1f) else 1f
        val place = when (val m = mission) {
            is ActiveMissionState.StreetRace -> 1 + m.rivalProgress.count { it > m.playerCheckpointIdx }
            else -> 1
        }
        return CityRacerHudState(
            phase = phase,
            cash = progress.cash,
            racesWon = progress.racesWon,
            carsStolen = progress.carsStolen,
            engineLevel = progress.engineLevel,
            nitroLevel = progress.nitroLevel,
            armorLevel = progress.armorLevel,
            speedMph = if (car != null) (abs(car.speed) * 0.28f).toInt() else 0,
            vehicleName = car?.spec?.displayName ?: "On Foot",
            isInVehicle = car != null,
            armorRatio = armorRatio,
            nitroRatio = nitroRatio,
            wantedStars = wantedStars,
            wantedCooldownRatio = (wantedCooldownSec / 10f).coerceIn(0f, 1f),
            driftCashPending = driftCashPending,
            canJackNearbyCar = nearby != null,
            nearbyCarName = nearby?.spec?.displayName ?: "",
            mission = mission,
            districtName = SuncoastCityMap.districtFor(playerX, playerY),
            racePlace = place,
            raceTotalRacers = 4,
            banner = banner,
            isMuted = audio.isMuted,
            showGarageModal = showGarageModal
        )
    }
}
