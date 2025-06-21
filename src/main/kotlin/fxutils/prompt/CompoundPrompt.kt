package fxutils.prompt

import fxutils.SubWindow
import javafx.scene.Node
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

open class CompoundPrompt<R : Any>(
    title: String, labelWidth: Double = DetailPane.LABEL_WIDTH,
    cancelText: String = "_Cancel",
    confirmText: String = "_Ok"
) : ConfirmablePrompt<R, DetailPane>(title, cancelText, confirmText) {
    private lateinit var confirm: () -> R?

    final override val content: DetailPane = DetailPane(labelWidth)

    override val windowType: SubWindow.Type
        get() = SubWindow.Type.Prompt

    init {
        VBox.setVgrow(content, Priority.ALWAYS)
    }

    override fun onReceiveFocus() {
        content.items().firstOrNull()?.control?.requestFocus()
    }

    fun <N : Node> addItem(name: String, node: N): N {
        content.addItem(name, node)
        return node
    }

    infix fun <N : Node> N.named(name: String): N = addItem(name, this)

    fun onConfirm(handler: () -> R?) {
        confirm = handler
    }

    override fun confirm(): R? = confirm.invoke()
}