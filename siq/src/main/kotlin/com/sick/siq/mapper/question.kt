package com.sick.siq.mapper

import com.sick.model.*
import com.sick.siq.xml.model.Item
import java.net.URL
import com.sick.siq.xml.model.NumberSet as XmlNumberSet
import com.sick.siq.xml.model.Question as XmlQuestion

/** A question the host cannot play; the mapper leaves it out of the pack. */
internal class UnplayableQuestion(reason: String) : Exception(reason)

private fun unplayable(reason: String): Nothing = throw UnplayableQuestion(reason)

/** Throws [UnplayableQuestion] when the question has an unknown type, a media file not in the pack, a malformed URL, or a Point Answer without an image. */
internal fun XmlQuestion.toDomain(files: PackFiles): Question<*> {
    val contents = toContent(files)
    return Question(
        price = price,
        type = toQuestionType(),
        contents = contents,
        answer = toAnswer(contents, files),
    )
}

private fun XmlQuestion.toQuestionType(): Question.Type = when (type) {
    null, "", "simple", "forAll" -> Question.Type.Simple
    "stake", "stakeAll" -> Question.Type.Stake
    "noRisk" -> Question.Type.NoRisk
    "secret", "secretPublicPrice" /* todo account this type as well */ ->
        Question.Type.Secret(
            exceptCurrent = (findParamByName("selectionMode")?.value ?: "exceptCurrent") == "exceptCurrent",
            pricingRule = findParamByType("numberSet")?.numberSet?.toPricingRule(),
            themeOverride = findParamByName("theme")?.value
        )

    else -> unplayable("unsupported question type \"$type\"")
}

private fun XmlNumberSet.toPricingRule() = Question.Type.Secret.PricingRule(
    minimum = minimum, maximum = maximum, step = step
)

private fun XmlQuestion.toAnswer(contents: List<Content>, files: PackFiles): Answer = when (findParamByName("answerType")?.value) {
    "select" -> Answer.Select(
        firstParamByName("answerOptions")?.param.orEmpty().map {
            Answer.Select.Option(
                name = it.name,
                answer = it.item.single().value,
                correct = right.answer.any { a -> a == it.name || a == it.value }
            )
        }
    )
    "point" -> toPointAnswer(contents) ?: toSimpleAnswer(files)
    else -> toSimpleAnswer(files)
}

private fun XmlQuestion.toSimpleAnswer(files: PackFiles) = Answer.Simple(
    right = right.answer,
    wrong = wrong?.answer.orEmpty(),
    contents = findParamByName("answer")?.item.orEmpty().mapNotNull { it.toContentOrNull(files) },
)

/** Parses `x,y[,aspectRatio]` as written by SIQuester on the first image in [contents]; null when the answer is malformed. */
private fun XmlQuestion.toPointAnswer(contents: List<Content>): Answer.Point? {
    val raw = right.answer.firstOrNull() ?: return null
    val parts = raw.split(',').map { it.trim().toDoubleOrNull() ?: return null }
    if (parts.size !in 2..3) return null
    val image = contents.firstOrNull { it is Content.Media && it.type == Content.Type.Image } as Content.Media?
        ?: unplayable("point answer without an image")
    return Answer.Point(
        image = image,
        x = parts[0],
        y = parts[1],
        aspectRatio = parts.getOrNull(2) ?: 1.0,
        deviation = findParamByName("answerDeviation")?.value?.trim()?.toDoubleOrNull() ?: 0.0,
    )
}

private fun Item.toContentOrNull(files: PackFiles): Content? = if (type == null || type == "say") {
    Content.Text(value ?: "")
} else {
    val rawValue = value ?: ""
    val mediaType = when (type) {
        "image" -> Content.Type.Image
        "audio" -> Content.Type.Audio
        "video" -> Content.Type.Video
        "voice" -> Content.Type.Audio
        "marker" -> return null

        else -> return if (rawValue.isBlank()) null else Content.Text(rawValue)
    }
    val source = if (isRef == "True" || rawValue.startsWith("@")) {
        val ref = rawValue.removePrefix("@")
        val file = files.find(mediaType, ref) ?: unplayable("no file \"$ref\" in ${PackFiles.folderOf(mediaType)}")
        Content.Media.Source.Local(file)
    } else {
        val url = runCatching { URL(rawValue) }.getOrNull() ?: unplayable("malformed URL \"$rawValue\"")
        Content.Media.Source.Remote(url)
    }
    Content.Media(mediaType, source)
}

private fun XmlQuestion.toContent(files: PackFiles): List<Content> {
    val items = firstParamByName("question")?.item ?: scenario?.atom.orEmpty()
    return items.mapNotNull { it.toContentOrNull(files) }
}

private fun XmlQuestion.findParamByName(name: String) = params?.param.orEmpty().find { it.name == name }
private fun XmlQuestion.firstParamByName(name: String) = params?.param.orEmpty().firstOrNull { it.name == name }
private fun XmlQuestion.findParamByType(type: String) = params?.param.orEmpty().find { it.type == type }
