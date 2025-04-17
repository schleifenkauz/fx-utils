package fxutils.actions

import fxutils.neverHGrow
import fxutils.styleClass
import javafx.beans.binding.Bindings
import javafx.scene.control.Button
import javafx.scene.layout.HBox

open class ActionBar private constructor(
    private val actions: MutableList<ContextualizedAction>,
    vararg buttonStyle: String,
) : HBox() {
    private val indices = mutableMapOf<Button, Int>()
    private val buttons = mutableMapOf<Action<*>, Button>()

    @Suppress("CanBePrimaryConstructorProperty") //cannot because it is vararg
    private val buttonStyle = buttonStyle

    constructor(actions: List<ContextualizedAction>, buttonStyle: String) : this(actions.toMutableList(), buttonStyle)
    constructor(vararg buttonStyle: String) : this(mutableListOf(), *buttonStyle)

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
        val button = action.makeButton(*buttonStyle)
        indices[button] = idx
        buttons[action.wrapped] = button
        if (button.isVisible) children.add(button)
        button.visibleProperty().addListener { _, _, visible ->
            if (visible) {
                var index = children.binarySearchBy(idx) { btn -> indices[btn] }
                if (index >= 0) return@addListener
                index = -(index + 1)
                children.add(index, button)
            } else children.remove(button)
        }
    }

    fun getButton(action: Action<*>) = buttons.getValue(action)
}