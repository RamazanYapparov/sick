package com.sick.model

data class Game(
    val pack: Package,
    val players: List<Player> = emptyList(),
    val playedQuestions: Set<Question> = emptySet(),
)

data class Settings(
    val timings: Any
)