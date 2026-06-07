package com.sick.com.sick.siq.mapper

import com.sick.model.*
import com.sick.siq.xml.model.Item
import java.net.URL
import com.sick.siq.xml.model.NumberSet as XmlNumberSet
import com.sick.siq.xml.model.Question as XmlQuestion

fun XmlQuestion.toDomain() = Question(
    price = price,
    type = toQuestionType(),
    contents = toContent(),
    answer = toAnswer(),
)

fun XmlQuestion.toQuestionType(): Question.Type = when (type) {
    null, "", "simple", "forAll" -> Question.Type.Simple
    "stake" -> Question.Type.Stake
    "noRisk" -> Question.Type.NoRisk
    "secret", "secretPublicPrice" /* todo account this type as well */ ->
        Question.Type.Secret(
            exceptCurrent = (findParamByName("selectionMode")?.value ?: "exceptCurrent") == "exceptCurrent",
            pricingRule = findParamByType("numberSet")?.numberSet?.toPricingRule(),
            themeOverride = findParamByName("theme")?.value
        )

    else -> throw UnsupportedOperationException("Question with type $type is unsupported")
}

fun XmlNumberSet.toPricingRule() = Question.Type.Secret.PricingRule(
    minimum = minimum, maximum = maximum, step = step
)

fun XmlQuestion.toAnswer() = if (findParamByName("answerType")?.value == "select") {
    Answer.Select(
        firstParamByName("answerOptions")?.param.orEmpty().map {
            Answer.Select.Option(
                name = it.name,
                answer = it.item.single().value,
                correct = right.answer.any { a -> a == it.name || a == it.value }
            )
        }
    )
} else {
    Answer.Simple(
        right = right.answer,
        wrong = wrong?.answer.orEmpty(),
        contents = findParamByName("answer")?.item.orEmpty().mapNotNull { it.toContentOrNull() },
    )
}

fun Item.toContentOrNull(): Content? = if (type == null || type == "say") {
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
    if (isRef == "True" || rawValue.startsWith("@")) {
        Content.Media.FileRef(mediaType, rawValue.removePrefix("@"))
    } else {
        Content.Media.FileUrl(mediaType, URL(rawValue))
    }
}

fun XmlQuestion.toContent(): List<Content> {
    val items = firstParamByName("question")?.item ?: scenario?.atom.orEmpty()
    return items.mapNotNull { it.toContentOrNull() }
}

private fun XmlQuestion.findParamByName(name: String) = params?.param.orEmpty().find { it.name == name }
private fun XmlQuestion.firstParamByName(name: String) = params?.param.orEmpty().firstOrNull { it.name == name }
private fun XmlQuestion.findParamByType(type: String) = params?.param.orEmpty().find { it.type == type }
private fun XmlQuestion.firstParamByType(type: String) = params?.param.orEmpty().firstOrNull { it.type == type }
