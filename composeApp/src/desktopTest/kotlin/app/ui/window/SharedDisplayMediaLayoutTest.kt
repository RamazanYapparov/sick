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

        assertTrue(shouldUseFullscreenQuestionMedia(GamePhase.ShowingQuestion, listOf(image)))
        assertTrue(shouldUseFullscreenQuestionMedia(GamePhase.PlayerAnswering, listOf(video)))
        assertTrue(shouldUseFullscreenQuestionMedia(GamePhase.ShowingAnswer, listOf(image)))
    }

    @Test
    fun `non-visual and non-live content keeps standard display layout`() {
        val image = QuestionDisplayItem.LocalImage("question.jpg")
        val audio = QuestionDisplayItem.LocalAudio("question.mp3")

        assertFalse(shouldUseFullscreenQuestionMedia(GamePhase.RevealingQuestion, listOf(image)))
        assertFalse(shouldUseFullscreenQuestionMedia(GamePhase.ShowingQuestion, listOf(audio)))
        assertFalse(shouldUseFullscreenQuestionMedia(GamePhase.ShowingQuestion, listOf(QuestionDisplayItem.Text("Text"))))
    }
}
