package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*

/**
 * Hệ thống âm thanh (placeholder - sẽ tích hợp với MediaPlayer sau)
 */
class SoundSystem {
    /**
     * Phát âm thanh enemy chết
     */
    fun playEnemyDeathSound(enemy: Enemy) {
        when (enemy.enemyType) {
            EnemyType.BASIC -> playSound("death_basic.wav")
            EnemyType.FAST -> playSound("death_fast.wav")
            EnemyType.TANK -> playSound("death_tank.wav")
            EnemyType.BOSS1 -> playSound("death_robe.wav")
            EnemyType.BOSS2 -> playSound("death_boss2.wav")
            EnemyType.BOSS3 -> playSound("death_boss3.wav")
            EnemyType.MURID -> playSound("death_murid.wav")
        }
    }
    
    /**
     * Phát âm thanh mua unit
     */
    fun playBuySound(unit: com.baothanhbin.game2d.game.model.Unit) {
        playSound("buy_unit.wav")
    }

    /**
     * Phát âm thanh game over
     */
    fun playGameOverSound() {
        playSound("game_over.wav")
    }
    


    /**
     * Placeholder cho việc phát âm thanh
     * Trong thực tế sẽ tích hợp với MediaPlayer hoặc SoundPool
     */
    private fun playSound(soundFile: String) {
        // TODO: Implement actual sound playing
        // Log.d("SoundSystem", "Playing sound: $soundFile")
    }
}
