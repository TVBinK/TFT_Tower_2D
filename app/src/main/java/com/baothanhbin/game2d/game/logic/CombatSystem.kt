package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*
import kotlin.math.sqrt

/**
 * Hệ thống combat - quản lý bắn đạn, va chạm, di chuyển
 */
class CombatSystem {
    
    private val effectSystem = EffectSystem()
	// Giảm tốc độ ra đạn bằng cách tăng cooldown mỗi lần bắn
	private val FIRE_RATE_MULTIPLIER: Float = 3.0f

	// Constants for tuning and readability
	private companion object {
		const val DEFAULT_BULLET_CAP: Int = 30
		const val DEFAULT_BULLET_RANGE: Float = 60f
		const val DEFAULT_SPREAD_RAD: Float = 0.30f
		const val WAVE_COOLDOWN_MULTIPLIER: Long = 5L
		const val MUZZLE_FLASH_ENABLED: Boolean = true
	}
    
    /**
     * Cập nhật cooldowns của tất cả units trên board
     */
    fun updateUnitCooldowns(state: GameState, deltaTimeMs: Long): GameState {
        val updatedBoard = state.player.board.mapValues { (_, unit) ->
            unit?.let { updateUnitCooldown(it.updateStatus(), deltaTimeMs) }
        }
        
        return state.copy(
            player = state.player.copy(board = updatedBoard)
        )
    }
    
    /**
     * Cập nhật cooldown của một unit
     */
    private fun updateUnitCooldown(unit: com.baothanhbin.game2d.game.model.Unit, deltaTimeMs: Long): com.baothanhbin.game2d.game.model.Unit {
        val newCooldown = (unit.cooldownRemainingMs - deltaTimeMs).coerceAtLeast(0L)
        return unit.copy(cooldownRemainingMs = newCooldown)
    }
    
    /**
     * Units bắn đạn vào enemies
     */
	fun unitsShoot(state: GameState): GameState {
		val enemies = state.enemies.filter { it.isAlive }
		logShootStart(state, enemies)
		if (!canCreateMoreBullets(state.bullets.size)) return state

		val newBullets = mutableListOf<Bullet>()
		val updatedBoard = state.player.board.toMutableMap()
		var updatedState = state

		for ((slot, unit) in state.player.board) {
			if (!canUnitAct(unit)) continue
			val target = findNearestEnemyInRange(unit!!, enemies, slot) ?: continue

			when (unit.type) {
				HeroType.FIRE -> {
					val res = handleFireSkill(updatedState, updatedBoard, slot, unit, target)
					updatedState = res
				}
				HeroType.WATER -> {
					updatedState = handleWaterSkill(updatedState, updatedBoard, slot, unit)
				}
				HeroType.FLOWER -> {
					handleFlower(updatedBoard, unit)
				}
				HeroType.ICE -> {
					updatedState = handleIceShot(updatedState, newBullets, updatedBoard, slot, unit, target)
				}
				else -> {
					updatedState = handleMetalShot(updatedState, newBullets, updatedBoard, slot, unit, target)
				}
			}
		}

		return updatedState.copy(
			bullets = state.bullets + newBullets,
			player = state.player.copy(board = updatedBoard)
		)
	}

	private fun canUnitAct(unit: com.baothanhbin.game2d.game.model.Unit?): Boolean {
		if (unit == null || unit.cooldownRemainingMs > 0 || !unit.canAct) {
			if (unit != null && unit.isFrozen) {
				println("COMBAT DEBUG: 🧊 Unit ${unit.type} at slot is FROZEN, skipping action")
			}
			return false
		}
		return true
	}

	private fun canCreateMoreBullets(current: Int): Boolean {
		if (current > DEFAULT_BULLET_CAP) {
			println("TOO MANY BULLETS! Current: $current, skipping bullet creation")
			return false
		}
		return true
	}

	private fun logShootStart(state: GameState, enemies: List<Enemy>) {
		val unitsOnBoard = state.player.board.values.count { it != null }
		android.util.Log.d("CombatSystem", "🔫 SHOOTING: unitsOnBoard=$unitsOnBoard, enemies=${enemies.size}")
	}

	private fun handleFireSkill(
		state: GameState,
		board: MutableMap<BoardSlot, com.baothanhbin.game2d.game.model.Unit?>,
		slot: BoardSlot,
		unit: com.baothanhbin.game2d.game.model.Unit,
		target: Enemy
	): GameState {
		if (unit.type != HeroType.FIRE || !unit.canUseFireSkill) return state

		val fireDuration = when (unit.star) {
			Star.ONE -> 5000L    // Thiêu đốt 5 giây
			Star.TWO -> 7000L   // Thiêu đốt 7 giây
 			Star.THREE -> 9000L // Thiêu đốt 9 giây
		}
        // DOT percent theo sao: 5% / 7% / 10% mỗi giây
        val firePercentPerSecond = when (unit.star) {
            Star.ONE -> 0.05f
            Star.TWO -> 0.07f
            Star.THREE -> 0.10f
        }
        val fireThickness = 150f
        var updatedState = effectSystem.addFireRowAtEnemyPosition(state, target, fireDuration, firePercentPerSecond, thickness = fireThickness)
		val effectEnd = System.currentTimeMillis() + fireDuration
		board[slot] = unit.copy(lastFireSkillAtMs = System.currentTimeMillis(), fireEffectEndAtMs = effectEnd)
		// Add sound event for FIRE skill
		updatedState = updatedState.addSoundEvent(SoundEvent.FIRE_SKILL)
		return updatedState
	}

	private fun handleWaterSkill(
		state: GameState,
		board: MutableMap<BoardSlot, com.baothanhbin.game2d.game.model.Unit?>,
		slot: BoardSlot,
		unit: com.baothanhbin.game2d.game.model.Unit
	): GameState {
		if (unit.type != HeroType.WATER) return state
		val waveCooldownMs = unit.actualFireRateMs * WAVE_COOLDOWN_MULTIPLIER
		val timeSinceLastWave = System.currentTimeMillis() - unit.lastWaveAtMs
		return if (timeSinceLastWave >= waveCooldownMs) {
			// Tạo hiệu ứng sóng nước theo số lượng sao: 1/2/3 sóng
			val (unitX, unitY) = getUnitPosition(slot)
            //Thoi gian sóng theo sao: 5s/6.5s/8s
			val waveDuration = when (unit.star) {
				Star.ONE -> 5000L
				Star.TWO -> 6500L
				Star.THREE -> 8000L
			}
			val waveCount = when (unit.star) {
				Star.ONE -> 1
				Star.TWO -> 2
				Star.THREE -> 3
			}
			val waveGapPx = 200f // khoảng cách giữa các sóng theo trục Y
			val waveDelayMs = 250L // trễ khởi động để giữ khoảng cách khi di chuyển lên
			var newState = state
				repeat(waveCount) { index ->
				val startY = (unitY - 40f) + index * waveGapPx
				val waveEffect = Effect(
					type = EffectType.WAVE,
					x = 0f,
					y = startY,
					durationMs = waveDuration,
						size = 130f, //heigh wave
					color = 0xFFFFFFFF.toInt(),
					currentTimeMs = (-index * waveDelayMs), // delay theo sóng
					startY = startY,
					endY = 0f,
					speed = 1f,
					width = GameState.SCREEN_WIDTH
				)
				newState = effectSystem.addEffect(newState, waveEffect)
			}
			board[slot] = unit.copy(
				cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
				lastShotAtMs = System.currentTimeMillis(),
				lastWaveAtMs = System.currentTimeMillis()
			)
			// Add sound event for WATER skill
			newState.addSoundEvent(SoundEvent.WATER_SKILL)
		} else {
			board[slot] = unit.copy(
				cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
				lastShotAtMs = System.currentTimeMillis()
			)
			state
		}
	}

	private fun handleIceShot(
		state: GameState,
		collector: MutableList<Bullet>,
		board: MutableMap<BoardSlot, com.baothanhbin.game2d.game.model.Unit?>,
		slot: BoardSlot,
		unit: com.baothanhbin.game2d.game.model.Unit,
		target: Enemy
	): GameState {
		if (unit.type != HeroType.ICE) return state
		val (bulletX, bulletY) = getUnitPosition(slot)
        // ICE damage fixed per star: 10 / 20 / 30
        val iceDamage = when (unit.star) {
            Star.ONE -> 10f
            Star.TWO -> 20f
            Star.THREE -> 30f
        }
		val iceUnit = unit.copy(baseDamage = iceDamage)
		createSpreadBullets(collector, iceUnit, bulletX, bulletY, target)
		var updatedState = state
		if (MUZZLE_FLASH_ENABLED) {
			updatedState = effectSystem.addMuzzleFlashEffect(updatedState, unit, slot)
		}
		board[slot] = applyShotCooldown(unit)
		return updatedState
	}

	private fun handleMetalShot(
		state: GameState,
		collector: MutableList<Bullet>,
		board: MutableMap<BoardSlot, com.baothanhbin.game2d.game.model.Unit?>,
		slot: BoardSlot,
		unit: com.baothanhbin.game2d.game.model.Unit,
		target: Enemy
	): GameState {
		val (bulletX, bulletY) = getUnitPosition(slot)
		val (enhancedDamage, enhancedSpeed) = if (unit.type == HeroType.METAL) {
			// Metal: 1★=15, 2★=30 (x2), 3★=45 (x3). Tăng tốc bắn ở 2★/3★ giữ nguyên.
			val base = 15f
			when (unit.star) {
				Star.ONE -> Pair(base * 1.0f, unit.baseFireRateMs * 1.0f)
				Star.TWO -> Pair(base * 2.0f, unit.baseFireRateMs * 0.8f)
				Star.THREE -> Pair(base * 3.0f, unit.baseFireRateMs * 0.6f)
			}
		} else Pair(unit.actualDamage, unit.baseFireRateMs.toFloat())
		val enhancedUnit = unit.copy(baseDamage = enhancedDamage, baseFireRateMs = enhancedSpeed.toLong())
		createSpreadBullets(collector, enhancedUnit, bulletX, bulletY, target)
		logBulletCreated(slot, target)
		var updatedState = state
		if (MUZZLE_FLASH_ENABLED) {
			updatedState = effectSystem.addMuzzleFlashEffect(updatedState, unit, slot)
		}
		board[slot] = applyShotCooldown(unit)
		return updatedState
	}

	private fun handleFlower(
		board: MutableMap<BoardSlot, com.baothanhbin.game2d.game.model.Unit?>,
		unit: com.baothanhbin.game2d.game.model.Unit
	) {
		if (unit.type != HeroType.FLOWER) return
		board.entries.find { it.value?.id == unit.id }?.let { entry ->
			board[entry.key] = unit.copy(
				cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
				lastShotAtMs = System.currentTimeMillis()
			)
		}
	}

	private fun createSpreadBullets(
		collector: MutableList<Bullet>,
		unit: com.baothanhbin.game2d.game.model.Unit,
		bulletX: Float,
		bulletY: Float,
		target: Enemy,
		bulletsPerShot: Int = 1,
		totalSpread: Float = DEFAULT_SPREAD_RAD,
		range: Float = DEFAULT_BULLET_RANGE
	) {
		val baseAngle = kotlin.math.atan2((target.y - bulletY), (target.x - bulletX))
		val step = if (bulletsPerShot > 1) totalSpread / (bulletsPerShot - 1) else 0f
		val startAngle = baseAngle - totalSpread / 2f
		repeat(bulletsPerShot) { index ->
			val angle = startAngle + index * step
			val tx = bulletX + kotlin.math.cos(angle) * range
			val ty = bulletY + kotlin.math.sin(angle) * range
			collector.add(Bullet.create(unit, bulletX, bulletY, target.copy(x = tx, y = ty)))
		}
	}

	private fun applyShotCooldown(unit: com.baothanhbin.game2d.game.model.Unit): com.baothanhbin.game2d.game.model.Unit {
		return unit.copy(
			cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
			lastShotAtMs = System.currentTimeMillis()
		)
	}

	private fun logBulletCreated(slot: BoardSlot, target: Enemy) {
		val targetColumn = ((target.x / (GameState.SCREEN_WIDTH / 5f)).toInt()).coerceIn(0, 4)
		val unitColumn = slot.position
		println("BULLET CREATED! Unit at column $unitColumn targeting Enemy at column $targetColumn")
	}
    
    /**
     * Lấy units đang active trên board
     */
    private fun getActiveUnits(player: Player): List<Pair<BoardSlot, com.baothanhbin.game2d.game.model.Unit>> {
        val deployedUnits = player.board.toList()
            .mapNotNull { (slot, unit) -> if (unit != null) slot to unit else null }
        
        // Chỉ lấy số lượng theo deploy cap
        return deployedUnits.take(player.deployCap)
    }
    
    /**
     * Tìm enemy gần nhất trong tầm bắn
     * Cập nhật: Tướng chỉ bắn enemies ở cột của mình (vì enemies chỉ spawn ở cột có tướng)
     * Ưu tiên enemies gần nhất và có HP thấp nhất để tối ưu hóa damage
     */
    private fun findNearestEnemyInRange(
        unit: com.baothanhbin.game2d.game.model.Unit,
        enemies: List<Enemy>, 
        slot: BoardSlot
    ): Enemy? {
        val (unitX, unitY) = getUnitPosition(slot)
        val unitColumn = slot.position
        
        // Debug: Log thông tin targeting
        println("TARGETING DEBUG: Unit at column $unitColumn, position ($unitX, $unitY), range ${unit.range}")
        println("Available enemies: ${enemies.size}")
        
        val filteredEnemies = enemies.filter { enemy ->
            // Tạm thời bỏ qua kiểm tra cột, chỉ kiểm tra tầm bắn
            val distance = calculateDistance(unitX, unitY, enemy.x, enemy.y)
            val inRange = distance <= unit.range
            
            // Tính cột của enemy để debug
            val slotWidth = GameState.SCREEN_WIDTH / 5f
            val enemyColumn = (enemy.x / slotWidth).toInt().coerceIn(0, 4)
            
            println("Enemy at column $enemyColumn, position (${enemy.x}, ${enemy.y}), distance $distance, inRange $inRange")
            
            inRange
        }
        
        println("Filtered enemies: ${filteredEnemies.size}")
        
        return filteredEnemies.minByOrNull { enemy ->
            val distance = calculateDistance(unitX, unitY, enemy.x, enemy.y)
            // Ưu tiên enemies gần nhất, nếu cùng khoảng cách thì ưu tiên HP thấp nhất
            distance * 1000f + enemy.currentHp
        }
    }
    
    /**
     * Lấy vị trí của unit trên màn hình
     */
    private fun getUnitPosition(slot: BoardSlot): Pair<Float, Float> {
        val slotWidth = GameState.SCREEN_WIDTH / 5f
        val x = slot.position * slotWidth + slotWidth / 2f
        val y = GameState.SCREEN_HEIGHT - 150f // Vị trí cố định, không random
        
        return Pair(x, y)
    }
    
    /**
     * Tính khoảng cách
     */
    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Di chuyển bullets
     */
    fun moveBullets(state: GameState, deltaTimeMs: Long): GameState {
        val deltaTimeSeconds = deltaTimeMs / 1000f
        val aliveEnemies = state.enemies.filter { it.isAlive }
        
        val updatedBullets = state.bullets.map { bullet ->
            // Chỉ cập nhật target nếu bullet chưa có target hoặc target đã chết
            val retargeted = if (bullet.targetEnemyId == null ||
                aliveEnemies.none { it.id == bullet.targetEnemyId }) {

                val nearestEnemy = aliveEnemies.minByOrNull { enemy ->
                    calculateDistance(bullet.x, bullet.y, enemy.x, enemy.y)
                }

                if (nearestEnemy != null) {
                    bullet.copy(
                        targetEnemyId = nearestEnemy.id,
                        targetX = nearestEnemy.x,
                        targetY = nearestEnemy.y
                    )
                } else {
                    bullet
                }
            } else bullet

            // Nếu có target, cập nhật targetX/Y theo vị trí hiện tại của enemy và di chuyển bám theo
            val targetEnemy = aliveEnemies.find { it.id == retargeted.targetEnemyId }
            val tx = targetEnemy?.x ?: retargeted.targetX
            val ty = targetEnemy?.y ?: retargeted.targetY

            if (tx != null && ty != null) {
                val dx = tx - retargeted.x
                val dy = ty - retargeted.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist > 0f) {
                    val maxStep = retargeted.speed * deltaTimeSeconds
                    val factor = kotlin.math.min(1f, maxStep / dist)
                    val newX = retargeted.x + dx * factor
                    val newY = retargeted.y + dy * factor
                    retargeted.copy(x = newX, y = newY, targetX = tx, targetY = ty)
                } else {
                    retargeted
                }
            } else {
                // Không có target, rơi về di chuyển thẳng
                retargeted.moveUp(deltaTimeSeconds)
            }
        }
        
        return state.copy(bullets = updatedBullets)
    }
    
    /**
     * Di chuyển enemies
     */
    fun moveEnemies(state: GameState, deltaTimeMs: Long): GameState {
        val deltaTimeSeconds = deltaTimeMs / 1000f
        
        val updatedEnemies = state.enemies.map { enemy ->
            // Cập nhật trạng thái đóng băng và làm chậm trước khi di chuyển
            val statusUpdatedEnemy = enemy.updateStatus()
            val moved = statusUpdatedEnemy.moveDown(deltaTimeSeconds)
            moved
        }
        
        var newState = state.copy(enemies = updatedEnemies)
        // Xử lý kỹ năng boss (đơn giản, theo cooldown)
        val now = System.currentTimeMillis()
        val bosses = newState.enemies.filter { it.isBoss && it.isAlive }
        bosses.forEach { boss ->
            val timeSinceLastAbility = now - boss.lastAbilityAtMs
            val canUseAbility = timeSinceLastAbility >= boss.abilityCooldownMs
            
            if (canUseAbility) {
                when (boss.bossAbility) {
                    BossAbility.SUMMON_MINIONS -> {
                        val colWidth = GameState.SCREEN_WIDTH / 5f
                        val summonX = boss.x
                        val minions = listOf(-1, 0, 1).map { dx ->
                            val x = (summonX + dx * (colWidth / 3f)).coerceIn(0f, GameState.SCREEN_WIDTH)
                            Enemy.create(x, boss.y - 60f,  newState.player.day, archetype = EnemyArchetype.FAST).copy(
                                enemyType = EnemyType.FAST
                            )
                        }
                        newState = newState.copy(enemies = newState.enemies + minions)
                    }
                    BossAbility.FREEZE_RANDOM_UNIT -> {
                        val candidates = newState.player.board.values.mapNotNull { it }
                        if (candidates.isNotEmpty()) {
                            val target = candidates.random()
                            val frozen = target.freeze(2000L)
                            val updatedBoard = newState.player.board.mapValues { (slot, unit) ->
                                if (unit?.id == target.id) frozen else unit
                            }
                            newState = newState.copy(player = newState.player.copy(board = updatedBoard))
                        }
                    }
                    BossAbility.SUMMON_MURID -> {
                        // Boss2: Summon 2 murid thay vì 3
                        val colWidth = GameState.SCREEN_WIDTH / 5f
                        val summonX = boss.x
                        val murids = listOf(-1, 1).map { dx -> // Giảm từ 3 xuống 2 murid
                            val x = (summonX + dx * (colWidth / 3f)).coerceIn(0f, GameState.SCREEN_WIDTH)
                            Enemy.create(x, boss.y - 60f, newState.player.day, archetype = EnemyArchetype.FAST).copy(
                                enemyType = EnemyType.MURID,
                                speed = 80f // Murid nhanh hơn enemy thường
                            )
                        }
                        newState = newState.copy(enemies = newState.enemies + murids)
                    }
                    BossAbility.FREEZE_HEROES -> {
                        // Boss3: Đóng băng random 2 tướng bất kỳ
                        println("BOSS3 DEBUG: 🧊 Boss3 using FREEZE_HEROES ability!")
                        var tempState = newState
                        
                        // Lấy danh sách tướng có sẵn trên board
                        val availableUnits = newState.player.board.values.mapNotNull { it }
                        if (availableUnits.isNotEmpty()) {
                            // Chọn random 2 tướng (hoặc ít hơn nếu không đủ)
                            val unitsToFreeze = availableUnits.shuffled().take(2)
                            println("BOSS3 DEBUG: 🧊 Freezing ${unitsToFreeze.size} random units")
                            
                            val updatedBoard = newState.player.board.mapValues { (slot, unit) ->
                                if (unit != null && unitsToFreeze.contains(unit)) {
                                    println("BOSS3 DEBUG: 🧊 Freezing unit ${unit.type} at slot ${slot.position}")
                                    // Thêm hiệu ứng freeze cho tướng được chọn
                                    val (unitX, unitY) = getUnitPosition(slot)
                                    val freezeEffect = Effect.createFreeze(unitX, unitY, 8000L) // Đóng băng 8 giây
                                    tempState = effectSystem.addEffect(tempState, freezeEffect)
                                    val frozenUnit = unit.freeze(8000L) // Đóng băng 8 giây
                                    println("BOSS3 DEBUG: 🧊 Unit ${frozenUnit.type} frozen: ${frozenUnit.isFrozen}, freezeEndTime: ${frozenUnit.freezeEndTime}")
                                    frozenUnit
                                } else {
                                    unit
                                }
                            }
                            newState = tempState.copy(player = tempState.player.copy(board = updatedBoard))
                        }
                        println("BOSS3 DEBUG: 🧊 FREEZE_HEROES completed!")
                    }
                    BossAbility.SHOOT_BULLETS -> {
                        // Tạm thời chỉ thêm hiệu ứng nhỏ; bắn đạn thật sẽ cần hệ va chạm mới
                        newState = newState // no-op placeholder
                    }
                    else -> {}
                }
                // cập nhật thời gian dùng skill cho boss này
                val updatedBoss = boss.copy(lastAbilityAtMs = now)
                newState = newState.copy(enemies = newState.enemies.map { if (it.id == boss.id) updatedBoss else it })
            }
        }
        return newState
    }
    
    /**
     * Xử lý va chạm giữa bullets và enemies
     */
    fun handleCollisions(state: GameState): GameState {
        val bulletsToRemove = mutableSetOf<String>()
        val updatedEnemies = mutableListOf<Enemy>()
        var goldGained = 0
        var scoreGained = 0L
        var updatedState = state
        
        // Xử lý va chạm
        for (enemy in state.enemies) {
            var currentEnemy = enemy
            var hit = false
            
            for (bullet in state.bullets) {
                if (bullet.id in bulletsToRemove) continue
                
                if (bullet.collidesWith(currentEnemy)) {
                    // Enemy nhận damage
                    currentEnemy = currentEnemy.takeDamage(bullet.damage)
                    bulletsToRemove.add(bullet.id)
                    hit = true
                    
                    // Xử lý đặc biệt cho đạn băng
                    if (bullet.heroType == HeroType.ICE) {
                        // Làm chậm enemy (100% chance)
                        currentEnemy = currentEnemy.slow(2000L, 0.3f) // 2 giây, tốc độ còn 30%
                        
                        // Tỷ lệ đóng băng theo sao của đạn ICE
                        val freezeChance = bullet.freezeChance ?: 0.3f
                        val freezeDuration = bullet.freezeDurationMs ?: 1500L
                        if (kotlin.random.Random.nextFloat() < freezeChance) {
                            currentEnemy = currentEnemy.freeze(freezeDuration)
                            
                            // Thêm hiệu ứng đóng băng
                            updatedState = effectSystem.addEffect(updatedState, Effect.createFreeze(currentEnemy.x, currentEnemy.y, freezeDuration))
                            
                            // Add sound event for ICE freeze
                            updatedState = updatedState.addSoundEvent(SoundEvent.ICE_SKILL)
                            
                            println("❄️ FREEZE! Enemy ${currentEnemy.id} frozen for ${freezeDuration}ms (p=${freezeChance})")
                        } else {
                            println("❄️ SLOW! Enemy ${currentEnemy.id} slowed for 2s")
                        }
                    }
                    
                    // Thêm hiệu ứng va chạm
                    updatedState = effectSystem.addHitEffect(updatedState, bullet, currentEnemy)

                    
                    // Nếu enemy chết, cộng reward và thêm hiệu ứng nổ
                    if (currentEnemy.isDead) {
                        goldGained += currentEnemy.reward
                        scoreGained += (currentEnemy.reward * 10).toLong()
                        updatedState = effectSystem.addEnemyDeathEffect(updatedState, currentEnemy)
                        // Cập nhật số enemy bị tiêu diệt
                        updatedState = updatedState.killEnemy(currentEnemy.id)
                        
                        // Track defeated bosses
                        if (currentEnemy.isBoss) {
                            updatedState = updatedState.addDefeatedBoss(currentEnemy.enemyType)
                            println("🏆 BOSS DEFEATED: ${currentEnemy.enemyType}, Total bosses: ${updatedState.defeatedBosses.size}")
                        }
                    }
                    
                    break // Một bullet chỉ va chạm một enemy
                }
            }
            
            if (currentEnemy.isAlive) {
                updatedEnemies.add(currentEnemy)
            }
        }
        
        // Xóa bullets đã va chạm
        val remainingBullets = state.bullets.filter { it.id !in bulletsToRemove }
        
        // Cập nhật player stats
        val updatedPlayer = state.player.copy(
            gold = state.player.gold + goldGained,
            score = state.player.score + scoreGained
        )
        
        return updatedState.copy(
            bullets = remainingBullets,
            enemies = updatedEnemies,
            player = updatedPlayer
        )
    }
    
    /**
     * Dọn dẹp bullets ra khỏi màn hình và enemies đã chết
     */
    fun cleanup(state: GameState): GameState {
        val validBullets = state.bullets.filter { bullet ->
            val isOffScreen = bullet.isOffScreen(GameState.SCREEN_HEIGHT)
            if (isOffScreen) {
                println("BULLET CLEANUP: Bullet(${bullet.x}, ${bullet.y}) removed - off screen")
            }
            !isOffScreen
        }
        
        // Nếu vẫn còn quá nhiều đạn, xóa bớt đạn cũ nhất
        val finalBullets = if (validBullets.size > 30) {
            println("BULLET CLEANUP: Too many bullets (${validBullets.size}), removing oldest bullets")
            validBullets.take(30) // Chỉ giữ lại 30 đạn mới nhất
        } else {
            validBullets
        }
        
		// Đánh dấu boss đã bị tiêu diệt nếu chết bởi hiệu ứng/nguồn khác ngoài va chạm đạn
		var updatedState = state
		val deadBosses = state.enemies.filter { !it.isAlive && it.isBoss }
		deadBosses.forEach { boss ->
			updatedState = updatedState.addDefeatedBoss(boss.enemyType)
		}
		
		val aliveEnemies = state.enemies.filter { it.isAlive }
        
        // Debug: Log bullet count
        if (finalBullets.size != state.bullets.size) {
            println("BULLET CLEANUP: Removed ${state.bullets.size - finalBullets.size} bullets. Remaining: ${finalBullets.size}")
        }
        
		return updatedState.copy(
			bullets = finalBullets,
			enemies = aliveEnemies
		)
    }
    
    /**
     * Kiểm tra enemies có chạm mép dưới PlayArea (đầu board) không
     * Nếu chạm: enemy chết ngay và trừ lives của player
     */
    fun checkEnemiesReachedBottom(state: GameState): GameState {
        // Đường chết trùng mép trên của BottomPanel: SCREEN_HEIGHT - DEATH_LINE_OFFSET
        val deathLineY = GameState.SCREEN_HEIGHT - GameState.DEATH_LINE_OFFSET
        // So sánh theo mép dưới của sprite enemy: bottom = y + size (vì vẽ cao = size*2)
        val (enemiesAtBottom, remainingEnemies) = state.enemies.partition { (it.y + it.size) >= deathLineY }

        if (enemiesAtBottom.isEmpty()) {
            return state.copy(enemies = remainingEnemies)
        }

        var updatedState = state
        enemiesAtBottom.forEach { enemy ->
            updatedState = effectSystem.addEnemyDeathEffect(updatedState, enemy)
            updatedState = updatedState.killEnemy(enemy.id)
        }

        // Kiểm tra boss chạm đáy - nếu có boss thì game over ngay
        val bossesAtBottom = enemiesAtBottom.filter { it.isBoss }
        if (bossesAtBottom.isNotEmpty()) {
            // Boss chạm đáy = game over ngay lập tức
            val gameOverPlayer = updatedState.player.copy(lives = 0)
            updatedState = updatedState.copy(player = gameOverPlayer)
            
        } else {
            // Trừ lives theo HP thực tế còn lại của enemies thường chạm đáy (làm tròn lên)
            val normalEnemiesAtBottom = enemiesAtBottom.filter { !it.isBoss }
            val damageToPlayer = normalEnemiesAtBottom.sumOf { enemy -> 
                kotlin.math.floor(enemy.currentHp.coerceAtLeast(0f)).toInt().coerceAtLeast(1)
            }
            // Trừ máu Player
            val newLives = (updatedState.player.lives - damageToPlayer).coerceAtLeast(0)
            val updatedPlayer = updatedState.player.copy(lives = newLives)
            updatedState = updatedState.copy(player = updatedPlayer)
        }
        
        // Debug log để theo dõi damage (chỉ cho enemies thường)
        val normalEnemiesAtBottom = enemiesAtBottom.filter { !it.isBoss }
        if (normalEnemiesAtBottom.isNotEmpty()) {
            val damageToPlayer = normalEnemiesAtBottom.sumOf { enemy -> 
                kotlin.math.floor(enemy.currentHp.coerceAtLeast(0f)).toInt().coerceAtLeast(1)
            }
            println("🔥 ENEMY REACHED BOTTOM! ${normalEnemiesAtBottom.size} normal enemies, total damage: $damageToPlayer HP")
            normalEnemiesAtBottom.forEach { enemy ->
                val finalDamage = kotlin.math.floor(enemy.currentHp.coerceAtLeast(0f)).toInt().coerceAtLeast(1)
                println("   - Enemy HP: ${enemy.currentHp}/${enemy.maxHp}, damage dealt: $finalDamage")
            }
        }

        return updatedState.copy(
            enemies = remainingEnemies
        )
    }
}
