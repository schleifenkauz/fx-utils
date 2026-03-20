package fxutils.prompt

import fxutils.SubWindow
import fxutils.styleClass
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Popup
import javafx.stage.Window

abstract class Prompt<R> {
    private var commited = false
    private var result: R? = null
    protected var _window: SubWindow? = null
        private set
    protected val window get() = _window ?: error("Window for prompt $title not initialized")

    var _placement: PromptPlacement? = null

    val placement: PromptPlacement get() = _placement ?: error("Prompt $title not yet placed.")

    val anchorNode: Region? get() = (placement as? PromptPlacement.RelativeTo)?.anchor
    val ownerWindow: Window? get() = placement.parentWindow

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

    protected open fun createLayout(): Region = VBox(
        Label(title) styleClass "dialog-title",
        content
    ) styleClass "dialog-box"

    protected open fun beforeShowing() {

    }

    fun showDialog(placement: PromptPlacement = PromptPlacement.Centered()): R {
        beforeShowing()
        val owner = placement.parentWindow
        val content = createLayout()
        if (_window == null) {
            _window = SubWindow(content, title, windowType)
            if (owner != null && owner !is Popup && !(owner is SubWindow && owner.type == SubWindow.Type.Popup)) {
                window.initOwner(owner)
            }
        }
        window.sizeToScene()
        _placement = placement
        window.setOnShown {
            val pos = placement.getPosition(content)
            window.x = pos.x
            window.y = pos.y
            onReceiveFocus()
        }
        window.showAndWait()
        return result ?: getDefault()
    }

    fun showDialog(anchorNode: Region, offset: Point2D = Point2D(0.0, anchorNode.height + 5.0)): R {
        val placement = PromptPlacement.RelativeTo(anchorNode)
        return showDialog(placement)
    }

    fun showDialog(owner: Window): R {
        val placement = PromptPlacement.Centered(owner)
        return showDialog(placement)
    }

    fun showDialog(owner: Window, coords: Point2D): R {
        val placement = PromptPlacement.At(coords.x, coords.y, owner)
        return showDialog(placement)
    }

    fun showDialog(ev: Event?, preferMouseCoords: Boolean = false): R {
        val placement = when {
            ev is MouseEvent && preferMouseCoords -> ev.atMouseCoords()
            ev != null -> ev.nextToTarget()
            else -> PromptPlacement.Centered()
        }
        return showDialog(placement)
    }

    fun showDialog(parentPrompt: Prompt<*>): R = showDialog(parentPrompt.placement)

    protected fun hide() {
        window.hide()
    }
}