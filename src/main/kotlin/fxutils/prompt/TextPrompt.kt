package fxutils.prompt

import fxutils.styleClass
import javafx.application.Platform
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class TextPrompt<R : Any>(final override val title: String, initialText: String) : Prompt<R?>() {
    protected abstract suspend fun convert(text: String, ev: KeyEvent): R?

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
                    CoroutineScope(Dispatchers.Default).launch {
                        val value = convert(content.text, ev)
                        if (value != null) {
                            Platform.runLater {
                                commit(value)
                            }
                        }
                    }
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