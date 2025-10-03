package fxutils.prompt

import fxutils.SubWindow
import fxutils.styleClass
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Popup
import javafx.stage.Screen
import javafx.stage.Window

abstract class Prompt<R> {
    private var commited = false
    private var result: R? = null
    protected var _window: SubWindow? = null
        private set
    protected val window get() = _window ?: error("Window for prompt $title not initialized")

    protected var anchor: Point2D? = null
        private set

    private var anchorNode: Region? = null
    private var anchorOffset: Point2D? = null

    protected var ownerWindow: Window? = null
        private set

    abstract val content: Node

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

    protected open fun beforeShowing() {

    }

    private fun showDialog(layout: Parent, owner: Window?, coords: Point2D?): R {
        beforeShowing()
        commited = false
        this.anchor = anchor
        if (_window == null) {
            _window = SubWindow(layout, title, windowType)
            if (owner != null && owner !is Popup && !(owner is SubWindow && owner.type == SubWindow.Type.Popup)) {
                window.initOwner(owner)
                this.ownerWindow = owner
            }
            window.setOnShown {
                onReceiveFocus()
            }
        }
        window.sizeToScene()
        if (coords != null) {
            window.setOnShown {
                onReceiveFocus()
                val screen =
                    Screen.getScreensForRectangle(coords.x, coords.y, 1.0, 1.0).firstOrNull() ?: Screen.getPrimary()
                val screenBounds = screen.bounds
                window.height = window.height.coerceAtMost(screenBounds.height)
                window.width = window.width.coerceAtMost(screenBounds.width)
                window.x = coords.x.coerceAtMost(screenBounds.maxX - window.width)
                window.y =
                    if (coords.y + window.height <= screenBounds.maxY) coords.y
                    else {
                        var y = coords.y - window.height
                        if (anchorNode != null) {
                            val anchorHeight = anchorNode!!.height
                            y -= anchorHeight
                            if (anchorOffset != null) y -= (anchorOffset!!.y - anchorHeight)
                        }
                        y.coerceAtLeast(0.0)
                    }
            }
        } else {
            val screen =
                Screen.getScreensForRectangle(window.x, window.y, 1.0, 1.0).firstOrNull() ?: Screen.getPrimary()
            window.centerOnScreen()
            window.maxHeight = screen.visualBounds.maxY - window.y
        }
        window.showAndWait()
        @Suppress("UNCHECKED_CAST")
        return if (commited) result as R else getDefault()
    }

    fun showDialog(anchorNode: Region, offset: Point2D = Point2D(0.0, anchorNode.height + 5.0)): R {
        val layout = createLayout()
        val coords = anchorNode.localToScreen(offset)
        this.anchorNode = anchorNode
        this.anchorOffset = offset
        return showDialog(layout, anchorNode.scene.window, coords)
    }

    fun showDialog(ev: Event?, offset: Point2D? = null, preferMouseCoords: Boolean = false): R {
        if (ev == null) return showDialog()
        val ownerWindow = (ev.source as? Node)?.scene?.window ?: (ev.target as? Node)?.scene?.window
        val anchorNode = ev.source as? Region
        return if (preferMouseCoords && ev is MouseEvent) showDialog(ownerWindow, Point2D(ev.screenX, ev.screenY))
        else if (anchorNode != null) showDialog(anchorNode, offset ?: Point2D(0.0, anchorNode.height + 5.0))
        else when (val target = ev.target) {
            is Scene -> showDialog(target.window)
            is Region -> showDialog(target, offset ?: Point2D(0.0, target.height + 5.0))
            is Window -> showDialog(target)
            else -> showDialog()
        }
    }

    protected fun hide() {
        window.hide()
    }
}