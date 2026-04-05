package fxutils.prompt

import fxutils.styleClass
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

abstract class TextPrompt<R : Any>(final override val title: String, initialText: String) : Prompt<R?>() {
    protected abstract fun convert(text: String): R?

    final override val content: TextField = TextField(initialText).styleClass("prompt", "prompt-text-field")

    override fun getDefault(): R? = null

    override fun onReceiveFocus() {
        content.requestFocus()
        content.selectAll()
    }

    init {
        content.addEventHandler(KeyEvent.KEY_RELEASED) { ev ->
            when (ev.code) {
                KeyCode.ENTER -> {
                    val value = convert(content.text)
                    if (value != null) commit(value)
                }

                KeyCode.ESCAPE -> {
                    window.hide()
                }

                else -> {}
            }
            ev.consume()
        }
    }
}