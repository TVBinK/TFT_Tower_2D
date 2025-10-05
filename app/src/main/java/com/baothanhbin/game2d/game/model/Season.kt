package com.baothanhbin.game2d.game.model

enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    fun next() = values()[(ordinal + 1) % values().size];
    fun prev() = values()[(ordinal + values().size - 1) % values().size]
}