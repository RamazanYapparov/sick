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
    null, "simple" -> Question.Type.Simple
    "stake" -> Question.Type.Stake
    "noRisk" -> Question.Type.NoRisk
    "secret", "secretPublicPrice" /* todo account this type as well */ -> Question.Type.Secret(
        exceptCurrent = firstParamByName("selectionMode").value == "exceptCurrent",
        pricingRule = firstParamByType("numberSet").numberSet.toPricingRule(),
        themeOverride = firstParamByName("theme").value
    )

    else -> throw UnsupportedOperationException("Question with type $type is unsupported")
}

fun XmlNumberSet.toPricingRule() = Question.Type.Secret.PricingRule(
    minimum = minimum, maximum = maximum, step = step
)

fun XmlQuestion.toAnswer() = if (findParamByName("answerType")?.value == "select") {
    Answer.Select(
        firstParamByName("answerOptions").param.map {
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
    )
}

fun XmlQuestion.toContent() = firstParamByName("question").item.map { it.toContent() }

fun Item.toContent() = if (type == null) {
    Content.Text(value)
} else {
    val mediaType = when (type) {
        "image" -> Content.Type.Image
        "audio" -> Content.Type.Audio
        "video" -> Content.Type.Video

        else -> throw UnsupportedOperationException("Media type $type is unsupported")
    }
    if (isRef == "True") {
        Content.Media.FileRef(mediaType, value)
    } else {
        Content.Media.FileUrl(mediaType, URL(value))
    }
}

private fun XmlQuestion.findParamByName(name: String) = params.param.find { it.name == name }
private fun XmlQuestion.firstParamByName(name: String) = params.param.first { it.name == name }
private fun XmlQuestion.firstParamByType(type: String) = params.param.first { it.type == type }
