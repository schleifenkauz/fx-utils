package fxutils.prompt

import fxutils.registerShortcuts
import javafx.scene.input.KeyEvent

class PredicateTextPrompt(
    title: String, initialText: String, private val check: (String) -> Boolean
) : TextPrompt<String>(title, initialText) {
    override fun convert(text: String): String? = text.takeIf(check)
}

class SimpleTextPrompt(title: String, initialText: String) : TextPrompt<String>(title, initialText) {
    override fun convert(text: String): String = text
}

class IntegerPrompt(
    title: String, initialValue: Int?,
    private val range: IntRange = Int.MIN_VALUE..Int.MAX_VALUE
) : TextPrompt<Int>(title, initialValue?.toString().orEmpty()) {
    init {
        content.registerShortcuts(KeyEvent.KEY_PRESSED) {
            on("DOWN") {
                content.text.toIntOrNull()?.let { v -> if (v - 1 in range) content.text = (v - 1).toString() }
            }
            on("UP") { content.text.toIntOrNull()?.let { v -> if (v + 1 in range) content.text = (v + 1).toString() } }
        }
    }

    override fun convert(text: String): Int? = text.toIntOrNull()?.takeIf { v -> v in range }
}

