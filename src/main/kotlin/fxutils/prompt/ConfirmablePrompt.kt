package fxutils.prompt

import fxutils.SubWindow
import fxutils.registerShortcuts
import fxutils.styleClass
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

abstract class ConfirmablePrompt<R : Any>(
    final override val title: String,
    cancelText: String = "_Cancel",
    confirmText: String = "_Ok"
) : Prompt<R?>() {
    val cancelButton = Button(cancelText) styleClass "sleek-button"
    val confirmButton = Button(confirmText) styleClass "sleek-button"

    init {
        cancelButton.setOnAction { commit(null) }
        confirmButton.setOnAction { commit(confirm()) }
    }

    override val windowType: SubWindow.Type
        get() = SubWindow.Type.Prompt

    override fun getDefault(): R? = null

    protected abstract fun confirm(): R?

    protected open fun extraButtons(): List<Button> = emptyList()

    protected open fun buttons(): List<Button> = listOf(cancelButton, confirmButton) + extraButtons()

    override fun createLayout(): Parent {
        val layout = super.createLayout() as VBox
        val buttonsBar = HBox() styleClass "buttons-bar"
        buttonsBar.children.addAll(buttons())
        layout.children.add(buttonsBar)
        layout.registerShortcuts {
            on("Ctrl+Enter") {
                commit(confirm())
            }
        }
        return layout
    }
}