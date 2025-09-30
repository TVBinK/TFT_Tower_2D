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
                        // Enemy bị cháy - trừ máu theo thời gian 1% maxHp mỗi giây
                        val damageThisTick = 0.01f * e.maxHp * seconds
                        e.takeDamage(damageThisTick)
                    } else e
                }
                newState = newState.copy(enemies = damaged)
            }
            effect.update(deltaTimeMs)
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
     * Tạo hàng lửa tại toạ độ Y của unit
     */
    fun addFireRow(state: GameState, slot: BoardSlot, durationMs: Long, dps: Float, thickness: Float = 30f): GameState {
        val (_, unitY) = getUnitPosition(slot)
        // Đặt hàng lửa cao hơn so với bottom panel (dịch lên trên xa hơn)
        val fireY = (unitY - 500f).coerceAtLeast(0f)
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
     * Thêm hiệu ứng vệt đạn
     */
    fun addBulletTrailEffect(state: GameState, bullet: Bullet): GameState {
        val trail = Effect.createTrail(
            x = bullet.x,
            y = bullet.y,
            color = bullet.heroType.color
        )
        
        return state.copy(
            effects = state.effects + trail
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
