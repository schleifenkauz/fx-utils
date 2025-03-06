package fxutils.prompt

import fxutils.registerShortcuts
import javafx.scene.layout.HBox
import fxutils.button
import fxutils.prompt.Prompt
import fxutils.styleClass

class YesNoPrompt(
    private val question: String,
    private val cancellable: Boolean = false,
    private val default: Boolean? = if (cancellable) null else false
) : Prompt<Boolean?, HBox>() {
    private val btnCancel = button("Cancel") { commit(null) } styleClass "sleek-button"
    private val btnNo = button("No") { commit(false) } styleClass "sleek-button"
    private val btnYes = button("Yes") { commit(true) } styleClass "sleek-button"
    override val content = HBox(btnNo, btnYes) styleClass "buttons-bar"
    override val title: String
        get() = question

    init {
        if (cancellable) content.children.add(0, btnCancel)
        content.registerShortcuts {
            on("Y") { commit(true) }
            on("N") { commit(false) }
        }
    }

    override fun onReceiveFocus() {
        when (default) {
            true -> btnYes.requestFocus()
            false -> btnNo.requestFocus()
            else -> btnCancel.requestFocus()
        }
    }

    override fun getDefault(): Boolean? = default
}