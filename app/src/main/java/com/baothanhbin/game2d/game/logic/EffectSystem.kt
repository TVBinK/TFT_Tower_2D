package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*

/**
 * Hệ thống quản lý hiệu ứng hình ảnh
 */
class EffectSystem {
    
    /**
     * Cập nhật tất cả hiệu ứng
     */
    fun updateEffects(state: GameState, deltaTimeMs: Long): GameState {
        var newState = state
        val updatedEffects = state.effects.map { effect ->
            // Áp dụng damage theo thời gian cho FIRE_ROW
            if (effect.type == EffectType.FIRE_ROW) {
                // Trừ 1% máu tối đa mỗi giây khi chạm vào lửa
                val seconds = (deltaTimeMs / 1000f)
                val rowTop = effect.y - effect.size / 2f
                val rowBottom = effect.y + effect.size / 2f
                val damaged = newState.enemies.map { e ->
                    if (e.isAlive && e.y in rowTop..rowBottom) {
                        // Enemy bị cháy - trừ máu theo thời gian 5% maxHp mỗi giây
                        val damageThisTick = 0.05f * e.maxHp * seconds
                        e.takeDamage(damageThisTick)
                    } else e
                }
                newState = newState.copy(enemies = damaged)
                effect.update(deltaTimeMs)
            }
            // Xử lý WAVE - đẩy lùi enemy (hàng ngang) - chỉ đẩy 1 lần mỗi enemy
            else if (effect.type == EffectType.WAVE) {
                val waveHeight = effect.size * effect.scale // Chiều cao của hàng
                val waveWidth = effect.width * effect.scale // Chiều rộng của hàng
                val waveY = effect.y
                val waveLeft = effect.x
                val waveRight = effect.x + waveWidth
                val waveTop = waveY - waveHeight / 2f
                val waveBottom = waveY + waveHeight / 2f
                
                var newKnockedBackEnemies = effect.knockedBackEnemies
                val knockedBack = newState.enemies.map { e ->
                    if (e.isAlive) {
                        // Kiểm tra enemy có trong vùng hàng ngang và chưa bị đẩy chưa
                        val inHorizontalRange = e.x >= waveLeft && e.x <= waveRight
                        val inVerticalRange = e.y >= waveTop && e.y <= waveBottom
                        val notYetKnockedBack = !effect.knockedBackEnemies.contains(e.id)
                        
                        if (inHorizontalRange && inVerticalRange && notYetKnockedBack) {
                            // Đẩy lùi enemy lên cao (giảm y) - sóng đẩy từ dưới lên trên
                            val knockbackForce = 100f // Lực đẩy lên cao
                            newKnockedBackEnemies = newKnockedBackEnemies + e.id
                            e.copy(y = e.y - knockbackForce)
                        } else e
                    } else e
                }
                newState = newState.copy(enemies = knockedBack)
                
                // Trả về effect đã cập nhật với danh sách enemy đã bị đẩy
                effect.copy(knockedBackEnemies = newKnockedBackEnemies).update(deltaTimeMs)
            }
            else {
                effect.update(deltaTimeMs)
            }
        }.filter { !it.isFinished }
        
        return newState.copy(effects = updatedEffects)
    }
    
    /**
     * Thêm hiệu ứng nổ khi enemy chết
     */
    fun addEnemyDeathEffect(state: GameState, enemy: Enemy): GameState {
        val explosionEffect = Effect.createExplosion(
            x = enemy.x,
            y = enemy.y,
            color = enemy.enemyType.color
        )
        
        val smokeEffect = Effect.createSmoke(
            x = enemy.x,
            y = enemy.y
        )
        
        return state.copy(
            effects = state.effects + explosionEffect + smokeEffect
        )
    }
    
    /**
     * Thêm hiệu ứng bắn đạn
     */
    fun addMuzzleFlashEffect(state: GameState, unit: com.baothanhbin.game2d.game.model.Unit, slot: BoardSlot): GameState {
        val (unitX, unitY) = getUnitPosition(slot)
        val muzzleFlash = Effect.createMuzzleFlash(
            x = unitX,
            y = unitY,
            color = unit.type.color
        )
        
        return state.copy(
            effects = state.effects + muzzleFlash
        )
    }
    
    /**
     * Tạo hàng lửa tại vị trí enemy gần nhất
     */
    fun addFireRowAtEnemyPosition(state: GameState, enemy: Enemy, durationMs: Long, dps: Float, thickness: Float = 30f): GameState {
        // Tạo hàng lửa tại vị trí Y của enemy
        val fireY = enemy.y
        val fire = Effect.createFireRow(y = fireY, durationMs = durationMs, dps = dps, thickness = thickness)
        return state.copy(effects = state.effects + fire)
    }
    
    /**
     * Thêm hiệu ứng va chạm đạn
     */
    fun addHitEffect(state: GameState, bullet: Bullet, enemy: Enemy): GameState {
        val hitSpark = Effect.createHitSpark(
            x = enemy.x,
            y = enemy.y,
            color = bullet.heroType.color
        )
        
        return state.copy(
            effects = state.effects + hitSpark
        )
    }
    
    /**
     * Thêm hiệu ứng generic
     */
    fun addEffect(state: GameState, effect: Effect): GameState {
        return state.copy(
            effects = state.effects + effect
        )
    }
    
    /**
     * Lấy vị trí của unit trên màn hình
     */
    private fun getUnitPosition(slot: BoardSlot): Pair<Float, Float> {
        val slotWidth = GameState.SCREEN_WIDTH / 5f
        val x = slot.position * slotWidth + slotWidth / 2f
        val y = GameState.SCREEN_HEIGHT - 150f // Gần đáy màn hình
        
        return Pair(x, y)
    }
    
    /**
     * Dọn dẹp hiệu ứng đã kết thúc
     */
    fun cleanupEffects(state: GameState): GameState {
        val activeEffects = state.effects.filter { !it.isFinished }
        return state.copy(effects = activeEffects)
    }
}
