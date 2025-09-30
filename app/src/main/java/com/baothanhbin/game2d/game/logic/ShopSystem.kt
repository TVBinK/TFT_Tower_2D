package com.baothanhbin.game2d.game.logic

import com.baothanhbin.game2d.game.model.*

/**
 * Hệ thống shop - quản lý reroll, odds, mua bán
 */
class ShopSystem {
    
    /**
     * Roll shop mới theo level của player
     */
    fun rollShop(shop: Shop, playerLevel: Int): Shop {
        return shop.reroll(playerLevel)
    }
    
    /**
     * Mua unit từ slot
     */
    fun buyFromShop(
        shop: Shop, 
        slotIndex: Int, 
        player: Player
    ): Triple<Shop?, Player?, com.baothanhbin.game2d.game.model.Unit?> {
        
        if (!shop.canBuy(slotIndex, player.gold)) {
            return Triple(null, null, null)
        }
        
        if (!player.hasBenchSpace) {
            return Triple(null, null, null)
        }
        
        val (newShop, unit) = shop.buyFromSlot(slotIndex)
        unit ?: return Triple(null, null, null)
        
        val newPlayer = player
            .copy(gold = player.gold - unit.tier.cost)
            .addToBench(unit)
        
        return Triple(newShop, newPlayer, unit)
    }
    
    /**
     * Bán unit từ bench
     */
    fun sellUnit(player: Player, unitId: String): Player? {
        val unit = player.bench.find { it.id == unitId } ?: return null
        
        return player
            .removeFromBench(unitId)
            .copy(gold = player.gold + unit.sellPrice)
    }
    
    /**
     * Bán unit từ board
     */
    fun sellUnitFromBoard(player: Player, slot: BoardSlot): Player? {
        val unit = player.board[slot] ?: return null
        
        val newBoard = player.board.toMutableMap().apply { put(slot, null) }
        
        return player.copy(
            board = newBoard,
            gold = player.gold + unit.sellPrice
        )
    }
    
    /**
     * Kiểm tra có thể reroll không
     */
    fun canReroll(player: Player): Boolean {
        return player.canReroll
    }
    
    /**
     * Tính giá reroll
     */
    fun getRerollCost(player: Player): Int {
        return if (player.freeRerollsRemaining > 0) 0 else Player.REROLL_COST
    }
    
    /**
     * Refresh free reroll ở đầu round
     */
    fun refreshFreeReroll(player: Player): Player {
        return player.copy(freeRerollsRemaining = 1)
    }
}
