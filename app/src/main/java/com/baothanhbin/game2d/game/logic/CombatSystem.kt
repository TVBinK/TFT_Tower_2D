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
            unit?.let { updateUnitCooldown(it, deltaTimeMs) }
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
     * Cập nhật: Tướng cần mana để bắn, không tự động bắn
     */
    fun unitsShoot(state: GameState): GameState {
        // Cập nhật mana trước
        val updatedPlayer = state.player.updateMana(System.currentTimeMillis())
        val activeUnits = getActiveUnits(updatedPlayer)
        val enemies = state.enemies.filter { it.isAlive }
        
        // Debug: Log shooting info
        val unitsOnBoard = state.player.board.values.count { it != null }
        android.util.Log.d("CombatSystem", "🔫 SHOOTING: unitsOnBoard=$unitsOnBoard, enemies=${enemies.size}, mana=${updatedPlayer.currentMana}")
        
        // Giới hạn số lượng đạn tối đa để tránh spam, hạ ngưỡng ngay đầu trận
        val bulletCap = 30
        if (state.bullets.size > bulletCap) {
            println("TOO MANY BULLETS! Current: ${state.bullets.size}, skipping bullet creation")
            return state.copy(player = updatedPlayer)
        }
        
        val newBullets = mutableListOf<Bullet>()
        val updatedBoard = updatedPlayer.board.toMutableMap()
        var currentPlayer = updatedPlayer
        var updatedState = state
        
        for ((slot, unit) in state.player.board) {
            if (unit == null || unit.cooldownRemainingMs > 0) continue
            // Không chặn sớm theo mana để tránh ảnh hưởng slot khác; hệ cần mana sẽ tự kiểm tra bên trong
            
            // Kiểm tra có đủ mana để bắn không (đã check trước)
            
            // Tìm enemy gần nhất trong tầm bắn
            val target = findNearestEnemyInRange(unit, enemies, slot)
            
            if (target != null) {
                // Hệ Hỏa: tạo hàng lửa thay vì bắn đạn
                if (unit.type == HeroType.FIRE) {
                    val fireDuration = 20000L // lâu hơn
                    val fireDps = unit.actualDamage * 2f
                    // Tăng chiều cao cột lửa
                    val fireThickness = 150f
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
                        val waveDuration = 3000L // 3 giây (giảm từ 8 giây)
                        val waveHeight = 200f // Chiều cao sóng giảm (từ 400f)
                        updatedState = effectSystem.addWave(updatedState, slot, waveDuration, waveHeight)
                        
                        android.util.Log.d("THUY_WAVE", "🌊 THUY Wave CREATED! Height=${waveHeight}")
                        
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
                    val updatedUnit = unit.copy(
                        cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
                        lastShotAtMs = System.currentTimeMillis()
                    )
                    updatedBoard[slot] = updatedUnit
                    continue
                }
                // Hệ Băng: bắn đạn băng
                else if (unit.type == HeroType.ICE) {
                    // Bỏ yêu cầu mana: mọi unit đều có thể dùng skill/bắn
                    
                    // Tạo bullet băng từ vị trí tướng
                    val (baseX, baseY) = getUnitPosition(slot)
                    val bulletX = baseX
                    val bulletY = baseY
                    
                    // Giảm số viên bắn mỗi lượt một chút để tổng lượng đạn ít hơn
                    val bulletsPerShot = 1 // Cố định 1 viên cho ICE
                    // Spread góc nhẹ để fan ra, tâm vẫn hướng về target
                    val baseAngle = kotlin.math.atan2((target.y - bulletY), (target.x - bulletX))
                    val totalSpread = 0.30f // ~17 độ tổng
                    val step = if (bulletsPerShot > 1) totalSpread / (bulletsPerShot - 1) else 0f
                    val startAngle = baseAngle - totalSpread / 2f

                    repeat(bulletsPerShot) { index ->
                        val angle = startAngle + index * step
                        val range = 60f // giữ nguyên tầm bắn
                        val tx = bulletX + kotlin.math.cos(angle) * range
                        val ty = bulletY + kotlin.math.sin(angle) * range
                        val b = Bullet.create(unit, bulletX, bulletY, target.copy(x = tx, y = ty))
                        newBullets.add(b)
                    }
                    
                    // Thêm hiệu ứng lửa nòng súng
                    updatedState = effectSystem.addMuzzleFlashEffect(updatedState, unit, slot)
                    
                    // Phát âm thanh bắn
                    soundSystem.playShootSound(unit)
                    
                    val updatedUnit = unit.copy(
                        cooldownRemainingMs = (unit.actualFireRateMs * FIRE_RATE_MULTIPLIER).toLong(),
                        lastShotAtMs = System.currentTimeMillis()
                    )
                    updatedBoard[slot] = updatedUnit
                    continue
                }
                // Bỏ yêu cầu mana cho các hệ còn lại
                
                // Tạo bullet thẳng từ vị trí tướng
                val (baseX, baseY) = getUnitPosition(slot)
                val bulletX = baseX
                val bulletY = baseY
                
                // Giảm số viên bắn mỗi lượt một chút để tổng lượng đạn ít hơn
                val bulletsPerShot = 1 // Cố định 1 viên cho các hệ khác
                // Spread góc nhẹ để fan ra, tâm vẫn hướng về target
                val baseAngle = kotlin.math.atan2((target.y - bulletY), (target.x - bulletX))
                val totalSpread = 0.30f // ~17 độ tổng
                val step = if (bulletsPerShot > 1) totalSpread / (bulletsPerShot - 1) else 0f
                val startAngle = baseAngle - totalSpread / 2f

                repeat(bulletsPerShot) { index ->
                    val angle = startAngle + index * step
                    val range = 60f // giữ nguyên tầm bắn
                    val tx = bulletX + kotlin.math.cos(angle) * range
                    val ty = bulletY + kotlin.math.sin(angle) * range
                    val b = Bullet.create(unit, bulletX, bulletY, target.copy(x = tx, y = ty))
                    newBullets.add(b)
                }
                
                // Debug: Log bullet creation với thông tin mana
                val targetColumn = ((target.x / (GameState.SCREEN_WIDTH / 5f)).toInt()).coerceIn(0, 4)
                val unitColumn = slot.position
                println("BULLET CREATED! Unit at column $unitColumn targeting Enemy at column $targetColumn - Mana used: ${unit.manaCost}, Remaining: ${currentPlayer.currentMana}")
                
                // Thêm hiệu ứng lửa nòng súng
                updatedState = effectSystem.addMuzzleFlashEffect(updatedState, unit, slot)
                
                // Phát âm thanh bắn
                soundSystem.playShootSound(unit)
                
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
            player = currentPlayer.copy(board = updatedBoard)
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
            statusUpdatedEnemy.moveDown(deltaTimeSeconds)
        }
        
        return state.copy(enemies = updatedEnemies)
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
                    
                    // Phát âm thanh va chạm
                    soundSystem.playHitSound(bullet, currentEnemy)
                    
                    // Nếu enemy chết, cộng reward và thêm hiệu ứng nổ
                    if (currentEnemy.isDead) {
                        goldGained += currentEnemy.reward
                        scoreGained += (currentEnemy.reward * 10).toLong()
                        updatedState = effectSystem.addEnemyDeathEffect(updatedState, currentEnemy)
                        soundSystem.playEnemyDeathSound(currentEnemy)
                        // Cập nhật số enemy bị tiêu diệt
                        updatedState = updatedState.killEnemy(currentEnemy.id)
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
     * Kiểm tra enemies có chạm đáy màn hình không
     */
    fun checkEnemiesReachedBottom(state: GameState): GameState {
        val bottomY = GameState.SCREEN_HEIGHT + 50f
        val (enemiesReachedBottom, remainingEnemies) = state.enemies.partition { 
            it.y >= bottomY 
        }
        
        if (enemiesReachedBottom.isEmpty()) {
            return state.copy(enemies = remainingEnemies)
        }
        
        // Trừ lives
        val livesLost = enemiesReachedBottom.size
        val updatedPlayer = state.player.copy(
            lives = (state.player.lives - livesLost).coerceAtLeast(0)
        )
        
        return state.copy(
            enemies = remainingEnemies,
            player = updatedPlayer
        )
    }
}
