package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*

/**
 * Hệ thống merge - tự động gộp 3 unit cùng loại thành 1 unit sao cao hơn
 */
class MergeSystem {
    
    /**
     * Thử auto-merge toàn bộ collection của player
     */
    fun tryAutoMerge(player: Player): Player {
        var currentPlayer = player
        var hasChanged = true
        
        // Lặp cho đến khi không còn merge nào có thể thực hiện
        while (hasChanged) {
            val mergeResult = performMerge(currentPlayer)
            hasChanged = mergeResult != currentPlayer
            currentPlayer = mergeResult
        }
        
        return currentPlayer
    }
    
    /**
     * Thực hiện một lượt merge
     */
    private fun performMerge(player: Player): Player {
        // Lấy tất cả units từ bench và board
        val allUnits = getAllUnitsFromPlayer(player)
        
        // Group theo (type, star)
        val groupedUnits = allUnits.groupBy { unit: com.baothanhbin.game2d.game.model.Unit -> 
            Pair(unit.type, unit.star) 
        }
        
        // Tìm group có thể merge (≥3 units cùng loại và star < 3)
        for ((key, units) in groupedUnits) {
            val (type, star) = key
            
            if (units.size >= 3 && star != Star.THREE) {
                // Thực hiện merge
                return executeMerge(player, units.take(3), getNextStar(star))
            }
        }
        
        return player
    }
    
    /**
     * Lấy tất cả units của player (bench + board)
     */
    private fun getAllUnitsFromPlayer(player: Player): List<com.baothanhbin.game2d.game.model.Unit> {
        val benchUnits = player.bench
        val boardUnits = player.board.values.filterNotNull()
        return benchUnits + boardUnits
    }
    
    /**
     * Thực hiện merge 3 units thành 1 unit sao cao hơn
     */
    private fun executeMerge(
        player: Player, 
        unitsToMerge: List<com.baothanhbin.game2d.game.model.Unit>, 
        newStar: Star
    ): Player {
        if (unitsToMerge.size < 3) return player
        
        val templateUnit = unitsToMerge.first()
        val mergedUnit = templateUnit.withStar(newStar)
        
        // Tìm vị trí tốt nhất để đặt unit merge
        val (newPlayer, finalUnit) = findBestPositionForMergedUnit(
            player, 
            mergedUnit, 
            unitsToMerge
        )
        
        // Xóa 3 units cũ và thêm unit mới
        return removeMergedUnitsAndAddNew(newPlayer, unitsToMerge, finalUnit)
    }
    
    /**
     * Tìm vị trí tốt nhất cho unit sau merge
     */
    private fun findBestPositionForMergedUnit(
        player: Player,
        mergedUnit: com.baothanhbin.game2d.game.model.Unit,
        unitsToMerge: List<com.baothanhbin.game2d.game.model.Unit>
    ): Pair<Player, com.baothanhbin.game2d.game.model.Unit> {
        
        // Ưu tiên: nếu có unit trên board trong list merge → đặt lên board cùng vị trí
        val unitOnBoard = unitsToMerge.firstOrNull { it.isOnBoard }
        
        if (unitOnBoard != null) {
            // Đặt merged unit lên board cùng vị trí
            val mergedUnitOnBoard = mergedUnit.withBoardPosition(unitOnBoard.boardPosition)
            return Pair(player, mergedUnitOnBoard)
        }
        
        // Nếu có slot trống và có thể deploy → đặt lên board
        val availableSlot = BoardSlot.values().find { slot ->
            slot.position < player.deployCap && player.board[slot] == null
        }
        
        if (availableSlot != null && player.canDeployMore) {
            val unitOnBoard = mergedUnit.withBoardPosition(availableSlot)
            return Pair(player, unitOnBoard)
        }
        
        // Ngược lại đặt vào bench
        return Pair(player, mergedUnit)
    }
    
    /**
     * Xóa units cũ và thêm unit mới
     */
    private fun removeMergedUnitsAndAddNew(
        player: Player,
        unitsToMerge: List<com.baothanhbin.game2d.game.model.Unit>,
        newUnit: com.baothanhbin.game2d.game.model.Unit
    ): Player {
        
        val mergedIds = unitsToMerge.map { it.id }.toSet()
        
        // Xóa từ bench
        val newBench = player.bench.filter { it.id !in mergedIds }
        
        // Xóa từ board
        val newBoard = player.board.toMutableMap()
        for ((slot, unit) in player.board) {
            if (unit != null && unit.id in mergedIds) {
                newBoard[slot] = null
            }
        }
        
        // Thêm unit mới
        val updatedPlayer = player.copy(bench = newBench, board = newBoard)
        
        return if (newUnit.isOnBoard && newUnit.boardPosition != null) {
            // Đặt lên board
            updatedPlayer.copy(
                board = newBoard.apply { put(newUnit.boardPosition, newUnit) }
            )
        } else {
            // Thêm vào bench
            updatedPlayer.copy(bench = newBench + newUnit)
        }
    }
    
    /**
     * Lấy sao tiếp theo
     */
    private fun getNextStar(currentStar: Star): Star {
        return when (currentStar) {
            Star.ONE -> Star.TWO
            Star.TWO -> Star.THREE
            Star.THREE -> Star.THREE // Không thể merge thêm
        }
    }
    
    /**
     * Kiểm tra có thể merge units không
     */
    fun canMergeUnits(units: List<com.baothanhbin.game2d.game.model.Unit>): Boolean {
        if (units.size < 3) return false
        
        val first = units.first()
        return units.all { unit: com.baothanhbin.game2d.game.model.Unit -> 
            unit.type == first.type && 
            unit.star == first.star &&
            unit.star != Star.THREE
        }
    }
    
    /**
     * Tìm tất cả các group có thể merge
     */
    fun findMergeableGroups(player: Player): List<List<com.baothanhbin.game2d.game.model.Unit>> {
        val allUnits = getAllUnitsFromPlayer(player)
        
        val groupedUnits = allUnits.groupBy { unit: com.baothanhbin.game2d.game.model.Unit -> 
            Pair(unit.type, unit.star) 
        }
        
        return groupedUnits.values.filter { units: List<com.baothanhbin.game2d.game.model.Unit> ->
            units.size >= 3 && units.first().star != Star.THREE
        }
    }
    
}
