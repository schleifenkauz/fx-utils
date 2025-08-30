package fxutils.prompt

import fxutils.button
import fxutils.registerShortcuts
import javafx.scene.control.Button

class InfoPrompt(private val text: String) : Prompt<Unit>() {
    private val okButton = button("Ok") { commit(Unit) }

    override val title: String
        get() = text

    override val content: Button get() = okButton

    init {
        content.registerShortcuts {
            on("ENTER") {

            }
        }
    }

    override fun onReceiveFocus() {
        okButton.requestFocus()
    }

    override fun getDefault() = Unit
}