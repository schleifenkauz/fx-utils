package fxutils

import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Dialog
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.stage.*

fun PopupWindow.showBelow(node: Node) {
    val p = node.localToScreen(0.0, node.prefHeight(-1.0)) ?: return
    show(node, p.x, p.y)
}

inline fun <R, D : Dialog<R>> D.showDialog(config: D.() -> Unit = {}): R? {
    config()
    isResizable = true
    setOnShown {
        runFXWithTimeout(delay = 100) {
            isResizable = false
        }
    }
    return showAndWait().orElse(null)
}

fun <R> showDialog(config: Dialog<R>.() -> Unit): R? = Dialog<R>().showDialog(config)
fun showPopup(owner: Node, node: Node) = popup(node).showBelow(owner)
fun Popup.show(owner: Node) {
    val coords = owner.localToScreen(0.0, 0.0)
    show(owner, coords.x, coords.y)
}

fun Popup.showBelow(owner: Region) {
    val coords = owner.localToScreen(0.0, owner.height)
    show(owner, coords.x, coords.y)
}

fun Popup.showRightOf(owner: Region) {
    val coords = owner.localToScreen(owner.width, 0.0)
    show(owner, coords.x, coords.y)
}

inline fun popup(node: Node, block: Popup.() -> Unit = {}) = Popup().apply {
    content.add(node)
    isAutoHide = true
    block()
}

fun Window.resize(width: Double, height: Double) {
    this.width = width
    this.height = height
}

fun Window.relocate(x: Double, y: Double) {
    this.x = x
    this.y = y
}

fun Stage.show(coords: Point2D) {
    x = coords.x
    y = coords.y
    show()
}

fun Stage.showCentered(owner: Window) {
    if (this.owner == null) initOwner(owner)
    centerOnScreen()
    show()
}

fun Stage.show(anchorNode: Node, offset: Point2D) {
    if (this.owner == null) initOwner(anchorNode.scene.window)
    val pos = anchorNode.localToScreen(offset)
    show(pos)
}

fun Stage.showBelow(anchorNode: Region) {
    show(anchorNode, Point2D(0.0, anchorNode.height))
}

fun Stage.showRightOf(anchorNode: Region) {
    show(anchorNode, Point2D(anchorNode.width, 0.0))
}

fun undecoratedSubWindow(root: Parent) = SubWindow(root, "", SubWindow.Type.Undecorated)

fun Event?.popupAnchor(): Point2D = when {
    this != null && source is Region -> {
        val source = source as Region
        source.localToScreen(0.0, source.height)
    }
    this is MouseEvent -> Point2D(screenX, screenY)

    else -> Screen.getPrimary().visualBounds.middlePoint()
}

val Event?.sourceWindow: Window?
    get() = when (val src = this?.source) {
        is Window -> src
        is Scene -> src.window
        is Node -> src.scene.window
        else -> null
    }

fun Node.showAsPopup(ev: Event? = null) {
    val popup = asPopup()
    popup.show(ev)
}

fun Popup.show(ev: Event?) {
    val anchor = ev.popupAnchor()
    val anchorNode = ev?.source as? Node
    show(anchorNode, anchor.x, anchor.y)
}

fun Node.asPopup(): Popup {
    val popup = Popup()
    popup.content.add(this)
    popup.isAutoHide = true
    return popup
}

fun Stage.showAndBringToFront() {
    if (!isShowing) {
        setOnShown {
            toFront()
            scene.root.requestFocus()
        }
        show()
    } else {
        toFront()
        scene.root.requestFocus()
    }
}