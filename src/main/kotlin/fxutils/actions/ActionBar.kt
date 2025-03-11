package fxutils.actions

import fxutils.neverHGrow
import fxutils.styleClass
import javafx.beans.binding.Bindings
import javafx.scene.control.Button
import javafx.scene.layout.HBox

open class ActionBar(vararg buttonStyle: String) : HBox() {
    private val indices = mutableMapOf<Button, Int>()
    private val buttons = mutableMapOf<Action<*>, Button>()

    @Suppress("CanBePrimaryConstructorProperty") //cannot because it is vararg
    private val buttonStyle = buttonStyle

    constructor(actions: List<ContextualizedAction>, buttonStyle: String) : this(buttonStyle) {
        addActions(actions)
    }

    init {
        styleClass("action-bar")
        visibleProperty().bind(Bindings.isEmpty(children).not())
        neverHGrow()
    }

    fun addActions(actions: List<ContextualizedAction>) {
        for ((idx, action) in actions.withIndex()) {
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
    }

    fun getButton(action: Action<*>) = buttons.getValue(action)
}