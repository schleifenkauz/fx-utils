package fxutils.actions

import fxutils.neverHGrow
import fxutils.styleClass
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane

open class ActionBar private constructor(
    private val actions: MutableList<ContextualizedAction>,
    private val buttonStyle: String,
) : HBox() {
    private val indices = mutableMapOf<Button, Int>()
    private val buttons = mutableMapOf<Action<*>, Button>()

    constructor(
        actions: Collection<ContextualizedAction>, buttonStyle: String,
    ) : this(actions.toMutableList(), buttonStyle)

    constructor(buttonStyle: String) : this(mutableListOf(), buttonStyle)

    init {
        styleClass("action-bar")
        visibleProperty().bind(Bindings.isEmpty(children).not())
        neverHGrow()
        for ((index, action) in actions.withIndex()) {
            addAction(action, index)
        }

    }

    fun actions(): List<ContextualizedAction> = actions.toList()

    fun addActions(actions: List<ContextualizedAction>) {
        val nActionsBefore = this.actions.size
        for ((i, action) in actions.withIndex()) {
            val index = nActionsBefore + i
            this.actions.add(action)
            addAction(action, index)
        }
    }

    private fun addAction(action: ContextualizedAction, idx: Int) {
        val button = action.makeButton(buttonStyle)
        indices[button] = idx
        buttons[action.wrapped] = button
        if (button.isVisible) children.add(button)
        button.visibleProperty().addListener { _, _, visible ->
            Platform.runLater {
                if (visible) {
                    var index = children.binarySearchBy(idx) { btn -> indices[btn] }
                    if (index >= 0) return@runLater
                    index = -(index + 1)
                    children.add(index, button)
                } else children.remove(button)
            }
        }
    }

    fun getButton(action: Action<*>) = buttons.getValue(action)

    fun floating(pos: Pos): ActionBar {
        maxWidth = prefWidth(-1.0)
        maxHeight = prefHeight(-1.0)
        padding = Insets(5.0)
        StackPane.setAlignment(this, pos)
        return this
    }
}