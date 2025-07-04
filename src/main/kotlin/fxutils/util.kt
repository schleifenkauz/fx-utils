/**
 * @author Nikolaus Knop
 */

@file:Suppress("UsePropertyAccessSyntax")

package fxutils

import fxutils.undo.ToggleEdit
import fxutils.undo.UndoManager
import fxutils.undo.VariableEdit
import javafx.animation.AnimationTimer
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.value.ObservableNumberValue
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.css.PseudoClass
import javafx.event.ActionEvent
import javafx.geometry.*
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.robot.Robot
import javafx.scene.shape.Rectangle
import javafx.stage.Window
import org.controlsfx.control.ToggleSwitch
import reaktive.Observer
import reaktive.value.ReactiveBoolean
import reaktive.value.ReactiveVariable
import reaktive.value.forEach
import reaktive.value.fx.asObservableValue
import reaktive.value.fx.asProperty
import reaktive.value.now
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
    val current = skin
    if (current is SimpleSkin && current.node == node) return
    skin = null
    skin = skin(this, node)
}

fun skin(control: Control, node: Node): Skin<Control> = SimpleSkin(control, node)


private class SimpleSkin(
    private val control: Control, private val node: Node,
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

inline fun showConfirmationAlert(yesButton: ButtonType = ButtonType.YES, config: Alert.() -> Unit): Boolean =
    Alert(Alert.AlertType.CONFIRMATION).showDialog(config) == yesButton

private val fxScheduler by lazy {
    Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "fx-scheduler").also { it.isDaemon = true }
    }
}

/**
 * Enqueues the given [action] into the JavaFX application thread after some [delay] which is given in milliseconds.
 */
fun runFXWithTimeout(delay: Long = 10, action: () -> Unit) {
    fxScheduler.schedule({ Platform.runLater(action) }, delay, TimeUnit.MILLISECONDS)
}

fun runAfterLayout(action: () -> Unit) {
    Platform.runLater {
        object : AnimationTimer() {
            private var alreadyRun = false

            override fun handle(now: Long) {
                if (alreadyRun) return
                stop()
                action()
                alreadyRun = true
            }
        }.start()
    }
}


operator fun Point2D.plus(other: Point2D) = Point2D(x + other.x, y + other.y)

operator fun Point2D.component1() = x
operator fun Point2D.component2() = y

infix fun Point2D.dist(p: Point2D) = sqrt((p.x - x).pow(2) + (p.y - y).pow(2))

fun <N : Node> N.styleClass(vararg classes: String) = also { it.styleClass.addAll(classes) }

infix fun <N : Node> N.styleClass(name: String) = also { it.styleClass.add(name) }

infix fun <N : Node> N.style(style: String) = also { it.style = style }

fun button(text: String = "", style: String = "sleek-button", onAction: (ev: ActionEvent) -> Unit = {}) =
    Button(text.escapeUnderscores()).styleClass(style).also { btn -> btn.setOnAction(onAction) }

fun <T, F> ObservableValue<out T>.map(f: (T) -> F): ObservableValue<F> =
    Bindings.createObjectBinding({ f(value) }, this)

fun ObservableNumberValue.pow(x: Double) = map { v -> v.toDouble().pow(x) }

fun ObservableNumberValue.sqrt() = map { v -> sqrt(v.toDouble()) }

fun <N : Region> N.alwaysHGrow() = also {
    maxWidth = Double.MAX_VALUE
    HBox.setHgrow(it, Priority.ALWAYS)
}

fun <N : Node> N.neverHGrow() = also { HBox.setHgrow(it, Priority.NEVER) }
fun <N : Node> N.alwaysVGrow() = also { VBox.setVgrow(it, Priority.ALWAYS) }

fun hspace(width: Double) = Region().apply { prefWidth = width }
fun vspace(height: Double) = Region().apply { prefHeight = height }

fun infiniteSpace() = Region().alwaysHGrow().alwaysVGrow()

fun <N : Node> N.centerChildren() = also {
    when (it) {
        is HBox -> it.alignment = Pos.CENTER_LEFT
        is VBox -> it.alignment = Pos.TOP_CENTER
        else -> {}
    }
}

fun VBox.center() = also { alignment = Pos.CENTER }
fun HBox.center() = also { alignment = Pos.CENTER }

fun Node.centered(): StackPane {
    val pane = StackPane(this)
    StackPane.setAlignment(this, Pos.CENTER)
    return pane
}

fun Region.centerHorizontally(parent: Region) {
    layoutXProperty().bind(parent.widthProperty().subtract(widthProperty()).divide(2))
}

fun Region.centerVertically(parent: Region) {
    layoutYProperty().bind(parent.heightProperty().subtract(heightProperty()).divide(2))
}

fun Region.centerIn(parent: Region) {
    centerHorizontally(parent)
    centerVertically(parent)
}

fun ToggleSwitch.sync(variable: ReactiveVariable<Boolean>): ToggleSwitch {
    selectedProperty().bindBidirectional(variable.asProperty())
    return this
}

fun CheckBox.sync(variable: ReactiveVariable<Boolean>): CheckBox {
    selectedProperty().bindBidirectional(variable.asProperty())
    return this
}

fun CheckBox.sync(variable: ReactiveVariable<Boolean>, description: String, undo: UndoManager): CheckBox {
    isSelected = variable.now
    selectedProperty().addListener { _, _, selected ->
        if (variable.now != selected) {
            variable.set(selected)
            undo.record(ToggleEdit("Toggle $description", variable))
        }
    }
    userData = variable.observe { _, _, selected -> isSelected = selected }
    return this
}

fun <T> Spinner<T>.sync(variable: ReactiveVariable<T>): Spinner<T> {
    valueFactory.valueProperty().bindBidirectional(variable.asProperty())
    return this
}

fun <T> Spinner<T>.sync(variable: ReactiveVariable<T>, description: String, undo: UndoManager): Spinner<T> {
    valueProperty().addListener { _, _, value ->
        if (variable.now != value) {
            val oldValue = variable.now
            variable.set(value)
            undo.record(VariableEdit(variable, oldValue, value, "Update $description"))
        }
    }
    userData = variable.observe { _, _, v -> valueFactory.value = v }
    return this
}

fun Node.bindPseudoClassState(name: String, active: ReactiveBoolean): Observer {
    val pseudoClass = PseudoClass.getPseudoClass(name)
    return active.forEach { state ->
        pseudoClassStateChanged(pseudoClass, state)
    }
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

val Rectangle.middleX get() = x + (width / 2)
val Rectangle.middleY get() = y + (height / 2)
val Rectangle.middlePoint get() = Point2D(middleX, middleY)

fun plural(noun: String) = if (noun.endsWith("s")) "${noun}es" else "${noun}s"

fun Node.setPseudoClassState(name: String, value: Boolean) {
    pseudoClassStateChanged(PseudoClass.getPseudoClass(name), value)
}

fun background(color: Color) = Background(BackgroundFill(color, null, null))

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

fun <W : Window> W.defaultSize(width: Double, height: Double): W {
    this.width = width
    this.height = height
    return this
}

fun <N : Region> N.pad(value: Double): N = also { padding = Insets(value) }

fun Color.opacity(opacity: Double) = deriveColor(0.0, 0.0, 0.0, opacity)

fun Rectangle2D.middlePoint() = Point2D((minX + maxX) / 2, (minY + maxY) / 2)

fun Node.isActuallyVisible(): Boolean {
    if (scene == null) return false
    var current: Node? = this
    while (current != null) {
        if (!current.isVisible()) return false
        current = current.getParent()
    }
    return true
}

fun ObservableList<Node>.addAfter(node: Node, newChild: Node) {
    val idx = indexOf(node)
    add(idx + 1, newChild)
}

fun ObservableList<Node>.replace(node: Node, newChild: Node?) {
    val idx = indexOf(node)
    if (newChild == null) removeAt(idx)
    else set(idx, newChild)
}

fun ObservableList<Node>.replace(node: Node?, newChild: Node?, defaultIdx: () -> Int = { size }) {
    val idx = indexOf(node)
    if (idx == -1) {
        if (newChild == null) return
        add(defaultIdx(), newChild)
    } else {
        if (newChild == null) removeAt(idx)
        else set(idx, newChild)
    }
}

fun Pane.reorient(orientation: Orientation): Pane {
    when (this) {
        is HBox -> if (orientation == Orientation.HORIZONTAL) return this
        is VBox -> if (orientation == Orientation.VERTICAL) return this
        else -> throw IllegalStateException("Pane must be of type HBox or VBox")
    }
    val items = children.toTypedArray()
    children.clear()
    val newPane = when (orientation) {
        Orientation.HORIZONTAL -> HBox(*items)
        Orientation.VERTICAL -> VBox(*items)
    }
    newPane.styleClass.addAll(this.styleClass)
    return newPane
}

fun ScrollPane.neverSquishHorizontally() = apply {
    minViewportWidthProperty().bind(contentProperty().flatMap { content ->
        (content as? Region)?.widthProperty() ?: SimpleDoubleProperty(0.0)
    })
    hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
    isFitToWidth = false
}

fun ScrollPane.neverSquishVertically() = apply {
    minViewportHeightProperty().bind(contentProperty().flatMap { content ->
        (content as? Region)?.heightProperty() ?: SimpleDoubleProperty(0.0)
    })
    vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
    isFitToHeight = false
}

fun ScrollPane.neverSquish() = apply {
    neverSquishHorizontally()
    neverSquishVertically()
}