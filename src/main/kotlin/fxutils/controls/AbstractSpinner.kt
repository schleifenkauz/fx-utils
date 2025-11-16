package fxutils.controls

import fxutils.actions.action
import fxutils.actions.makeButton
import fxutils.autoSize
import fxutils.setRoot
import fxutils.styleClass
import fxutils.undo.UndoManager
import fxutils.undo.VariableEdit
import javafx.css.PseudoClass
import javafx.scene.control.Control
import javafx.scene.control.TextField
import javafx.scene.control.skin.TextFieldSkin
import javafx.scene.layout.HBox
import org.kordamp.ikonli.materialdesign2.MaterialDesignC
import reaktive.Observer
import reaktive.value.ReactiveVariable
import reaktive.value.binding.equalTo
import reaktive.value.binding.or
import reaktive.value.forEach
import reaktive.value.fx.asObservableValue
import reaktive.value.fx.asReactiveValue
import reaktive.value.now

abstract class AbstractSpinner<T : Comparable<T>>(
    val value: ReactiveVariable<T>, private var min: T, private var max: T
) : Control() {
    private var undoManager: UndoManager? = null
    private var variableDescription: String? = null
    private lateinit var valueObserver: Observer
    private val valueInput: TextField = TextField()
    private val btnDecrement = decrementAction.withContext(this).makeButton("small-icon-button")
    private val btnIncrement = incrementAction.withContext(this).makeButton("small-icon-button")
    private var onUserInput: (T) -> Unit = {}
    private var minColumns: Int = 1

    init {
        setRoot(HBox(btnDecrement, valueInput, btnIncrement) styleClass "spinner")
        isFocusTraversable = false
        valueInput.skin = ValueInputSkin()
        valueInput.autoSize(minColumns = this::minColumns)
        valueInput.editableProperty().bind(disabledProperty().not())
        btnDecrement.disableProperty().bind(
            value.equalTo(min).or(disabledProperty().asReactiveValue()).asObservableValue()
        )
        btnIncrement.disableProperty().bind(
            value.equalTo(max).or(disabledProperty().asReactiveValue()).asObservableValue()
        )
    }

    protected fun bind() {
        valueInput.focusedProperty().addListener { _, _, focused ->
            if (!focused) {
                updateValueFromTextInput()
            }
        }
        valueInput.setOnAction { updateValueFromTextInput() }
        valueObserver = value.forEach { v ->
            valueInput.text = toString(v)
        }
    }

    fun value(): T = value.now

    protected abstract fun increment(value: T): T
    protected abstract fun decrement(value: T): T
    protected abstract fun parseValue(text: String): T?
    protected open fun toString(value: T): String = value.toString()

    fun setMin(v: T) {
        min = v
        if (value.now < min) {
            value.now = min
        }
    }

    fun setMax(v: T) {
        max = v
        if (value.now > max) {
            value.now = max
        }
    }

    fun setRange(range: ClosedRange<T>) {
        setMin(range.start)
        setMax(range.endInclusive)
    }

    fun setupUndo(variableDescription: String?, manager: UndoManager?) = also {
        undoManager = manager
        this.variableDescription = variableDescription
    }

    fun onUserInput(handler: (newValue: T) -> Unit) = also {
        onUserInput = handler
    }

    fun enableTextInput() = also {
        valueInput.isEditable = true
    }

    fun disableTextInput() = also {
        valueInput.isEditable = false
    }

    fun minColumns(n: Int) = also {
        minColumns = n
    }

    fun display(value: T) {
        if (this.value.now == value) return
        this.value.now = value
    }

    private fun increment() {
        val newValue = increment(value.now)
        if (newValue <= max) {
            updateValue(newValue, "Increment")
        }
    }

    private fun decrement() {
        val newValue = decrement(value.now)
        if (newValue >= min) {
            updateValue(newValue, "Decrement")
        }
    }

    private fun updatedText(text: String) {
        val value = parseValue(text)
        valueInput.style =
            if (value != null) "-fx-text-fill: green"
            else "-fx-text-fill: red"
    }

    private fun updateValueFromTextInput() {
        val newValue = parseValue(valueInput.text)?.coerceIn(min, max)
        if (newValue != null) {
            updateValue(newValue, "Update")
        }
        valueInput.text = value.now.toString()
        onUserInput(value.now)
        valueInput.style = ""
    }

    private fun updateValue(newValue: T, actionDescription: String) {
        value.now = newValue
        undoManager?.record(VariableEdit(value, value.now, newValue, "$actionDescription $variableDescription"))
    }

    override fun requestFocus() {
        valueInput.requestFocus()
    }

    private inner class ValueInputSkin : TextFieldSkin(valueInput) {
        override fun replaceText(start: Int, end: Int, text: String) {
            super.replaceText(start, end, text)
            updatedText(valueInput.text)
        }

        override fun deleteChar(previous: Boolean) {
            super.deleteChar(previous)
            updatedText(valueInput.text)
        }
    }

    companion object {
        private val incrementAction = action<AbstractSpinner<*>>("Increment") {
            icon(MaterialDesignC.CHEVRON_RIGHT)
            shortcut("Ctrl+PLUS")
            executes(AbstractSpinner<*>::increment)
        }
        private val decrementAction = action<AbstractSpinner<*>>("Decrement") {
            icon(MaterialDesignC.CHEVRON_LEFT)
            shortcut("Ctrl+MINUS")
            executes(AbstractSpinner<*>::decrement)
        }
    }
}