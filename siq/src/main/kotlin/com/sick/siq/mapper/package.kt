package com.sick.com.sick.siq.mapper

import com.sick.model.*
import com.sick.siq.xml.model.Package as XmlPackage
import com.sick.siq.xml.model.Round as XmlRound
import com.sick.siq.xml.model.Theme as XmlTheme

fun XmlPackage.toDomain() = Package(
    name = name,
    logo = "",
    tags = tags.tag,
    author = info.authors.author.single(),
    rounds = rounds.round.map { it.toDomain() }
)

fun XmlRound.toRoundType() = if(type == "final") RoundType.Final else RoundType.Simple

fun XmlRound.toDomain() = Round(
    name = name,
    type = toRoundType(),
    themes = themes.theme.map { it.toDomain() }
)

fun XmlTheme.toDomain() = Theme(
    name = name,
    questions = questions.question.map { it.toDomain() },
)




