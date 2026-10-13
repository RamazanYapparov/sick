package app.state

import com.sick.model.Answer
import java.util.Locale

fun Answer.hostSummary(): List<String> = when (this) {
    is Answer.Simple -> buildList {
        if (right.isNotEmpty()) add("Right: ${right.joinToString(", ")}")
        if (wrong.isNotEmpty()) add("Wrong: ${wrong.joinToString(", ")}")
        if (contents.isNotEmpty()) add("Answer media: ${contents.size} item(s)")
    }
    is Answer.Select -> options.map { option ->
        "${if (option.correct) "[x]" else "[ ]"} ${option.name}: ${option.answer}"
    }
    is Answer.Point -> listOf(
        "Point on the question image: ${coordinatesLabel()}",
        "Marked in red on the question image (shared screen shows it with the answer).",
    )
}

fun Answer.Point.coordinatesLabel(): String =
    "x=%.2f, y=%.2f, radius %.2f".format(Locale.ROOT, x, y, deviation)
