/**
 * @author Nikolaus Knop
 */

@file:Suppress("UsePropertyAccessSyntax")

package fxutils

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableValue
import javafx.css.PseudoClass
import javafx.event.ActionEvent
import javafx.geometry.Bounds
import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.*
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.stage.Popup
import javafx.stage.PopupWindow
import javafx.stage.Stage
import javafx.stage.Window
import org.controlsfx.control.ToggleSwitch
import reaktive.value.ReactiveBoolean
import reaktive.value.ReactiveVariable
import reaktive.value.fx.asObservableValue
import reaktive.value.fx.asProperty
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sqrt

fun control(skin: Skin<out Control>): Control {
    return object : Control() {
        init {
            setSkin(skin)
        }
    }
}

fun Control.setRoot(node: Node) {
    skin = null
    skin = skin(this, node)
}

fun skin(control: Control, node: Node): Skin<Control> = SimpleSkin(control, node)


private class SimpleSkin(
    private val control: Control, private val node: Node
) : Skin<Control> {
    override fun getSkinnable(): Control = control

    override fun getNode(): Node = node

    override fun dispose() {}
}

inline fun Node.registerShortcut(s: KeyCombination, crossinline action: () -> Unit) {
    addEventHandler(KeyEvent.KEY_RELEASED) { k ->
        if (s.match(k)) {
            action()
            k.consume()
        }
    }
}

fun PopupWindow.showBelow(node: Node) {
    val p = node.localToScreen(0.0, node.prefHeight(-1.0)) ?: return
    show(node, p.x, p.y)
}

fun TextField.smartSetText(new: String) {
    val previous = text
    if (previous != new) {
        text = new
        if (new.startsWith(previous)) {
            positionCaret(new.length)
        }
    }
}

fun Node.onAction(action: () -> Unit) {
    addEventHandler(KeyEvent.KEY_RELEASED) { ev ->
        if (ev.code == ENTER) {
            action()
            ev.consume()
        }
    }
    addEventHandler(MouseEvent.MOUSE_PRESSED) { ev ->
        if (ev.clickCount >= 2) {
            action()
            ev.consume()
        }
    }
}

/**
 * Return a [Label] with the given text and with the 'hextant-text' and the 'keyword' style class.
 */
fun keyword(name: String) = Label(name).apply {
    styleClass.add("hextant-text")
    styleClass.add("keyword")
}

/**
 * Return a [Label] with the given text and with the 'hextant-text' and the 'operator' style class.
 */
fun operator(name: String) = Label(name).apply {
    styleClass.add("hextant-text")
    styleClass.add("operator")
}

fun Region.fixWidth(value: Double) {
    prefWidth = value
    minWidth = value
    maxWidth = value
}

fun <N : Node> N.withStyleClass(vararg names: String) = apply { styleClass.addAll(*names) }

fun <N : Node> N.withStyle(style: String) = also { it.style = style }

fun <C : Control> C.withTooltip(tooltip: Tooltip) = apply { this.tooltip = tooltip }

fun <C : Control> C.withTooltip(text: String) = withTooltip(Tooltip(text))

fun hextantLabel(text: String, graphic: Node? = null) = Label(text, graphic).withStyleClass("hextant-text")


fun Dialog<*>.setDefaultButton(type: ButtonType) {
    for (tp in dialogPane.buttonTypes) {
        val button = dialogPane.lookupButton(tp) as Button
        button.isDefaultButton = tp == type
    }
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

inline fun showConfirmationAlert(yesButton: ButtonType = ButtonType.YES, config: Alert.() -> Unit): Boolean =
    Alert(Alert.AlertType.CONFIRMATION).showDialog(config) == yesButton

/**
 * Enqueues the given [action] into the JavaFX application thread after some [delay] which is given in milliseconds.
 */
fun runFXWithTimeout(delay: Long = 10, action: () -> Unit) {
    thread {
        Thread.sleep(delay)
        Platform.runLater(action)
    }
}


operator fun Point2D.plus(other: Point2D) = Point2D(x + other.x, y + other.y)

operator fun Point2D.component1() = x
operator fun Point2D.component2() = y

infix fun Point2D.dist(p: Point2D) = sqrt((p.x - x).pow(2) + (p.y - y).pow(2))

fun <N : Node> N.styleClass(vararg classes: String) = also { it.styleClass.addAll(classes) }

infix fun <N : Node> N.styleClass(name: String) = also { it.styleClass.add(name) }

fun button(text: String = "", onAction: (ev: ActionEvent) -> Unit = {}) =
    Button(text.escapeUnderscores()).styleClass("sleek-button").also { btn -> btn.setOnAction(onAction) }

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

fun <T, F> ObservableValue<out T>.map(f: (T) -> F): ObservableValue<F> =
    Bindings.createObjectBinding({ f(value) }, this)

fun <N : Region> N.alwaysHGrow() = also {
    maxWidth = Double.MAX_VALUE
    HBox.setHgrow(it, Priority.ALWAYS)
}

fun <N : Node> N.neverHGrow() = also { HBox.setHgrow(it, Priority.NEVER) }
fun <N : Node> N.alwaysVGrow() = also { VBox.setVgrow(it, Priority.ALWAYS) }

fun hspace(width: Double) = Region().apply { prefWidth = width }

fun infiniteSpace() = Region().alwaysHGrow().alwaysVGrow()

fun <N : Node> N.centerChildren() = also {
    when (it) {
        is HBox -> it.alignment = Pos.CENTER_LEFT
        is VBox -> it.alignment = Pos.TOP_CENTER
        else -> {}
    }
}

fun Region.centerHorizontally(parent: Region) {
    layoutXProperty().bind(parent.widthProperty().subtract(widthProperty()).divide(2))
}

fun Node.setupDropArea(condition: (db: Dragboard) -> Boolean, onDrop: (ev: DragEvent) -> Unit) {
    addEventHandler(DragEvent.DRAG_OVER) { ev ->
        if (condition(ev.dragboard)) {
            ev.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            ev.consume()
        }
    }
    addEventHandler(DragEvent.DRAG_ENTERED) { ev ->
        if (condition(ev.dragboard)) {
            setPseudoClassState("drop-possible", true)
            ev.consume()
        }
    }
    addEventHandler(DragEvent.DRAG_EXITED) { ev ->
        if (condition(ev.dragboard)) {
            setPseudoClassState("drop-possible", false)
            ev.consume()
        }
    }
    addEventHandler(DragEvent.DRAG_DROPPED) { ev ->
        if (condition(ev.dragboard)) {
            try {
                onDrop(ev)
            } catch (ex: Exception) {
                System.err.println("Exception while dropping")
                ex.printStackTrace()
            }
            ev.isDropCompleted = true
            ev.consume()
        }
    }
}

fun Dragboard.hasFiles(extension: String) =
    hasFiles() && files.all { f -> f.extension.equals(extension, ignoreCase = true) }

fun Dragboard.hasFile(extension: String): Boolean = hasFiles(extension) && files.size == 1

fun ToggleSwitch.sync(variable: ReactiveVariable<Boolean>): ToggleSwitch {
    selectedProperty().bindBidirectional(variable.asProperty())
    return this
}

fun Spinner<Int>.sync(variable: ReactiveVariable<Int>): Spinner<Int> {
    valueFactory.valueProperty().bindBidirectional(variable.asProperty())
    return this
}

fun <T> runOnApplicationThread(action: () -> T): T {
    val future = CompletableFuture<T>()
    Platform.runLater {
        val result = action()
        future.complete(result)
    }
    return future.get()
}

fun solidBorder(fill: Color, width: Double = 1.0, radius: Double = 0.0) =
    Border(BorderStroke(fill, BorderStrokeStyle.SOLID, CornerRadii(radius), BorderWidths(width), Insets(-width)))

val Bounds.middleX get() = (minX + maxX) / 2
val Bounds.middleY get() = (minY + maxY) / 2

fun plural(noun: String) = if (noun.endsWith("s")) "${noun}es" else "${noun}s"

fun Node.setPseudoClassState(name: String, value: Boolean) {
    pseudoClassStateChanged(PseudoClass.getPseudoClass(name), value)
}

fun background(color: Color) = Background(BackgroundFill(color, null, null))

fun Window.resize(width: Double, height: Double) {
    this.width = width
    this.height = height
}

fun Window.relocate(x: Double, y: Double) {
    this.x = x
    this.y = y
}

fun <R : Region> R.setFixedWidth(width: Double) = also { r ->
    r.prefWidth = width
    r.minWidth = width
    r.maxWidth = width
}


private val robot by lazy { Robot() }

val Node.mousePosition: Point2D get() = screenToLocal(robot.mousePosition)
val Node.mouseX get() = mousePosition.x
val Node.mouseY get() = mousePosition.y

fun String.canonicalizeDecimal(): String {
    if ('.' !in this) return this
    return dropLastWhile { c -> c == '0' }.removeSuffix(".")
}

fun Double.format(accuracy: Int) = String.format(Locale.US, "%.${accuracy}f", this).canonicalizeDecimal()

fun Button.disableIf(condition: ReactiveBoolean): Button {
    disableProperty().bind(condition.asObservableValue())
    return this
}

fun ScrollPane.letContentFillViewPort(): ScrollPane {
    val c = content as? Region ?: return this
    c.minWidthProperty().bind(viewportBoundsProperty().map(Bounds::getWidth))
    c.minHeightProperty().bind(viewportBoundsProperty().map(Bounds::getHeight))
    return this
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

fun <W: Window> W.defaultSize(width: Double, height: Double): W {
    this.width = width
    this.height = height
    return this
}

fun <N: Region> N.pad(value: Double): N = also { padding = Insets(value) }

fun Color.opacity(opacity: Double) = deriveColor(0.0, 0.0, 0.0, opacity)