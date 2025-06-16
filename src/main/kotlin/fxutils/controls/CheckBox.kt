package fxutils.controls

import fxutils.label
import fxutils.setRoot
import fxutils.styleClass
import fxutils.undo.ToggleEdit
import fxutils.undo.UndoManager
import javafx.scene.control.Control
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.material2.Material2AL
import org.kordamp.ikonli.materialdesign2.MaterialDesignS
import reaktive.Observer
import reaktive.value.*
import reaktive.value.binding.map
import reaktive.value.fx.asObservableValue

class CheckBox(
    val state: ReactiveVariable<Boolean>,
    val text: ReactiveString = reactiveValue(""),
) : Control() {
    private var undoManager: UndoManager? = null
    private var variableDescription: String? = null
    private val mark = FontIcon(Material2AL.CHECK) styleClass "check-box-mark"
    private val box = FontIcon(MaterialDesignS.SQUARE_OUTLINE) styleClass "check-box-square"

    private val valueObserver: Observer

    constructor(state: ReactiveVariable<Boolean>, text: String) : this(state, reactiveValue(text))

    constructor(state: Boolean = false, text: String = "") : this(reactiveVariable(state), reactiveValue(text))

    constructor(state: Boolean = false, text: ReactiveString) : this(reactiveVariable(state), text)

    init {
        val label = label(text)
        label.visibleProperty().bind(text.map { t -> t.isNotEmpty() }.asObservableValue())
        label.managedProperty().bind(label.visibleProperty())
        val boxPane = StackPane(mark, box) styleClass "check-box-alt"
        val layout = HBox(label, boxPane)
        setRoot(layout)
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