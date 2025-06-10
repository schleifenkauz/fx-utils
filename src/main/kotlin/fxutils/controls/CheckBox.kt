package fxutils.controls

import fxutils.setRoot
import fxutils.styleClass
import fxutils.undo.ToggleEdit
import fxutils.undo.UndoManager
import javafx.scene.control.Control
import javafx.scene.layout.StackPane
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.material2.Material2AL
import org.kordamp.ikonli.materialdesign2.MaterialDesignS
import reaktive.Observer
import reaktive.value.ReactiveVariable
import reaktive.value.forEach
import reaktive.value.now
import reaktive.value.reactiveVariable

class CheckBox(val state: ReactiveVariable<Boolean>) : Control() {
    private var undoManager: UndoManager? = null
    private var variableDescription: String? = null
    private val mark = FontIcon(Material2AL.CHECK) styleClass "check-box-mark"
    private val box = FontIcon(MaterialDesignS.SQUARE_OUTLINE) styleClass "check-box-square"

    private val valueObserver: Observer

    constructor(state: Boolean = false) : this(reactiveVariable(state))

    init {
        setRoot(StackPane(mark, box) styleClass "check-box-alt")
        valueObserver = state.forEach { v -> displayState(v) }
        setOnMouseClicked { toggle() }
    }

    private fun displayState(value: Boolean) {
        mark.isVisible = value
    }

    fun setupUndo(manager: UndoManager, variableDescription: String): CheckBox {
        undoManager = manager
        this.variableDescription = variableDescription
        return this
    }

    private fun set(value: Boolean, updateDescription: String) {
        this.state.now = value
        undoManager?.record(ToggleEdit("$updateDescription $variableDescription", this.state))
    }

    val isSelected get() = state.now

    fun set(value: Boolean) {
        set(value, "Update")
    }

    fun set() {
        set(true, "Set")
    }

    fun clear() {
        set(false, "Clear")
    }

    fun toggle() {
        set(!state.now, "Toggle")
    }
}