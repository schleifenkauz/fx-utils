package fxutils.controls

import fxutils.actions.action
import fxutils.actions.makeButton
import fxutils.actions.registerShortcuts
import fxutils.prompt.SelectorPrompt
import fxutils.setRoot
import fxutils.styleClass
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import org.kordamp.ikonli.materialdesign2.MaterialDesignC
import reaktive.Observer
import reaktive.value.ReactiveVariable
import reaktive.value.binding.equalTo
import reaktive.value.forEach
import reaktive.value.fx.asObservableValue
import reaktive.value.now
import reaktive.value.reactiveVariable

class OptionSpinner<E : Any>(
    private val selectedOption: ReactiveVariable<E>,
    private val options: List<E>,
    private val toString: (E) -> String = { it.toString() },
    private val selectorPrompt: SelectorPrompt<E>? = null,
) : Control() {
    private val selectedIndex = reactiveVariable(options.indexOf(selectedOption.now))

    private val optionObserver: Observer

    val btnDecrement = decrementAction.withContext(this).makeButton("small-icon-button")
    val btnIncrement = incrementAction.withContext(this).makeButton("small-icon-button")
    val label = Label() styleClass "option-spinner-label"

    init {
        require(selectedIndex.now != -1) { "Initial option not found in options" }
        btnDecrement.disableProperty().bind(selectedIndex.equalTo(0).asObservableValue())
        btnIncrement.disableProperty().bind(selectedIndex.equalTo(options.lastIndex).asObservableValue())
        setRoot(HBox(btnDecrement, label, btnIncrement) styleClass "option-spinner")
        label.registerShortcuts(listOf(decrementAction.withContext(this), incrementAction.withContext(this)))
        optionObserver = selectedOption.forEach { v ->
            label.text = toString(v)
        }
        HBox.setHgrow(label, Priority.ALWAYS)
        label.setOnMouseClicked { ev ->
            when (ev.button) {
                MouseButton.PRIMARY -> label.requestFocus()
                MouseButton.SECONDARY -> {
                    val option = selectorPrompt?.showPopup(ev) ?: return@setOnMouseClicked
                    select(option)
                }
                else -> {}
            }
        }
    }

    fun select(option: E) {
        val idx = options.indexOf(option)
        require(idx != -1) { "Option $option not found in options" }
        selectIndex(idx)
    }

    fun selectIndex(idx: Int) {
        selectedIndex.now = idx
        selectedOption.now = options[idx]
    }

    private fun increment() {
        if (selectedIndex.now == options.lastIndex) return
        selectIndex(selectedIndex.now + 1)
    }

    private fun decrement() {
        if (selectedIndex.now == 0) return
        selectIndex(selectedIndex.now - 1)
    }

    companion object {
        private val decrementAction = action<OptionSpinner<*>>("Decrement") {
            icon(MaterialDesignC.CHEVRON_LEFT)
            shortcut("LEFT")
            executes { spinner -> spinner.decrement() }
        }

        private val incrementAction = action<OptionSpinner<*>>("Increment") {
            icon(MaterialDesignC.CHEVRON_RIGHT)
            shortcut("RIGHT")
            executes { spinner -> spinner.increment() }
        }
    }
}