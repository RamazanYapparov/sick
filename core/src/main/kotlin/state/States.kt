package com.sick.state

sealed class GamePhase(val name: String) {
    data object Lobby : GamePhase("Lobby")
    data object ChoosingPlayer : GamePhase("ChoosingPlayer")
    data object ChoosingQuestion : GamePhase("ChoosingQuestion")
    data object ShowingQuestion : GamePhase("ShowingQuestion")
    data object PlayerAnswering : GamePhase("PlayerAnswering")
    data object RoundEnd : GamePhase("RoundEnd")
    data object GameOver : GamePhase("GameOver")
}
