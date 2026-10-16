package app.state

import com.sick.model.Content
import com.sick.model.Content.Media.Source
import java.net.URL

sealed interface QuestionDisplayItem {
    data class Text(val text: String) : QuestionDisplayItem
    data class LocalImage(val absolutePath: String) : QuestionDisplayItem
    data class RemoteImage(val url: URL) : QuestionDisplayItem
    data class LocalVideo(val absolutePath: String) : QuestionDisplayItem
    data class RemoteVideo(val url: URL) : QuestionDisplayItem
    data class LocalAudio(val absolutePath: String) : QuestionDisplayItem
    data class RemoteAudio(val url: URL) : QuestionDisplayItem
}

/** Text first, then media; each group keeps pack order. */
fun displayContents(contents: List<Content>): List<QuestionDisplayItem> =
    contents
        .sortedBy { it !is Content.Text }
        .map { it.toDisplayItem() }

fun Content.toDisplayItem(): QuestionDisplayItem = when (this) {
    is Content.Text -> QuestionDisplayItem.Text(text)
    is Content.Media -> when (val source = source) {
        is Source.Local -> source.path.toString().let { path ->
            when (type) {
                Content.Type.Image -> QuestionDisplayItem.LocalImage(path)
                Content.Type.Video -> QuestionDisplayItem.LocalVideo(path)
                Content.Type.Audio -> QuestionDisplayItem.LocalAudio(path)
            }
        }
        is Source.Remote -> when (type) {
            Content.Type.Image -> QuestionDisplayItem.RemoteImage(source.url)
            Content.Type.Video -> QuestionDisplayItem.RemoteVideo(source.url)
            Content.Type.Audio -> QuestionDisplayItem.RemoteAudio(source.url)
        }
    }
}

fun QuestionDisplayItem.isImage(): Boolean =
    this is QuestionDisplayItem.LocalImage || this is QuestionDisplayItem.RemoteImage

fun QuestionDisplayItem.isPlayable(): Boolean = when (this) {
    is QuestionDisplayItem.LocalVideo, is QuestionDisplayItem.RemoteVideo,
    is QuestionDisplayItem.LocalAudio, is QuestionDisplayItem.RemoteAudio -> true
    is QuestionDisplayItem.Text, is QuestionDisplayItem.LocalImage, is QuestionDisplayItem.RemoteImage -> false
}
