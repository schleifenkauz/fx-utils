package fxutils.prompt

import fxutils.SubWindow
import fxutils.styleClass
import javafx.event.Event
import javafx.geometry.HPos
import javafx.geometry.HPos.*
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

    fun showDialog(owner: Window? = null, coords: Point2D? = null): R {
        val layout = createLayout()
        return showDialog(layout, owner, coords)
    }

    private fun showDialog(layout: Parent, owner: Window?, coords: Point2D?): R {
        commited = false
        window = SubWindow(layout, title, SubWindow.Type.Prompt)
        if (owner != null) window.initOwner(owner)
        window.setOnShown { onReceiveFocus() }
        window.sizeToScene()
        if (coords != null) {
            window.x = coords.x
            window.y = coords.y
        }
        window.showAndWait()
        @Suppress("UNCHECKED_CAST")
        return if (commited) result as R else getDefault()
    }

    fun showDialog(anchorNode: Region, alignment: HPos = LEFT): R {
        val layout = createLayout()
        val relativeX = when (alignment) {
            LEFT -> 0.0
            CENTER -> anchorNode.width / 2 //TODO how to determine the width of the dialog?
            RIGHT -> anchorNode.width
        }
        val coords = anchorNode.localToScreen(relativeX, anchorNode.height + 5.0)
        return showDialog(layout, anchorNode.scene.window, coords)
    }

    fun showDialog(ev: Event?): R {
        if (ev == null) return showDialog()
        val anchorNode = ev.source as? Region
        return if (anchorNode != null) showDialog(anchorNode)
        else when (val target = ev.target) {
            is Scene -> showDialog(target.window)
            is Region -> showDialog(target)
            is Window -> showDialog(target)
            else -> showDialog()
        }
    }
}