package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*

/**
 * Hệ thống âm thanh (placeholder - sẽ tích hợp với MediaPlayer sau)
 */
class SoundSystem {
    
    /**
     * Phát âm thanh bắn đạn
     */
    fun playShootSound(unit: com.baothanhbin.game2d.game.model.Unit) {
        // TODO: Tích hợp với MediaPlayer để phát âm thanh
        // Có thể sử dụng SoundPool hoặc MediaPlayer
        when (unit.type) {
            HeroType.METAL -> playSound("shoot_kim.wav")
            HeroType.FLOWER -> playSound("shoot_moc.wav")
            HeroType.WATER -> playSound("shoot_thuy.wav")
            HeroType.FIRE -> playSound("shoot_hoa.wav")
            HeroType.ICE -> playSound("shoot_tho.wav")
        }
    }
    
    /**
     * Phát âm thanh va chạm
     */
    fun playHitSound(bullet: Bullet, enemy: Enemy) {
        when (enemy.enemyType) {
            EnemyType.BASIC -> playSound("hit_basic.wav")
            EnemyType.FAST -> playSound("hit_fast.wav")
            EnemyType.TANK -> playSound("hit_tank.wav")
            EnemyType.ROBE -> playSound("hit_robe.wav")
        }
    }
    
    /**
     * Phát âm thanh enemy chết
     */
    fun playEnemyDeathSound(enemy: Enemy) {
        when (enemy.enemyType) {
            EnemyType.BASIC -> playSound("death_basic.wav")
            EnemyType.FAST -> playSound("death_fast.wav")
            EnemyType.TANK -> playSound("death_tank.wav")
            EnemyType.ROBE -> playSound("death_robe.wav")
        }
    }
    
    /**
     * Phát âm thanh mua unit
     */
    fun playBuySound(unit: com.baothanhbin.game2d.game.model.Unit) {
        playSound("buy_unit.wav")
    }
    
    /**
     * Phát âm thanh merge unit
     */
    fun playMergeSound(unit: com.baothanhbin.game2d.game.model.Unit) {
        playSound("merge_unit.wav")
    }
    
    /**
     * Phát âm thanh bán unit
     */
    fun playSellSound(unit: com.baothanhbin.game2d.game.model.Unit) {
        playSound("sell_unit.wav")
    }
    
    /**
     * Phát âm thanh game over
     */
    fun playGameOverSound() {
        playSound("game_over.wav")
    }
    
    /**
     * Phát âm thanh level up
     */
    fun playLevelUpSound() {
        playSound("level_up.wav")
    }
    
    /**
     * Phát âm thanh chuyển phase
     */
    fun playPhaseChangeSound(phase: RoundPhase) {
        when (phase) {
            RoundPhase.PREP -> playSound("phase_prep.wav")
            RoundPhase.COMBAT -> playSound("phase_combat.wav")
        }
    }
    
    /**
     * Placeholder cho việc phát âm thanh
     * Trong thực tế sẽ tích hợp với MediaPlayer hoặc SoundPool
     */
    private fun playSound(soundFile: String) {
        // TODO: Implement actual sound playing
        // Log.d("SoundSystem", "Playing sound: $soundFile")
    }
    
    /**
     * Preload các âm thanh thường dùng
     */
    fun preloadSounds() {
        // TODO: Preload sounds for better performance
    }
    
    /**
     * Dừng tất cả âm thanh
     */
    fun stopAllSounds() {
        // TODO: Stop all playing sounds
    }
}
