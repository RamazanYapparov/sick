package app.state

import com.sick.model.Answer

fun Answer.hostSummary(): List<String> = when (this) {
    is Answer.Simple -> buildList {
        if (right.isNotEmpty()) add("Right: ${right.joinToString(", ")}")
        if (wrong.isNotEmpty()) add("Wrong: ${wrong.joinToString(", ")}")
        if (contents.isNotEmpty()) add("Answer media: ${contents.size} item(s)")
    }
    is Answer.Select -> options.map { option ->
        "${if (option.correct) "[x]" else "[ ]"} ${option.name}: ${option.answer}"
    }
}
