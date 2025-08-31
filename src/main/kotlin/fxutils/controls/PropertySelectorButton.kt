package fxutils.controls

import fxutils.escapeUnderscores
import fxutils.prompt.SelectorPrompt
import fxutils.undo.Edit
import fxutils.undo.PropertyEdit
import kotlin.reflect.KMutableProperty0

class PropertySelectorButton<E : Any>(
    private val property: KMutableProperty0<E>,
    prompt: SelectorPrompt<E>, defaultValue: E,
) : SelectorButton<E>(prompt, defaultValue) {
    init {
        update(property.get())
    }

    override fun getCurrent(): E = property.get()

    override fun update(option: E) {
        text = displayText(option).escapeUnderscores()
    }

    override fun createEdit(oldValue: E, newValue: E, actionDescription: String): Edit =
        PropertyEdit(property, oldValue, newValue, actionDescription)
}