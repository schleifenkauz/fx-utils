package fxutils.controls

import fxutils.label
import fxutils.prompt.TextPrompt
import fxutils.registerShortcuts
import fxutils.styleClass
import fxutils.undo.NoUndoManager
import fxutils.undo.UndoManager
import fxutils.undo.VariableEdit
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TextField
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import reaktive.Observer
import reaktive.value.*

class SliderBar<T : Any>(
    val value: ReactiveVariable<T>,
    private val name: ReactiveString,
    private val converter: Converter<T>,
    private val style: Style = Style.Regular,
    private val undoManager: UndoManager? = null,
    private val updateActionDescription: String? = null
) : StackPane() {
    constructor(
        value: ReactiveVariable<T>, name: String, converter: Converter<T>, style: Style = Style.Regular,
        undoManager: UndoManager? = null, updateActionDescription: String? = null
    ) : this(value, reactiveValue(name), converter, style, undoManager, updateActionDescription)

    private val bar = ProgressBar()
    private val nameLabel = label(name)
    private val valueLabel = Label()

    //TODO is this needed or is the prompt sufficient?
    private val valueInput = TextField() styleClass "sleek-text-field"
    private val valueObserver: Observer

    init {
        styleClass.add("slider-bar")
        children.add(bar)
        bar.prefWidthProperty().bind(widthProperty())
        if (style == Style.AlwaysValue) children.add(valueLabel)
        else children.add(nameLabel)
        nameLabel.prefWidthProperty().bind(widthProperty())
        nameLabel.alignment = Pos.CENTER
        valueLabel.prefWidthProperty().bind(widthProperty())
        valueLabel.alignment = Pos.CENTER
        addEventHandlers()
        setupTextFieldInput()
        valueObserver = value.forEach(::valueChanged)
    }

    private fun setupTextFieldInput() {
        valueInput.prefWidthProperty().bind(bar.widthProperty())
        valueInput.focusedProperty().addListener { _, _, focused ->
            if (!focused) showName()
        }
        valueInput.registerShortcuts {
            on("ESCAPE") {
                showName()
            }
            on("ENTER") {
                val v = converter.fromLiteral(valueInput.text) ?: return@on
                updateValue(v)
                showName()
            }
        }
    }

    private fun setActiveControl(node: Node) {
        children[1] = node
    }

    private fun valueChanged(newValue: T) {
        Platform.runLater {
            bar.progress = converter.toDouble(newValue)
            valueLabel.text = converter.toString(newValue)
        }
    }

    private fun addEventHandlers() {
        addEventHandler(MouseEvent.ANY) { ev ->
            when (ev.eventType) {
                MouseEvent.MOUSE_ENTERED -> showValue()
                MouseEvent.MOUSE_PRESSED -> if (ev.button == MouseButton.PRIMARY) setValueFromXCoordinate(ev.x)
                MouseEvent.MOUSE_DRAGGED -> if (ev.button == MouseButton.PRIMARY) setValueFromXCoordinate(ev.x)
                MouseEvent.MOUSE_RELEASED -> updateValue(converter.fromDouble(bar.progress))
                MouseEvent.MOUSE_EXITED -> showName()
                MouseEvent.MOUSE_CLICKED -> {
                    if (ev.button == MouseButton.SECONDARY) {
                        val v = Prompt().showDialog(anchorNode = this) ?: return@addEventHandler
                        updateValue(v)
                    } else {
                        showValue()
                    }
                }
            }
        }
    }

    private fun updateValue(v: T) {
        val oldValue = value.now
        if (v == oldValue) return
        value.now = v
        val actionDescription = updateActionDescription ?: "Update ${name.now}"
        undoManager?.record(VariableEdit(value, oldValue, v, actionDescription))
    }

    private inner class Prompt : TextPrompt<T>(nameLabel.text, converter.toString(value.now)) {
        override fun convert(text: String): T? = converter.fromLiteral(text)
    }

    private fun showValue() {
        setActiveControl(if (style == Style.AlwaysName) nameLabel else valueLabel)
    }

    private fun showName() {
        setActiveControl(if (style == Style.AlwaysValue) valueLabel else nameLabel)
    }

    private fun setValueFromXCoordinate(x: Double) {
        val doubleValue = (x / this.width).coerceIn(0.0, 1.0)
        val value = converter.fromDouble(doubleValue)
        bar.progress = converter.toDouble(value)
        valueLabel.text = converter.toString(value)
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
}