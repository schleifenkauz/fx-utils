package fxutils.prompt

import fxutils.styleClass
import javafx.scene.control.TextField

abstract class TextPrompt<R : Any>(final override val title: String, initialText: String) : Prompt<R?>() {
    protected abstract fun convert(text: String): R?

    final override val content: TextField = TextField(initialText).styleClass("prompt", "prompt-text-field")

    override fun getDefault(): R? = null

    override fun onReceiveFocus() {
        content.requestFocus()
        content.selectAll()
    }

    init {
        content.setOnAction { ev ->
            val value = convert(content.text)
            if (value != null) commit(value)
            ev.consume()
        }
    }
}