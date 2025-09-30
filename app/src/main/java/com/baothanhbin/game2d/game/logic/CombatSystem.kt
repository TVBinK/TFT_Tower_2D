package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*
import kotlin.math.sqrt

/**
 * Hệ thống combat - quản lý bắn đạn, va chạm, di chuyển
 */
class CombatSystem {
    
    private val effectSystem = EffectSystem()
    private val soundSystem = SoundSystem()
    
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
            // Thêm delay khởi động: yêu cầu mana đầu trận phải tích lũy đủ trước khi bắn
            if (currentPlayer.currentMana < unit.manaCost) continue
            
            // Kiểm tra có đủ mana để bắn không (đã check trước)
            
            // Tìm enemy gần nhất trong tầm bắn
            val target = findNearestEnemyInRange(unit, enemies, slot)
            
            if (target != null) {
                // Hệ Hỏa: tạo hàng lửa thay vì bắn đạn
                if (unit.type == HeroType.HOA) {
                    val fireDuration = 20000L // lâu hơn
                    val fireDps = unit.actualDamage * 2f
                    updatedState = effectSystem.addFireRow(updatedState, slot, fireDuration, fireDps, thickness = 90f)
                    val updatedUnit = unit.copy(
                        cooldownRemainingMs = unit.actualFireRateMs,
                        lastShotAtMs = System.currentTimeMillis()
                    )
                    updatedBoard[slot] = updatedUnit
                    continue
                }
                // Sử dụng mana để bắn
                val manaUsed = currentPlayer.useMana(unit.manaCost)
                if (manaUsed == null) {
                    println("FAILED TO USE MANA! Unit needs ${unit.manaCost} mana")
                    continue
                }
                currentPlayer = manaUsed
                
                // Tạo bullet thẳng từ vị trí tướng
                val (baseX, baseY) = getUnitPosition(slot)
                val bulletX = baseX
                val bulletY = baseY
                
                // Giảm số viên bắn mỗi lượt một chút để tổng lượng đạn ít hơn
                val bulletsPerShot = kotlin.math.max(1, unit.tier.cost - 1)
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
                    cooldownRemainingMs = unit.actualFireRateMs,
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
     * Player bắn thủ công (khi tap)
     */
    fun manualShoot(state: GameState, slot: BoardSlot): GameState {
        val unit = state.player.board[slot] ?: return state
        if (unit.cooldownRemainingMs > 0) return state
        
        val enemies = state.enemies.filter { it.isAlive }
        val target = findNearestEnemyInRange(unit, enemies, slot)
        
        if (target == null) {
            println("NO TARGET FOUND for manual shoot at slot $slot")
            return state
        }
        
        // Kiểm tra mana
        if (state.player.currentMana < unit.manaCost) {
            println("NOT ENOUGH MANA for manual shoot! Need ${unit.manaCost}, have ${state.player.currentMana}")
            return state
        }
        
        // Sử dụng mana
        val updatedPlayer = state.player.useMana(unit.manaCost) ?: return state
        
        // Tạo bullet
        val (baseX, baseY) = getUnitPosition(slot)
        val randomOffsetX = (Math.random() * 10f - 5f).toFloat()
        val randomOffsetY = (Math.random() * 5f - 2.5f).toFloat()
        val bulletX = baseX + randomOffsetX
        val bulletY = baseY + randomOffsetY
        
        val bulletsPerShot = unit.tier.cost.coerceIn(1, 5)
        val baseAngle = kotlin.math.atan2((target.y - bulletY), (target.x - bulletX))
        val totalSpread = 0.30f
        val step = if (bulletsPerShot > 1) totalSpread / (bulletsPerShot - 1) else 0f
        val startAngle = baseAngle - totalSpread / 2f

        val newBullets = mutableListOf<Bullet>()
        repeat(bulletsPerShot) { index ->
            val angle = startAngle + index * step
            val range = 60f
            val tx = bulletX + kotlin.math.cos(angle) * range
            val ty = bulletY + kotlin.math.sin(angle) * range
            val b = Bullet.create(unit, bulletX, bulletY, target.copy(x = tx, y = ty))
            newBullets.add(b)
        }
        
        // Reset cooldown và đánh dấu thời điểm bắn
        val updatedUnit = unit.copy(
            cooldownRemainingMs = unit.actualFireRateMs,
            lastShotAtMs = System.currentTimeMillis()
        )
        val updatedBoard = state.player.board.toMutableMap().apply { put(slot, updatedUnit) }
        
        println("MANUAL SHOOT! Unit at slot $slot - Mana used: ${unit.manaCost}, Remaining: ${updatedPlayer.currentMana}")
        
        return state.copy(
            bullets = state.bullets + newBullets,
            player = updatedPlayer.copy(board = updatedBoard)
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
            enemy.moveDown(deltaTimeSeconds)
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
