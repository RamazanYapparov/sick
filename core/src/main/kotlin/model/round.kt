package com.sick.model

enum class RoundType {
    Simple,
    Final,
}

data class Round(
    val name: String,
    val type: RoundType,
    val themes: List<Theme>,
)

data class Theme(
    val name: String,
    val questions: List<Question>,
)
