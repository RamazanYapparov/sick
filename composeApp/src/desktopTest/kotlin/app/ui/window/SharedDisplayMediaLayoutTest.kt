package app.ui.window

import app.state.QuestionDisplayItem
import com.sick.state.GamePhase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedDisplayMediaLayoutTest {
    @Test
    fun `visual media uses fullscreen layout while it is visible`() {
        val image = QuestionDisplayItem.LocalImage("question.jpg")
        val video = QuestionDisplayItem.LocalVideo("question.mp4")

        assertTrue(shouldUseExpandedQuestionMedia(GamePhase.ShowingQuestion, listOf(image)))
        assertTrue(shouldUseExpandedQuestionMedia(GamePhase.PlayerAnswering, listOf(video)))
        assertTrue(shouldUseExpandedQuestionMedia(GamePhase.ShowingAnswer, listOf(image)))
    }

    @Test
    fun `non-visual and non-live content keeps standard display layout`() {
        val image = QuestionDisplayItem.LocalImage("question.jpg")
        val audio = QuestionDisplayItem.LocalAudio("question.mp3")

        assertFalse(shouldUseExpandedQuestionMedia(GamePhase.RevealingQuestion, listOf(image)))
        assertFalse(shouldUseExpandedQuestionMedia(GamePhase.ShowingQuestion, listOf(audio)))
        assertFalse(shouldUseExpandedQuestionMedia(GamePhase.ShowingQuestion, listOf(QuestionDisplayItem.Text("Text"))))
    }
}
