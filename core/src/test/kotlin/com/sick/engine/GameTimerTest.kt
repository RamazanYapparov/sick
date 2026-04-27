package com.sick.engine

import com.sick.event.*
import com.sick.state.GamePhase
import com.sick.test.QUESTION_IDS
import com.sick.test.minimalPackage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GameTimerTest {

    private fun engineAtShowingQuestion(): GameEngine {
        val engine = GameEngine(minimalPackage(questionsPerTheme = 2))
        engine.process(PlayerJoined("Alice"))
        val alice = engine.state.players.first()
        engine.process(StartGame)
        engine.process(SelectActivePlayer(alice.id))
        engine.process(QuestionSelected(QUESTION_IDS[0][0]))
        return engine
    }

    @Test
    fun `start fires ticks and TimerExpired after elapsed time`() = runTest {
        val engine = engineAtShowingQuestion()
        val timer = GameTimer(engine, this)
        val initialRemaining = engine.state.timerRemaining  // 30

        timer.start(3)
        // advanceTimeBy uses exclusive upper bound: tasks at exactly t=N*1000 need +1ms
        advanceTimeBy(3_001L)

        assertEquals(initialRemaining - 3, engine.state.timerRemaining)
        assertEquals(GamePhase.ChoosingQuestion, engine.phase)
    }

    @Test
    fun `stop cancels the timer before TimerExpired fires`() = runTest {
        val engine = engineAtShowingQuestion()
        val timer = GameTimer(engine, this)
        val initialRemaining = engine.state.timerRemaining

        timer.start(5)
        advanceTimeBy(2_001L)  // 2 ticks fire (at t=1000 and t=2000, both < 2001)
        timer.stop()
        advanceTimeBy(3_000L)  // remaining ticks would fire but job is cancelled

        assertEquals(initialRemaining - 2, engine.state.timerRemaining)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)  // no TimerExpired
    }

    @Test
    fun `start twice cancels the first job`() = runTest {
        val engine = engineAtShowingQuestion()
        val timer = GameTimer(engine, this)

        timer.start(3)
        advanceTimeBy(501L)    // no ticks yet (first delay fires at t=1000)
        timer.start(3)          // cancels job1 before it fires anything, starts job2
        advanceTimeBy(3_001L)  // job2: 3 ticks + TimerExpired

        // With cancellation: 3 ticks total (job2 only) → timerRemaining = initialRemaining - 3
        // Without cancellation: 3 (job1) + 3 (job2) ticks → timerRemaining = initialRemaining - 6
        assertEquals(engine.state.timerSeconds - 3, engine.state.timerRemaining)
        assertEquals(GamePhase.ChoosingQuestion, engine.phase)
    }

    @Test
    fun `stop before start does not throw`() = runTest {
        val engine = engineAtShowingQuestion()
        val timer = GameTimer(engine, this)

        timer.stop()  // no-op: job is null

        assertEquals(GamePhase.ShowingQuestion, engine.phase)
    }

    @Test
    fun `timer drives full phase transition from ShowingQuestion to ChoosingQuestion`() = runTest {
        val engine = engineAtShowingQuestion()
        val timer = GameTimer(engine, this)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)

        timer.start(1)
        advanceTimeBy(1_001L)  // task at t=1000 needs +1ms to be included

        assertEquals(GamePhase.ChoosingQuestion, engine.phase)
    }

    @Test
    fun `paused timer does not advance elapsed question state`() = runTest {
        val engine = engineAtShowingQuestion()
        val timer = GameTimer(engine, this)
        engine.process(PauseTimer)
        val before = engine.state.timerRemaining

        timer.start(1)
        advanceTimeBy(1_001L)

        assertEquals(before, engine.state.timerRemaining)
        assertEquals(GamePhase.ShowingQuestion, engine.phase)
        assertTrue(engine.state.isTimerPaused)
    }
}
