package fxutils.controls

import fxutils.actions.action
import fxutils.actions.makeButton
import fxutils.autoSize
import fxutils.setRoot
import fxutils.styleClass
import fxutils.undo.UndoManager
import fxutils.undo.VariableEdit
import javafx.scene.control.Control
import javafx.scene.control.TextField
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
import reaktive.value.reactiveVariable

class IntSpinner(
    val value: ReactiveVariable<Int>,
    private val min: Int, private val max: Int, private val step: Int = 1,
) : Control() {
    constructor(
        value: ReactiveVariable<Int>, range: IntRange, step: Int = 1,
    ) : this(value, range.first, range.last, step)

    constructor(
        min: Int, max: Int, initialValue: Int, step: Int = 1,
    ) : this(reactiveVariable(initialValue), min, max, step)

    constructor(
        range: IntRange, initialValue: Int, step: Int = 1,
    ) : this(range.first, range.last, initialValue, step)

    private var undoManager: UndoManager? = null
    private var variableDescription: String? = null

    private val valueObserver: Observer

    private val valueInput: TextField = TextField()
    private val btnDecrement = decrementAction.withContext(this).makeButton("small-icon-button")
    private val btnIncrement = incrementAction.withContext(this).makeButton("small-icon-button")

    private var onUserInput: (Int) -> Unit = {}

    private var minColumns: Int = 1

    fun value(): Int = value.now

    init {
        setRoot(HBox(btnDecrement, valueInput, btnIncrement) styleClass "int-spinner")
        valueInput.focusedProperty().addListener { _, _, focused ->
            if (!focused) {
                updateValueFromTextInput()
            }
        }
        valueInput.setOnAction { updateValueFromTextInput() }
        valueObserver = value.forEach { v ->
            valueInput.text = v.toString()
        }
        valueInput.autoSize(minColumns = this::minColumns)
        valueInput.editableProperty().bind(disabledProperty().not())
        btnDecrement.disableProperty().bind(
            value.equalTo(min).or(disabledProperty().asReactiveValue()).asObservableValue()
        )
        btnIncrement.disableProperty().bind(
            value.equalTo(max).or(disabledProperty().asReactiveValue()).asObservableValue()
        )
    }

    fun setupUndo(variableDescription: String, manager: UndoManager) = also {
        undoManager = manager
        this.variableDescription = variableDescription
    }

    fun onUserInput(handler: (newValue: Int) -> Unit) = also {
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

    fun display(value: Int) {
        if (this.value.now == value) return
        this.value.now = value
    }

    private fun increment() {
        val newValue = value.now + step
        if (newValue <= max) {
            updateValue(newValue, "Increment")
        }
    }

    private fun decrement() {
        val newValue = value.now - step
        if (newValue >= min) {
            updateValue(newValue, "Decrement")
        }
    }

    private fun updateValueFromTextInput() {
        val newValue = valueInput.text.toIntOrNull()?.coerceIn(min, max)
        if (newValue != null) {
            updateValue(newValue, "Update")
        }
        valueInput.text = value.now.toString()
        onUserInput(value.now)
    }

    private fun updateValue(newValue: Int, actionDescription: String) {
        value.now = newValue
        undoManager?.record(VariableEdit(value, value.now, newValue, "$actionDescription $variableDescription"))
    }

    companion object {
        private val incrementAction = action<IntSpinner>("Increment") {
            icon(MaterialDesignC.CHEVRON_RIGHT)
            shortcut("Up")
            executes { spinner -> spinner.increment() }
        }

        private val decrementAction = action<IntSpinner>("Decrement") {
            icon(MaterialDesignC.CHEVRON_LEFT)
            shortcut("DOWN")
            executes { spinner -> spinner.decrement() }
        }
    }
}