package com.sick.siq.mapper

import com.sick.model.*
import io.github.oshai.kotlinlogging.KotlinLogging
import com.sick.siq.xml.model.Package as XmlPackage
import com.sick.siq.xml.model.Round as XmlRound
import com.sick.siq.xml.model.Theme as XmlTheme

private val logger = KotlinLogging.logger {}

/** Maps the pack, leaving out (and logging) every question the host cannot play; media are looked up in [files]. */
internal fun XmlPackage.toDomain(files: PackFiles) = Package(
    name = name,
    logo = "",
    tags = tags?.tag ?: emptyList(),
    author = info.authors.author.single(),
    rounds = rounds?.round?.map { it.toDomain(files) } ?: emptyList()
)

private fun XmlRound.toRoundType() = if(type == "final") RoundType.Final else RoundType.Simple

private fun XmlRound.toDomain(files: PackFiles) = Round(
    name = name,
    type = toRoundType(),
    themes = themes?.theme?.map { it.toDomain(round = name, files) } ?: emptyList()
)

private fun XmlTheme.toDomain(round: String, files: PackFiles) = Theme(
    name = name,
    questions = questions?.question?.mapNotNull { question ->
        try {
            question.toDomain(files)
        } catch (e: UnplayableQuestion) {
            logger.warn { "Skipped question: round \"$round\", theme \"$name\", price ${question.price}: ${e.message}" }
            null
        }
    } ?: emptyList(),
)
