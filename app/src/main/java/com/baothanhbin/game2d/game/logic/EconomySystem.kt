package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.Player

/**
 * Hệ thống kinh tế - quản lý income, interest, level, XP
 */
class EconomySystem {
    
    /**
     * Áp dụng thu nhập đầu round
     */
    fun applyRoundIncome(player: Player): Player {
        val totalIncome = player.totalIncome
        
        return player.copy(
            gold = player.gold + totalIncome
        )
    }
    
    /**
     * Mua XP
     */
    fun buyXP(player: Player): Player {
        if (!player.canBuyXP) return player
        
        val newXP = player.xp + Player.XP_GAIN
        val newGold = player.gold - Player.XP_COST
        
        // Kiểm tra có level up không
        return checkLevelUp(player.copy(gold = newGold, xp = newXP))
    }
    
    /**
     * Kiểm tra và thực hiện level up
     */
    private fun checkLevelUp(player: Player): Player {
        val xpNeeded = player.xpNeededForNextLevel
        
        return if (player.xp >= xpNeeded && player.level < Player.MAX_LEVEL) {
            player.copy(
                level = player.level + 1,
                xp = player.xp - xpNeeded
            )
        } else {
            player
        }
    }
}
