package fxutils.controls

import fxutils.escapeUnderscores
import fxutils.prompt.SelectorPrompt
import fxutils.undo.Edit
import fxutils.undo.VariableEdit
import reaktive.value.ReactiveVariable
import reaktive.value.binding.map
import reaktive.value.fx.asObservableValue

class VariableSelectorButton<E : Any>(
    private val variable: ReactiveVariable<E>,
    prompt: SelectorPrompt<E>, defaultValue: E,
) : SelectorButton<E>(prompt, defaultValue) {
    init {
        textProperty().bind(variable.map { txt -> displayText(txt).escapeUnderscores() }.asObservableValue())
    }

    override fun getCurrent(): E = variable.get()

    override fun update(option: E) {
        variable.set(option)
    }

    override fun createEdit(oldValue: E, newValue: E, actionDescription: String): Edit =
        VariableEdit(variable, oldValue, newValue, actionDescription)
}