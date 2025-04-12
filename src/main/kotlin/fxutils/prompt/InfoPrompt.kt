package fxutils.prompt

import fxutils.button
import fxutils.registerShortcuts
import fxutils.styleClass
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox

class InfoPrompt(private val text: String) : Prompt<Unit, Button>() {
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

    override fun createLayout(): Parent = HBox(
        Label(title) styleClass "dialog-title",
        content
    ) styleClass "dialog-box"

    override fun getDefault() = Unit
}