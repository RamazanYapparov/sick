package app.state

import com.sick.model.Content
import com.sick.model.Question
import java.net.URL
import java.nio.file.Path

sealed interface QuestionDisplayItem {
    data class Text(val text: String) : QuestionDisplayItem
    data class LocalImage(val absolutePath: String) : QuestionDisplayItem
    data class RemoteImage(val url: URL) : QuestionDisplayItem
    data class LocalVideo(val absolutePath: String) : QuestionDisplayItem
    data class RemoteVideo(val url: URL) : QuestionDisplayItem
}

fun Question<*>.displayContents(basePath: Path?): List<QuestionDisplayItem> =
    contents.map { content ->
        when (content) {
            is Content.Text -> QuestionDisplayItem.Text(content.text)
            is Content.Media.FileRef -> when (content.type) {
                Content.Type.Image -> QuestionDisplayItem.LocalImage(
                    basePath?.resolve("Images")?.resolve(content.ref)?.toString() ?: content.ref
                )
                Content.Type.Video -> QuestionDisplayItem.LocalVideo(
                    basePath?.resolve("Video")?.resolve(content.ref)?.toString() ?: content.ref
                )
                else -> TODO("Unsupported media type: ${content.type}")
            }
            is Content.Media.FileUrl -> when (content.type) {
                Content.Type.Image -> QuestionDisplayItem.RemoteImage(content.url)
                Content.Type.Video -> QuestionDisplayItem.RemoteVideo(content.url)
                else -> TODO("Unsupported media type: ${content.type}")
            }
        }
    }
