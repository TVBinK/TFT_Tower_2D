package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*
import kotlin.math.sqrt

/**
 * Hệ thống combat - quản lý bắn đạn, va chạm, di chuyển
 */
class CombatSystem {
    
    private val effectSystem = EffectSystem()
    private val soundSystem = SoundSystem()
    // Giảm tốc độ ra đạn bằng cách tăng cooldown mỗi lần bắn
    private val FIRE_RATE_MULTIPLIER: Float = 3.0f
    
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
        val activeUnits = getActiveUnits(state.player)
        val enemies = state.enemies.filter { it.isAlive }
        
        // Debug: Log shooting info
        val unitsOnBoard = state.player.board.values.count { it != null }
        android.util.Log.d("CombatSystem", "🔫 SHOOTING: unitsOnBoard=$unitsOnBoard, enemies=${enemies.size}")
        
        // Giới hạn số lượng đạn tối đa để tránh spam, hạ ngưỡng ngay đầu trận
        val bulletCap = 30
        if (state.bullets.size > bulletCap) {
            println("TOO MANY BULLETS! Current: ${state.bullets.size}, skipping bullet creation")
            return state
        }
        
        val newBullets = mutableListOf<Bullet>()
        val updatedBoard = state.player.board.toMutableMap()
        var updatedState = state
        
        for ((slot, unit) in state.player.board) {
            if (unit == null || unit.cooldownRemainingMs > 0 || !unit.canAct) {
                if (unit != null && unit.isFrozen) {
                    println("COMBAT DEBUG: 🧊 Unit ${unit.type} at slot ${slot.position} is FROZEN, skipping action")
                }
                continue
            }
            
            // Tìm enemy gần nhất trong tầm bắn
            val target = findNearestEnemyInRange(unit, enemies, slot)
            
            if (target != null) {
                // Hệ Hỏa: tạo hàng lửa với thời gian theo sao
                if (unit.type == HeroType.FIRE) {
                    val fireDuration = when (unit.star) {
                        Star.ONE -> 5000L   // ★☆☆ 5 giây
                        Star.TWO -> 7000L   // ★★☆ 7 giây  
                        Star.THREE -> 9000L // ★★★ 9 giây
                    }
                    val fireDps = unit.actualDamage * 2f
                    val fireThickness = 150f
                    
                    // ★★★ có splash damage (sẽ implement sau trong EffectSystem)
                    val hasSplashDamage = unit.star == Star.THREE
                    
                    updatedState = effectSystem.addFireRow(updatedState, slot, fireDuration, fireDps, thickness = fireThickness)
                    
                    val updatedUnit = unit.copy(
                        cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
                        lastShotAtMs = System.currentTimeMillis()
                    )
                    updatedBoard[slot] = updatedUnit
                    continue
                }
                // Hệ Thủy: tạo cơn sóng đẩy lùi địch (có cooldown riêng cho wave)
                else if (unit.type == HeroType.WATER) {
                    // Kiểm tra cooldown của wave (dài hơn cooldown bắn thường)
                    val waveCooldownMs = unit.actualFireRateMs * 5L // Cooldown wave = 5x cooldown bắn thường (tăng từ 3x)
                    val timeSinceLastWave = System.currentTimeMillis() - unit.lastWaveAtMs
                    
                    android.util.Log.d("THUY_WAVE", "🌊 THUY Wave Check: timeSinceLastWave=${timeSinceLastWave}ms, waveCooldownMs=${waveCooldownMs}ms, canCreateWave=${timeSinceLastWave >= waveCooldownMs}")
                    
                    if (timeSinceLastWave >= waveCooldownMs) {
                        val waveDuration = 3000L // 3 giây
                        val waveHeight = 200f // Chiều cao sóng
                        
                        // Tạo số sóng theo sao
                        val waveCount = when (unit.star) {
                            Star.ONE -> 1   // ★☆☆ 1 sóng
                            Star.TWO -> 2   // ★★☆ 2 sóng
                            Star.THREE -> 3 // ★★★ 3 sóng
                        }
                        
                        // Tạo nhiều sóng (sẽ implement delay sau trong EffectSystem)
                        repeat(waveCount) { index ->
                            // Tạm thời tạo sóng liên tiếp, delay sẽ được implement trong EffectSystem
                            updatedState = effectSystem.addWave(updatedState, slot, waveDuration, waveHeight)
                        }
                        
                        android.util.Log.d("THUY_WAVE", "🌊 THUY Wave CREATED! Count=${waveCount}, Height=${waveHeight}")
                        
                        // Cập nhật lastWaveAtMs khi tạo wave
                        val updatedUnit = unit.copy(
                            cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
                            lastShotAtMs = System.currentTimeMillis(),
                            lastWaveAtMs = System.currentTimeMillis()
                        )
                        updatedBoard[slot] = updatedUnit
                    } else {
                        android.util.Log.d("THUY_WAVE", "🌊 THUY Wave COOLDOWN: ${waveCooldownMs - timeSinceLastWave}ms remaining")
                        
                        // Không tạo wave, chỉ cập nhật cooldown bắn thường
                        val updatedUnit = unit.copy(
                            cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
                            lastShotAtMs = System.currentTimeMillis()
                        )
                        updatedBoard[slot] = updatedUnit
                    }
                    continue
                }
                // Hệ Mộc: không bắn – chỉ hồi máu (xử lý trong GameEngine)
                else if (unit.type == HeroType.FLOWER) {
                    // FLOWER không bắn đạn, chỉ hồi HP theo sao
                    // Logic hồi HP được xử lý trong GameEngine với thời gian khác nhau theo sao
                    val updatedUnit = unit.copy(
                        cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
                        lastShotAtMs = System.currentTimeMillis()
                    )
                    updatedBoard[slot] = updatedUnit
                    continue
                }
                // Hệ Băng: bắn đạn băng với hiệu ứng theo sao
                else if (unit.type == HeroType.ICE) {
                    
                    // Tạo bullet băng từ vị trí tướng
                    val (baseX, baseY) = getUnitPosition(slot)
                    val bulletX = baseX
                    val bulletY = baseY
                    
                    // Tăng damage theo sao
                    val iceDamage = when (unit.star) {
                        Star.ONE -> unit.actualDamage * 1.0f    // ★☆☆ damage cơ bản
                        Star.TWO -> unit.actualDamage * 1.5f    // ★★☆ +50% damage
                        Star.THREE -> unit.actualDamage * 2.0f  // ★★★ +100% damage
                    }
                    
                    // Tăng freeze chance theo sao
                    val freezeChance = when (unit.star) {
                        Star.ONE -> 0.3f   // ★☆☆ 30% chance
                        Star.TWO -> 0.5f   // ★★☆ 50% chance
                        Star.THREE -> 0.7f // ★★★ 70% chance
                    }
                    
                    // Tạo bullet với damage và freeze chance đã điều chỉnh
                    val iceUnit = unit.copy(baseDamage = iceDamage)
                    val bulletsPerShot = 1
                    val baseAngle = kotlin.math.atan2((target.y - bulletY), (target.x - bulletX))
                    val totalSpread = 0.30f
                    val step = if (bulletsPerShot > 1) totalSpread / (bulletsPerShot - 1) else 0f
                    val startAngle = baseAngle - totalSpread / 2f

                    repeat(bulletsPerShot) { index ->
                        val angle = startAngle + index * step
                        val range = 60f
                        val tx = bulletX + kotlin.math.cos(angle) * range
                        val ty = bulletY + kotlin.math.sin(angle) * range
                        val b = Bullet.create(iceUnit, bulletX, bulletY, target.copy(x = tx, y = ty))
                        newBullets.add(b)
                    }
                    
                    // Thêm hiệu ứng lửa nòng súng
                    updatedState = effectSystem.addMuzzleFlashEffect(updatedState, unit, slot)
                    
                    val updatedUnit = unit.copy(
                        cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
                        lastShotAtMs = System.currentTimeMillis()
                    )
                    updatedBoard[slot] = updatedUnit
                    continue
                }
                
                // Tạo bullet thẳng từ vị trí tướng (METAL và các hệ khác)
                val (baseX, baseY) = getUnitPosition(slot)
                val bulletX = baseX
                val bulletY = baseY
                
                // Tăng damage và speed theo sao cho METAL
                val (enhancedDamage, enhancedSpeed) = if (unit.type == HeroType.METAL) {
                    when (unit.star) {
                        Star.ONE -> Pair(unit.actualDamage * 1.0f, unit.actualFireRateMs * 1.0f)    // ★☆☆ cơ bản
                        Star.TWO -> Pair(unit.actualDamage * 1.3f, unit.actualFireRateMs * 0.8f)   // ★★☆ +30% damage, +25% speed
                        Star.THREE -> Pair(unit.actualDamage * 1.6f, unit.actualFireRateMs * 0.6f) // ★★★ +60% damage, +67% speed
                    }
                } else {
                    Pair(unit.actualDamage, unit.actualFireRateMs.toFloat())
                }
                
                // Tạo unit với stats đã tăng cường
                val enhancedUnit = unit.copy(
                    baseDamage = enhancedDamage,
                    baseFireRateMs = enhancedSpeed.toLong()
                )
                
                val bulletsPerShot = 1
                val baseAngle = kotlin.math.atan2((target.y - bulletY), (target.x - bulletX))
                val totalSpread = 0.30f
                val step = if (bulletsPerShot > 1) totalSpread / (bulletsPerShot - 1) else 0f
                val startAngle = baseAngle - totalSpread / 2f

                repeat(bulletsPerShot) { index ->
                    val angle = startAngle + index * step
                    val range = 60f
                    val tx = bulletX + kotlin.math.cos(angle) * range
                    val ty = bulletY + kotlin.math.sin(angle) * range
                    val b = Bullet.create(enhancedUnit, bulletX, bulletY, target.copy(x = tx, y = ty))
                    newBullets.add(b)
                }
                
                // Debug: Log bullet creation
                val targetColumn = ((target.x / (GameState.SCREEN_WIDTH / 5f)).toInt()).coerceIn(0, 4)
                val unitColumn = slot.position
                println("BULLET CREATED! Unit at column $unitColumn targeting Enemy at column $targetColumn")
                
                // Thêm hiệu ứng lửa nòng súng
                updatedState = effectSystem.addMuzzleFlashEffect(updatedState, unit, slot)

                
                // Reset cooldown và đánh dấu thời điểm bắn
                val updatedUnit = unit.copy(
                    cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
                    lastShotAtMs = System.currentTimeMillis()
                )
                updatedBoard[slot] = updatedUnit
            }
        }
        
        return updatedState.copy(
            bullets = state.bullets + newBullets,
            player = state.player.copy(board = updatedBoard)
        )
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
                    println("BULLET TARGET UPDATE: Bullet(${bullet.x}, ${bullet.y}) now targeting Enemy(${nearestEnemy.x}, ${nearestEnemy.y})")
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
            
            if (boss.enemyType == EnemyType.BOSS3) {
                println("BOSS3 DEBUG: 🧊 Cooldown check - timeSinceLastAbility: ${timeSinceLastAbility}ms, abilityCooldownMs: ${boss.abilityCooldownMs}ms, canUseAbility: $canUseAbility")
            }
            
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
                    // Debug: Log collision
                    println("BULLET HIT! Bullet(${bullet.x}, ${bullet.y}) hit Enemy(${currentEnemy.x}, ${currentEnemy.y})")
                    
                    // Enemy nhận damage
                    currentEnemy = currentEnemy.takeDamage(bullet.damage)
                    bulletsToRemove.add(bullet.id)
                    hit = true
                    
                    // Xử lý đặc biệt cho đạn băng
                    if (bullet.heroType == HeroType.ICE) {
                        // Làm chậm enemy (100% chance)
                        currentEnemy = currentEnemy.slow(2000L, 0.3f) // 2 giây, tốc độ còn 30%
                        
                        // Tỷ lệ đóng băng (30% chance)
                        val freezeChance = 0.3f
                        if (kotlin.random.Random.nextFloat() < freezeChance) {
                            currentEnemy = currentEnemy.freeze(1500L) // 1.5 giây đóng băng
                            
                            // Thêm hiệu ứng đóng băng
                            updatedState = effectSystem.addEffect(updatedState, Effect.createFreeze(currentEnemy.x, currentEnemy.y, 1500L))
                            
                            println("❄️ FREEZE! Enemy ${currentEnemy.id} frozen for 1.5s")
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
                        soundSystem.playEnemyDeathSound(currentEnemy)
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
        
        val aliveEnemies = state.enemies.filter { it.isAlive }
        
        // Debug: Log bullet count
        if (finalBullets.size != state.bullets.size) {
            println("BULLET CLEANUP: Removed ${state.bullets.size - finalBullets.size} bullets. Remaining: ${finalBullets.size}")
        }
        
        return state.copy(
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
            soundSystem.playEnemyDeathSound(enemy)
            updatedState = updatedState.killEnemy(enemy.id)
        }

        // Kiểm tra boss chạm đáy - nếu có boss thì game over ngay
        val bossesAtBottom = enemiesAtBottom.filter { it.isBoss }
        if (bossesAtBottom.isNotEmpty()) {
            // Boss chạm đáy = game over ngay lập tức
            val gameOverPlayer = updatedState.player.copy(lives = 0)
            updatedState = updatedState.copy(player = gameOverPlayer)
            
            println("💀 GAME OVER! Boss reached bottom!")
            bossesAtBottom.forEach { boss ->
                println("   - Boss ${boss.enemyType} reached bottom - INSTANT DEFEAT!")
            }
        } else {
            // Trừ lives của player dựa trên HP còn lại của enemies thường chạm đáy (có cân đối)
            val normalEnemiesAtBottom = enemiesAtBottom.filter { !it.isBoss }
            val damageToPlayer = normalEnemiesAtBottom.sumOf { enemy -> 
                // Tính damage dựa trên HP còn lại, nhưng có giới hạn để cân đối
                val remainingHp = enemy.currentHp.coerceAtLeast(1f)
                
                // Chuyển đổi HP enemy thành damage player với tỷ lệ cân đối
                // Công thức: sqrt(HP) * 2 để giảm damage khi HP cao
                val rawDamage = kotlin.math.sqrt(remainingHp) * 2f
                
                // Giới hạn damage tối đa mỗi enemy là 10 HP
                kotlin.math.ceil(rawDamage).toInt().coerceIn(1, 10)
            }
            val newLives = (updatedState.player.lives - damageToPlayer).coerceAtLeast(0)
            val updatedPlayer = updatedState.player.copy(lives = newLives)
            updatedState = updatedState.copy(player = updatedPlayer)
        }
        
        // Debug log để theo dõi damage (chỉ cho enemies thường)
        val normalEnemiesAtBottom = enemiesAtBottom.filter { !it.isBoss }
        if (normalEnemiesAtBottom.isNotEmpty()) {
            val damageToPlayer = normalEnemiesAtBottom.sumOf { enemy -> 
                val remainingHp = enemy.currentHp.coerceAtLeast(1f)
                val rawDamage = kotlin.math.sqrt(remainingHp) * 2f
                kotlin.math.ceil(rawDamage).toInt().coerceIn(1, 10)
            }
            println("🔥 ENEMY REACHED BOTTOM! ${normalEnemiesAtBottom.size} normal enemies, total damage: $damageToPlayer HP")
            normalEnemiesAtBottom.forEach { enemy ->
                val remainingHp = enemy.currentHp.coerceAtLeast(1f)
                val rawDamage = kotlin.math.sqrt(remainingHp) * 2f
                val finalDamage = kotlin.math.ceil(rawDamage).toInt().coerceIn(1, 10)
                println("   - Enemy HP: ${enemy.currentHp}/${enemy.maxHp}, damage dealt: $finalDamage")
            }
        }

        return updatedState.copy(
            enemies = remainingEnemies
        )
    }
}
