package fxutils.prompt

import fxutils.button
import fxutils.styleClass
import javafx.scene.control.Button
import javafx.scene.layout.HBox

class OptionsPrompt(
    override val title: String,
    private val options: List<String>,
    private val defaultOption: String,
) : Prompt<String>() {
    private var defaultButton: Button? = null

    override val content: HBox = HBox() styleClass "buttons-bar"

    init {
        for (option in options) {
            val btn = button(option) { commit(option) }
            content.children.add(btn)
            if (option == defaultOption) {
                defaultButton = btn
            }
        }
    }

    override fun getDefault(): String = defaultOption

    override fun onReceiveFocus() {
        defaultButton?.requestFocus()
    }
}