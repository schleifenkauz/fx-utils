package fxutils.prompt

import fxutils.SubWindow
import fxutils.styleClass
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Window

abstract class Prompt<R, N : Node> {
    private var commited = false
    private var result: R? = null
    private var _window: SubWindow? = null
    protected val window get() = _window ?: error("Window for prompt $title not initialized")

    protected abstract val content: N

    protected abstract val title: String

    protected open val windowType get() = SubWindow.Type.Popup

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

    fun showDialog(owner: Window? = null, coords: Point2D? = null): R {
        val layout = createLayout()
        return showDialog(layout, owner, coords)
    }

    private fun showDialog(layout: Parent, owner: Window?, coords: Point2D?): R {
        commited = false
        if (_window == null) {
            _window = SubWindow(layout, title, windowType)
            if (owner != null) window.initOwner(owner)
            window.setOnShown { onReceiveFocus() }
        }
        window.sizeToScene()
        if (coords != null) {
            window.x = coords.x
            window.y = coords.y
        } else window.centerOnScreen()
        window.showAndWait()
        @Suppress("UNCHECKED_CAST")
        return if (commited) result as R else getDefault()
    }

    fun showDialog(anchorNode: Region, offset: Point2D = Point2D(0.0, anchorNode.height + 5.0)): R {
        val layout = createLayout()
        val coords = anchorNode.localToScreen(offset)
        return showDialog(layout, anchorNode.scene.window, coords)
    }

    fun showDialog(ev: Event?, offset: Point2D? = null): R {
        if (ev == null) return showDialog()
        val anchorNode = ev.source as? Region
        return if (anchorNode != null) showDialog(anchorNode, offset ?: Point2D(0.0, anchorNode.height + 5.0))
        else when (val target = ev.target) {
            is Scene -> showDialog(target.window)
            is Region -> showDialog(target, offset ?: Point2D(0.0, target.height + 5.0))
            is Window -> showDialog(target)
            else -> showDialog()
        }
    }
}