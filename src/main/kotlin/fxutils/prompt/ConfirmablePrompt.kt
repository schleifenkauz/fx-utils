package fxutils.prompt

import fxutils.SubWindow
import fxutils.button
import fxutils.shortcut
import fxutils.styleClass
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

abstract class ConfirmablePrompt<R : Any, N : Node>(final override val title: String) : Prompt<R?, N>() {
    val cancelButton = button("Cancel") { commit(null) }
    val confirmButton = button("Confirm") { commit(confirm()) }

    override val windowType: SubWindow.Type
        get() = SubWindow.Type.Prompt

    override fun getDefault(): R? = null

    protected abstract fun confirm(): R?

    protected open fun extraButtons(): List<Button> = emptyList()

    override fun createLayout(): Parent {
        val layout = super.createLayout() as VBox
        val buttons = HBox(cancelButton, confirmButton) styleClass "buttons-bar"
        buttons.children.addAll(extraButtons())
        layout.children.add(buttons)
        layout.addEventFilter(KeyEvent.KEY_TYPED) { ev ->
            if ("Enter".shortcut.matches(ev)) {
                if (ev.target == layout) commit(confirm())
            }
        }
        return layout
    }
}