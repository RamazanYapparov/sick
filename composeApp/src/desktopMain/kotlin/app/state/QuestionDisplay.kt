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
    data class LocalAudio(val absolutePath: String) : QuestionDisplayItem
    data class RemoteAudio(val url: URL) : QuestionDisplayItem
}

fun Question<*>.displayContents(basePath: Path?): List<QuestionDisplayItem> =
    contents
        .sortedWith { item, _ -> if (item is Content.Text) -1 else 1 }
        .map { content ->
        when (content) {
            is Content.Text -> QuestionDisplayItem.Text(content.text)
            is Content.Media.FileRef -> when (content.type) {
                Content.Type.Image -> QuestionDisplayItem.LocalImage(
                    basePath?.resolve("Images")?.resolve(content.ref)?.toString() ?: content.ref
                )
                Content.Type.Video -> {
                    val resolved = basePath?.resolve("Video")?.resolve(content.ref)
                    if (resolved != null && resolved.toFile().exists()) {
                        QuestionDisplayItem.LocalVideo(resolved.toFile().absolutePath)
                    } else if (resolved != null) {
                        QuestionDisplayItem.Text("Video not found: ${content.ref}")
                    } else {
                        QuestionDisplayItem.LocalVideo(content.ref)
                    }
                }
                Content.Type.Audio -> {
                    val resolved = basePath?.resolve("Audio")?.resolve(content.ref)
                    if (resolved != null && resolved.toFile().exists()) {
                        QuestionDisplayItem.LocalAudio(resolved.toFile().absolutePath)
                    } else if (resolved != null) {
                        QuestionDisplayItem.Text("Audio not found: ${content.ref}")
                    } else {
                        QuestionDisplayItem.LocalAudio(content.ref)
                    }
                }
            }
            is Content.Media.FileUrl -> when (content.type) {
                Content.Type.Image -> QuestionDisplayItem.RemoteImage(content.url)
                Content.Type.Video -> QuestionDisplayItem.RemoteVideo(content.url)
                Content.Type.Audio -> QuestionDisplayItem.RemoteAudio(content.url)
            }
        }
    }
