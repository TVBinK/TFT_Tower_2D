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
    
    /**
     * Tính toán interest
     */
    fun calculateInterest(gold: Int): Int {
        return minOf(gold / 10 * Player.INTEREST_PER_10_GOLD, Player.MAX_INTEREST)
    }
    
    /**
     * Tính streak bonus
     */
    fun calculateStreakBonus(winStreak: Int, loseStreak: Int): Int {
        return when {
            winStreak >= 3 -> 3
            winStreak >= 2 -> 2
            loseStreak >= 3 -> 2
            loseStreak >= 2 -> 1
            else -> 0
        }
    }
    
    /**
     * Cập nhật win streak
     */
    fun updateWinStreak(player: Player): Player {
        return player.copy(
            winStreak = player.winStreak + 1,
            loseStreak = 0
        )
    }
    
    /**
     * Cập nhật lose streak
     */
    fun updateLoseStreak(player: Player): Player {
        return player.copy(
            winStreak = 0,
            loseStreak = player.loseStreak + 1
        )
    }
    
    /**
     * Tính tổng thu nhập của round
     */
    fun calculateTotalIncome(player: Player): Int {
        val baseIncome = Player.BASE_INCOME
        val interest = calculateInterest(player.gold)
        val streakBonus = calculateStreakBonus(player.winStreak, player.loseStreak)
        
        return baseIncome + interest + streakBonus
    }
    
    /**
     * Breakdown thu nhập để hiển thị
     */
    data class IncomeBreakdown(
        val baseIncome: Int,
        val interest: Int,
        val streakBonus: Int,
        val total: Int
    )
    
    fun getIncomeBreakdown(player: Player): IncomeBreakdown {
        val baseIncome = Player.BASE_INCOME
        val interest = calculateInterest(player.gold)
        val streakBonus = calculateStreakBonus(player.winStreak, player.loseStreak)
        
        return IncomeBreakdown(
            baseIncome = baseIncome,
            interest = interest,
            streakBonus = streakBonus,
            total = baseIncome + interest + streakBonus
        )
    }
}
