package com.sick.engine

import com.sick.event.GameEvent
import com.sick.model.GameState
import com.sick.state.GamePhase

/** What one successful [GameEngine.process] call did: the [event] it applied and the state and phase on both sides. */
data class EngineTransition(
    val event: GameEvent,
    val previousState: GameState,
    val state: GameState,
    val previousPhase: GamePhase,
    val phase: GamePhase,
)
