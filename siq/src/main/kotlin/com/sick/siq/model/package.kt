package com.sick.siq.model

data class Package(
    val name: String,
    val logo: String,
    val tags: List<String>,
    val author: String,
    val rounds: List<Round>,
)
