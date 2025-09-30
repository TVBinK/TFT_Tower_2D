package com.baothanhbin.game2d.game.model

import java.util.UUID

/**
 * Bullet - đạn bắn từ tướng
 */
data class Bullet(
    val id: String = UUID.randomUUID().toString(),
    val x: Float,
    val y: Float,
    val damage: Float,
    val speed: Float,
    val size: Float = 8f,
    val heroType: HeroType,
    val targetEnemyId: String? = null,
    val targetX: Float? = null,
    val targetY: Float? = null
) {
    
    /**
     * Di chuyển đạn thẳng lên trên
     */
    fun moveUp(deltaTime: Float): Bullet {
        // Di chuyển thẳng lên không có random
        val newX = x
        val newY = y - speed * deltaTime
        return copy(x = newX, y = newY)
    }
    
    /**
     * Kiểm tra đạn có ra khỏi màn hình không
     */
    fun isOffScreen(screenHeight: Float): Boolean {
        return y < -size
    }
    
    /**
     * Kiểm tra va chạm với enemy
     */
    fun collidesWith(enemy: Enemy): Boolean {
        val dx = x - enemy.x
        val dy = y - enemy.y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        return distance <= (size + enemy.size) / 2
    }
    
    companion object {
        /**
         * Tạo bullet mới từ unit
         */
        fun create(
            unit: Unit,
            startX: Float,
            startY: Float,
            targetEnemy: Enemy? = null
        ): Bullet {
            return Bullet(
                x = startX,
                y = startY,
                damage = unit.actualDamage,
                speed = unit.bulletSpeed,
                heroType = unit.type,
                targetEnemyId = targetEnemy?.id,
                targetX = targetEnemy?.x,
                targetY = targetEnemy?.y
            )
        }
    }
}
