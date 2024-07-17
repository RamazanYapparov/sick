package com.sick.state

import com.sick.model.Game
import com.sick.model.Package
import com.sick.model.Player
import com.sick.model.Question
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.state.activeStates
import ru.nsk.kstatemachine.state.dataState
import ru.nsk.kstatemachine.statemachine.createStdLibStateMachine

data class StateI(val game: Game)

class StateHolder(var state: StateI? = null)

sealed class State(val game: Game) : DefaultState() {


    class ChoosePlayerToStart(game: Game) : State(game)
    class ChooseQuestion(game: Game, val activePlayer: Player) : State(game)
    class PlayingQuestion<T : Question.Type>(
        game: Game,
        val activePlayer: Player,
        val question: Question<T>
    ) : State(game)

    class AnsweringQuestion<T : Question.Type>(
        game: Game,
        val activePlayer: Player,
        val question: Question<T>
    ) : State(game)
}


val machine = createStdLibStateMachine {

}

fun run() {
    machine.activeStates()
    val pkg: Package = TODO()
    State.ChoosePlayerToStart(Game(pkg)).addListener()
}
