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
) : StackPane() {
    private val bar = ProgressBar()
    private val nameLabel = label(name)
    private val valueLabel = Label()
    private val valueInput = TextField() styleClass "sleek-text-field"
    private val valueObserver: Observer

    init {
        styleClass.add("slider-bar")
        children.addAll(bar, nameLabel)
        addEventHandlers()
        setupTextFieldInput()
        valueObserver = value.forEach(::valueChanged)
    }

    private fun setupTextFieldInput() {
        valueInput.focusedProperty().addListener { _, _, focused ->
            if (!focused) setActiveControl(nameLabel)
        }
        valueInput.registerShortcuts {
            on("ESC") {
                setActiveControl(nameLabel)
            }
            on("ENTER") {
                val v = converter.fromLiteral(valueInput.text) ?: return@on
                value.now = v
                setActiveControl(nameLabel)
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
                MouseEvent.MOUSE_ENTERED -> setActiveControl(valueLabel)
                MouseEvent.MOUSE_PRESSED -> setValueFromXCoordinate(ev.x)
                MouseEvent.MOUSE_DRAGGED -> setValueFromXCoordinate(ev.x)
                MouseEvent.MOUSE_RELEASED -> value.now = converter.fromDouble(bar.progress)
                MouseEvent.MOUSE_EXITED -> setActiveControl(nameLabel)
                MouseEvent.MOUSE_CLICKED -> {
                    if (ev.clickCount == 2) {
                        valueInput.text = converter.toString(value.now)
                        setActiveControl(valueInput)
                    } else {
                        setActiveControl(valueLabel)
                    }
                }
            }
        }
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
}