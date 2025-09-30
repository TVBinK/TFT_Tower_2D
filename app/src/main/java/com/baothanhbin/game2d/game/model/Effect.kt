package com.baothanhbin.game2d.game.model

import java.util.UUID

/**
 * Hiệu ứng hình ảnh trong game
 */
data class Effect(
    val id: String = UUID.randomUUID().toString(),
    val type: EffectType,
    val x: Float,
    val y: Float,
    val durationMs: Long,
    val currentTimeMs: Long = 0L,
    val size: Float = 20f,
    val color: Int = 0xFFFFFFFF.toInt(),
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val damagePerSecond: Float = 0f
) {
    
    /**
     * Hiệu ứng đã kết thúc chưa
     */
    val isFinished: Boolean
        get() = currentTimeMs >= durationMs
    
    /**
     * Cập nhật thời gian hiệu ứng
     */
    fun update(deltaTimeMs: Long): Effect {
        val newTime = currentTimeMs + deltaTimeMs
        val progress = (newTime.toFloat() / durationMs).coerceIn(0f, 1f)
        
        return copy(
            currentTimeMs = newTime,
            alpha = when (type) {
                EffectType.EXPLOSION -> 1f - progress
                EffectType.MUZZLE_FLASH -> if (progress < 0.3f) 1f else 1f - ((progress - 0.3f) / 0.7f)
                EffectType.HIT_SPARK -> 1f - progress
                EffectType.TRAIL -> 1f - progress
                EffectType.SMOKE -> 1f - (progress * 0.5f)
                EffectType.FIRE_ROW -> 1f // Giữ alpha ổn định cho hàng lửa
            },
            scale = when (type) {
                EffectType.EXPLOSION -> 1f + progress * 2f
                EffectType.MUZZLE_FLASH -> 1f + progress * 0.5f
                EffectType.HIT_SPARK -> 1f + progress * 0.3f
                EffectType.TRAIL -> 1f - progress * 0.5f
                EffectType.SMOKE -> 1f + progress * 1.5f
                EffectType.FIRE_ROW -> 1f
            },
            rotation = rotation + (deltaTimeMs * 0.01f) // Quay chậm
        )
    }
    
    companion object {
        /**
         * Tạo hiệu ứng nổ
         */
        fun createExplosion(x: Float, y: Float, color: Int = 0xFFFF4444.toInt()): Effect {
            return Effect(
                type = EffectType.EXPLOSION,
                x = x,
                y = y,
                durationMs = 500L,
                size = 30f,
                color = color
            )
        }
        
        /**
         * Tạo hiệu ứng lửa nòng súng
         */
        fun createMuzzleFlash(x: Float, y: Float, color: Int = 0xFFFFAA00.toInt()): Effect {
            return Effect(
                type = EffectType.MUZZLE_FLASH,
                x = x,
                y = y,
                durationMs = 200L,
                size = 15f,
                color = color
            )
        }
        
        /**
         * Tạo hiệu ứng va chạm
         */
        fun createHitSpark(x: Float, y: Float, color: Int = 0xFFFFFF00.toInt()): Effect {
            return Effect(
                type = EffectType.HIT_SPARK,
                x = x,
                y = y,
                durationMs = 300L,
                size = 12f,
                color = color
            )
        }
        
        /**
         * Tạo hiệu ứng vệt đạn
         */
        fun createTrail(x: Float, y: Float, color: Int = 0xFFFFFFFF.toInt()): Effect {
            return Effect(
                type = EffectType.TRAIL,
                x = x,
                y = y,
                durationMs = 150L,
                size = 8f,
                color = color,
                alpha = 0.6f
            )
        }
        
        /**
         * Tạo hiệu ứng khói
         */
        fun createSmoke(x: Float, y: Float, color: Int = 0xFF888888.toInt()): Effect {
            return Effect(
                type = EffectType.SMOKE,
                x = x,
                y = y,
                durationMs = 1000L,
                size = 25f,
                color = color,
                alpha = 0.7f
            )
        }

        /**
         * Tạo hàng lửa gây sát thương theo thời gian
         * size = độ dày (chiều cao) của hàng lửa
         */
        fun createFireRow(y: Float, durationMs: Long, dps: Float, thickness: Float = 30f): Effect {
            return Effect(
                type = EffectType.FIRE_ROW,
                x = 0f,
                y = y,
                durationMs = durationMs,
                size = thickness,
                color = 0xFFFF3D00.toInt(),
                alpha = 1f,
                damagePerSecond = dps
            )
        }
    }
}

/**
 * Các loại hiệu ứng
 */
enum class EffectType(val displayName: String) {
    EXPLOSION("Nổ"),
    MUZZLE_FLASH("Lửa nòng súng"),
    HIT_SPARK("Tia lửa va chạm"),
    TRAIL("Vệt đạn"),
    SMOKE("Khói"),
    FIRE_ROW("Hàng lửa")
}
