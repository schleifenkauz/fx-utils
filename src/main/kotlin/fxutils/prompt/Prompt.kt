package fxutils.prompt

import fxutils.SubWindow
import fxutils.styleClass
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Window

abstract class Prompt<R, N : Node> {
    private var commited = false
    private var result: R? = null
    protected lateinit var window: SubWindow

    protected abstract val content: N

    protected abstract val title: String

    protected fun commit(result: R) {
        commited = true
        this.result = result
        window.hide()
    }

    protected abstract fun getDefault(): R

    protected open fun onReceiveFocus() {
        content.requestFocus()
    }

    protected open fun createLayout(): Parent = VBox(
        Label(title) styleClass "dialog-title",
        content
    ) styleClass "dialog-box"

    fun showDialog(
        anchorNode: Node? = null,
        owner: Window? = null
    ): R {
        commited = false
        val layout = createLayout()
        window = SubWindow(layout, title, SubWindow.Type.Prompt)
        window.initOwner(owner ?: anchorNode?.scene?.window)
        window.setOnShown { onReceiveFocus() }
        window.sizeToScene()
        window.showAndWait()
        if (anchorNode != null) {
            val coords = anchorNode.localToScreen(0.0, 0.0)
            window.x = coords.x
            window.y = coords.y + ((anchorNode as? Region)?.height ?: 10.0) + 5.0
        }
        @Suppress("UNCHECKED_CAST")
        return if (commited) result as R else getDefault()
    }
}