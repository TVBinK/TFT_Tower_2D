package com.baothanhbin.game2d.game.model

/**
 * Player state - thông tin người chơi theo TFT style
 */
data class Player(
    val gold: Int = 10,
    val level: Int = 2,
    val xp: Int = 0,
    val bench: List<Unit> = createStartingUnits(),
    val board: Map<BoardSlot, Unit?> = BoardSlot.values().associateWith { null },
    val lives: Int = 100,
    val score: Long = 0L,
    val wave: Int = 1,
    val gameTimeMs: Long = 0L,
    val winStreak: Int = 0,
    val loseStreak: Int = 0,
    val freeRerollsRemaining: Int = 1,
    // Hệ thống Mana mới
    val currentMana: Float = 100f,
    val maxMana: Float = 100f,
    val manaRegenRate: Float = 10f, // Mana hồi phục mỗi giây
    val lastManaUpdateTime: Long = 0L
) {
    
    companion object {
        const val MAX_BENCH_SIZE = 9
        const val MAX_BOARD_SIZE = 5
        const val MAX_LEVEL = 9
        
        /**
         * Tạo tướng ban đầu cho player
         */
        private fun createStartingUnits(): List<Unit> {
            return listOf(
                Unit.create(HeroType.KIM, Tier.T1),
                Unit.create(HeroType.HOA, Tier.T1)
            )
        }
        
        /**
         * Tạo player với tướng ban đầu ở bench (board trống)
         */
        fun createWithStartingUnits(): Player {
            val startingUnits = createStartingUnits()
            
            // Tất cả tướng ban đầu ở bench, board trống hoàn toàn
            return Player(
                bench = startingUnits,
                board = BoardSlot.values().associateWith { null }
            )
        }
        
        // XP cần thiết để lên level
        val XP_REQUIREMENTS = mapOf(
            1 to 0,
            2 to 2,
            3 to 6,
            4 to 10,
            5 to 18,
            6 to 30,
            7 to 36,
            8 to 56,
            9 to 72
        )
        
        // Giá mua XP
        const val XP_COST = 4
        const val XP_GAIN = 4
        
        // Kinh tế
        const val BASE_INCOME = 5
        const val INTEREST_PER_10_GOLD = 1
        const val MAX_INTEREST = 5
        const val REROLL_COST = 2
    }
    
    /**
     * Số slot deploy tối đa (theo level)
     */
    val deployCap: Int
        get() = minOf(level, MAX_BOARD_SIZE)
    
    /**
     * Số unit đang được deploy trên board
     */
    val deployedCount: Int
        get() = board.values.count { it != null }
    
    /**
     * Có thể deploy thêm unit không
     */
    val canDeployMore: Boolean
        get() = deployedCount < deployCap
    
    /**
     * Bench có chỗ trống không
     */
    val hasBenchSpace: Boolean
        get() = bench.size < MAX_BENCH_SIZE
    
    /**
     * XP cần thiết để lên level tiếp theo
     */
    val xpNeededForNextLevel: Int
        get() = if (level >= MAX_LEVEL) 0 else (XP_REQUIREMENTS[level + 1] ?: 0)
    
    /**
     * Có thể mua XP không
     */
    val canBuyXP: Boolean
        get() = gold >= XP_COST && level < MAX_LEVEL
    
    /**
     * Có thể reroll không
     */
    val canReroll: Boolean
        get() = gold >= REROLL_COST || freeRerollsRemaining > 0
    
    /**
     * Interest nhận được (mỗi 10 vàng = 1 interest, tối đa 5)
     */
    val interest: Int
        get() = minOf(gold / 10 * INTEREST_PER_10_GOLD, MAX_INTEREST)
    
    /**
     * Có đủ mana để bắn không
     */
    val hasEnoughMana: Boolean
        get() = currentMana >= 20f // Cost cơ bản để bắn
    
    /**
     * Mana hiện tại dưới dạng phần trăm
     */
    val manaPercentage: Float
        get() = (currentMana / maxMana).coerceIn(0f, 1f)
    
    /**
     * Tổng thu nhập đầu vòng
     */
    val totalIncome: Int
        get() = BASE_INCOME + interest + streakBonus
    
    /**
     * Bonus từ streak
     */
    val streakBonus: Int
        get() = when {
            winStreak >= 3 -> 3
            winStreak >= 2 -> 2
            loseStreak >= 3 -> 2
            loseStreak >= 2 -> 1
            else -> 0
        }
    
    /**
     * Thêm unit vào bench
     */
    fun addToBench(unit: Unit): Player? {
        return if (hasBenchSpace) {
            copy(bench = bench + unit)
        } else null
    }
    
    /**
     * Bỏ unit khỏi bench
     */
    fun removeFromBench(unitId: String): Player {
        return copy(bench = bench.filter { it.id != unitId })
    }
    
    /**
     * Đặt unit lên board
     */
    fun deployToBoard(unitId: String, slot: BoardSlot): Player? {
        val unit = bench.find { it.id == unitId } ?: return null
        
        // Kiểm tra slot phải trống
        if (board[slot] != null) return null
        
        // Kiểm tra còn deploy cap không
        if (!canDeployMore) return null
        
        val updatedUnit = unit.withBoardPosition(slot)
        val newBoard = board.toMutableMap().apply { put(slot, updatedUnit) }
        val newBench = bench.filter { it.id != unitId }
        
        return copy(
            bench = newBench,
            board = newBoard
        )
    }
    
    /**
     * Thu hồi unit từ board về bench
     */
    fun recallFromBoard(slot: BoardSlot): Player? {
        val unit = board[slot] ?: return null
        
        if (!hasBenchSpace) return null
        
        val recalledUnit = unit.withBoardPosition(null)
        val newBoard = board.toMutableMap().apply { put(slot, null) }
        val newBench = bench + recalledUnit
        
        return copy(
            bench = newBench,
            board = newBoard
        )
    }
    
    /**
     * Swap unit giữa bench và board
     */
    fun swapBenchBoard(unitId: String, slot: BoardSlot): Player? {
        val benchUnit = bench.find { it.id == unitId } ?: return null
        
        val boardUnit = board[slot]
        
        val newBenchUnit = benchUnit.withBoardPosition(slot)
        val newBoard = board.toMutableMap().apply { put(slot, newBenchUnit) }
        val newBench = bench.filter { it.id != unitId }.let { currentBench ->
            if (boardUnit != null) {
                val recalledUnit = boardUnit.withBoardPosition(null)
                currentBench + recalledUnit
            } else {
                currentBench
            }
        }
        
        return copy(
            bench = newBench,
            board = newBoard
        )
    }
    
    /**
     * Cập nhật mana theo thời gian
     */
    fun updateMana(currentTimeMs: Long): Player {
        if (lastManaUpdateTime == 0L) {
            return copy(lastManaUpdateTime = currentTimeMs)
        }
        
        val deltaTimeSeconds = (currentTimeMs - lastManaUpdateTime) / 1000f
        val newMana = (currentMana + manaRegenRate * deltaTimeSeconds).coerceAtMost(maxMana)
        
        return copy(
            currentMana = newMana,
            lastManaUpdateTime = currentTimeMs
        )
    }
    
    /**
     * Sử dụng mana để bắn
     */
    fun useMana(cost: Float): Player? {
        return if (currentMana >= cost) {
            copy(currentMana = (currentMana - cost).coerceAtLeast(0f))
        } else null
    }
    
    /**
     * Hồi phục mana (khi level up hoặc item)
     */
    fun restoreMana(amount: Float): Player {
        return copy(currentMana = (currentMana + amount).coerceAtMost(maxMana))
    }
}
