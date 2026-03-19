package fxutils.controls

import fxutils.label
import fxutils.registerShortcuts
import fxutils.runAfterLayout
import fxutils.styleClass
import fxutils.undo.UndoManager
import fxutils.undo.VariableEdit
import javafx.application.Platform
import javafx.geometry.Point2D
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TextField
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import javafx.scene.robot.Robot
import reaktive.Observer
import reaktive.event.event
import reaktive.value.*

class SliderBar<T : Any>(
    val value: ReactiveVariable<T>,
    private val name: ReactiveString,
    private val converter: Converter<T>,
    private val style: Style = Style.Regular,
    sliderWidth: Double = DEFAULT_SLIDER_WIDTH,
    private val sensitivity: Double = DEFAULT_SENSITIVITY,
    private val undoManager: UndoManager? = null,
    private val updateActionDescription: String? = null
) : StackPane() {
    constructor(
        value: ReactiveVariable<T>, name: String,
        converter: Converter<T>, style: Style = Style.Regular,
        sliderWidth: Double = DEFAULT_SLIDER_WIDTH, sensitivity: Double = DEFAULT_SENSITIVITY,
        undoManager: UndoManager? = null, updateActionDescription: String? = null
    ) : this(
        value, reactiveValue(name), converter, style,
        sliderWidth, sensitivity, undoManager, updateActionDescription
    )

    private val bar = ProgressBar()
    private val nameLabel = label(name) styleClass "slider-name-label"
    private val valueLabel = Label() styleClass "slider-value-label"

    private val valueInput = TextField().styleClass("sleek-text-field", "slider-value-input")
    private val valueObserver: Observer

    private var dragStartPosition = Point2D(0.0, 0.0)
    private var lastDragPosition = Point2D(0.0, 0.0)
    private var dragIsShiftDown = false
    private var dragStartValue = 0.0
    private val robot = Robot()

    private val updateFinish = event<T>()
    val updateFinished get() = updateFinish.stream

    init {
        styleClass.add("slider-bar")
        children.add(bar)
        minWidth = sliderWidth
        bar.viewOrder = 100.0
        bar.prefWidthProperty().bind(this.widthProperty())
        if (style == Style.AlwaysValue) children.add(valueLabel)
        else children.add(nameLabel)
        nameLabel.prefWidthProperty().bind(this.widthProperty())
        nameLabel.alignment = Pos.CENTER
        valueLabel.prefWidthProperty().bind(this.widthProperty())
        valueLabel.alignment = Pos.CENTER
        addEventHandlers()
        setupTextFieldInput()
        valueObserver = value.forEach(::valueChanged)
    }

    private fun setupTextFieldInput() {
        valueInput.prefWidthProperty().bind(bar.widthProperty())
        valueInput.focusedProperty().addListener { _, _, focused ->
            if (!focused) exitValueInput()
        }
        valueInput.registerShortcuts {
            on("ESCAPE") {
                exitValueInput()
            }
            on("ENTER") {
                val v = converter.fromLiteral(valueInput.text) ?: return@on
                updateValue(v)
                updateFinish.fire(v)
                exitValueInput()
            }
        }
    }

    private fun show(node: Node) {
        children[1] = node
    }

    private fun valueChanged(newValue: T) {
        Platform.runLater {
            bar.progress = converter.toDouble(newValue)
            valueLabel.text = converter.toString(newValue)
        }
    }

    private fun startValueDrag(ev: MouseEvent) {
        dragStartPosition = Point2D(ev.screenX, ev.screenY)
        lastDragPosition = dragStartPosition
        dragIsShiftDown = ev.isShiftDown
        dragStartValue = converter.toDouble(value.now)
        cursor = Cursor.NONE
    }

    private fun dragValue(ev: MouseEvent) {
        if (ev.isShiftDown != dragIsShiftDown) {
            dragIsShiftDown = ev.isShiftDown
            lastDragPosition = Point2D(ev.screenX, ev.screenY)
            dragStartValue = converter.toDouble(value.now)
            return
        }
        val deltaX = ev.screenX - lastDragPosition.x
        val sens = if (ev.isShiftDown) sensitivity / 10 else sensitivity
        val doubleValue = dragStartValue + (deltaX * sens)
        val value = converter.fromDouble(doubleValue.coerceIn(0.0, 1.0))
        updateValue(value)
    }

    private fun finishedValueDrag() {
        cursor = Cursor.DEFAULT
        robot.mouseMove(dragStartPosition)
        updateFinish.fire(value.now)
    }

    private fun updateValue(v: T) {
        val oldValue = value.now
        if (v == oldValue) return
        value.now = v
        val actionDescription = updateActionDescription ?: "Update ${name.now}"
        undoManager?.record(VariableEdit(value, oldValue, v, actionDescription))
    }

    private fun addEventHandlers() {
        addEventHandler(MouseEvent.ANY) { ev ->
            when (ev.eventType) {
                MouseEvent.MOUSE_ENTERED -> mouseEntered()
                MouseEvent.MOUSE_PRESSED -> if (ev.button == MouseButton.PRIMARY) startValueDrag(ev)
                MouseEvent.MOUSE_DRAGGED -> if (ev.button == MouseButton.PRIMARY) dragValue(ev)
                MouseEvent.MOUSE_RELEASED -> finishedValueDrag()
                MouseEvent.MOUSE_EXITED -> mouseExited()
                MouseEvent.MOUSE_CLICKED -> {
                    if (ev.button == MouseButton.SECONDARY) {
                        showTextFieldInput()
                    }
                }
                else -> return@addEventHandler
            }
            ev.consume()
        }
    }

    private fun mouseEntered() {
        if (style == Style.Regular && children[1] !is TextField) {
            show(valueLabel)
        }
    }

    private fun mouseExited() {
        if (style == Style.Regular && children[1] !is TextField) {
            show(nameLabel)
        }
    }

    private fun showTextFieldInput() {
        valueInput.text = converter.toString(value.now)
        show(valueInput)
        runAfterLayout {
            valueInput.requestFocus()
            valueInput.selectAll()
        }
    }

    private fun exitValueInput() {
        show(if (style == Style.AlwaysValue) valueLabel else nameLabel)
    }


    interface Converter<T> {
        fun fromLiteral(value: String): T?

        fun toString(value: T): String

        fun fromDouble(value: Double): T

        fun toDouble(value: T): Double
    }

    enum class Style {
        Regular, AlwaysValue, AlwaysName;
    }

    companion object {
        private const val DEFAULT_SLIDER_WIDTH = 70.0
        private const val DEFAULT_SENSITIVITY = 0.003
    }
}