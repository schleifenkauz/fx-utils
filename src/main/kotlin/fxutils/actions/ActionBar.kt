package fxutils.actions

import fxutils.neverHGrow
import fxutils.reorient
import fxutils.setRoot
import fxutils.styleClass
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane

open class ActionBar private constructor(
    private val actions: MutableList<ContextualizedAction>,
    private val buttonStyle: String,
) : Control() {
    private var layout: Pane = HBox() styleClass "action-bar"
        private set(value) {
            field = value
            setRoot(value)
        }

    private val buttons = mutableMapOf<ContextualizedAction, Button>()

    constructor(
        actions: Collection<ContextualizedAction>, buttonStyle: String,
    ) : this(actions.toMutableList(), buttonStyle)

    constructor(buttonStyle: String) : this(mutableListOf(), buttonStyle)

    init {
        styleClass("action-bar")
        setRoot(layout)
        neverHGrow()
        for (action in actions) {
            addAction(action)
        }
    }

    fun actions(): List<ContextualizedAction> = actions.toList()

    fun addActions(actions: List<ContextualizedAction>) {
        this.actions.addAll(actions)
        for (action in actions) {
            addAction(action)
        }
    }

    private fun addAction(action: ContextualizedAction) {
        val button = action.makeButton(buttonStyle)
        buttons[action] = button
        layout.children.add(button)
        button.managedProperty().bind(button.visibleProperty())
    }

    fun setOrientation(orientation: Orientation) {
        layout = layout.reorient(orientation)
    }

    fun getButton(action: Action<*>): Button {
        val contextualized = actions.find { it.wrapped == action } ?: error("Action $action not found in action bar")
        return buttons.getValue(contextualized)
    }

    fun floating(pos: Pos): ActionBar {
        maxWidth = prefWidth(-1.0)
        maxHeight = prefHeight(-1.0)
        padding = Insets(5.0)
        StackPane.setAlignment(this, pos)
        return this
    }
}