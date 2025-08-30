package fxutils.prompt

import fxutils.button
import fxutils.registerShortcuts
import fxutils.styleClass
import javafx.scene.layout.HBox

class YesNoPrompt(
    private val question: String,
    private val cancellable: Boolean = false,
    private val default: Boolean? = if (cancellable) null else false
) : Prompt<Boolean?>() {
    private val btnCancel = button("Cancel") { commit(null) }
    private val btnNo = button("No") { commit(false) }
    private val btnYes = button("Yes") { commit(true) }
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