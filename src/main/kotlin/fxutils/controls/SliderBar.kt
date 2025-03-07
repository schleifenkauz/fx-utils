package fxutils.controls

import fxutils.label
import fxutils.registerShortcuts
import fxutils.styleClass
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent
import javafx.scene.layout.StackPane
import reaktive.Observer
import reaktive.value.ReactiveString
import reaktive.value.ReactiveVariable
import reaktive.value.forEach
import reaktive.value.now

class SliderBar<T>(
    val value: ReactiveVariable<T>,
    name: ReactiveString,
    private val converter: Converter<T>,
    private val style: Style = Style.Regular
) : StackPane() {
    private val bar = ProgressBar()
    private val nameLabel = label(name)
    private val valueLabel = Label()
    private val valueInput = TextField() styleClass "sleek-text-field"
    private val valueObserver: Observer

    init {
        styleClass.add("slider-bar")
        children.add(bar)
        if (style == Style.AlwaysValue) children.add(valueLabel)
        else children.add(nameLabel)
        addEventHandlers()
        setupTextFieldInput()
        valueObserver = value.forEach(::valueChanged)
    }

    private fun setupTextFieldInput() {
        valueInput.focusedProperty().addListener { _, _, focused ->
            if (!focused) showName()
        }
        valueInput.registerShortcuts {
            on("ESC") {
                showName()
            }
            on("ENTER") {
                val v = converter.fromLiteral(valueInput.text) ?: return@on
                value.now = v
                showName()
            }
        }
    }

    private fun setActiveControl(node: Node) {
        children[1] = node
    }

    private fun valueChanged(newValue: T) {
        bar.progress = converter.toDouble(newValue)
        valueLabel.text = converter.toString(newValue)
    }

    private fun addEventHandlers() {
        addEventHandler(MouseEvent.ANY) { ev ->
            when (ev.eventType) {
                MouseEvent.MOUSE_ENTERED -> showValue()
                MouseEvent.MOUSE_PRESSED -> setValueFromXCoordinate(ev.x)
                MouseEvent.MOUSE_DRAGGED -> setValueFromXCoordinate(ev.x)
                MouseEvent.MOUSE_RELEASED -> value.now = converter.fromDouble(bar.progress)
                MouseEvent.MOUSE_EXITED -> showName()
                MouseEvent.MOUSE_CLICKED -> {
                    if (ev.clickCount == 2) {
                        valueInput.text = converter.toString(value.now)
                        setActiveControl(valueInput)
                    } else {
                        showValue()
                    }
                }
            }
        }
    }

    private fun showValue() {
        setActiveControl(if (style == Style.AlwaysName) nameLabel else valueLabel)
    }

    private fun showName() {
        setActiveControl(if (style == Style.AlwaysValue) valueLabel else nameLabel)
    }

    private fun setValueFromXCoordinate(x: Double) {
        val doubleValue = x / this.width
        bar.progress = doubleValue
        val value = converter.fromDouble(doubleValue)
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