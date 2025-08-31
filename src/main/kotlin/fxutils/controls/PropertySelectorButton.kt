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
        text = displayText(property.get()).escapeUnderscores()
    }

    override fun getCurrent(): E = property.get()

    public override fun update(option: E) {
        property.set(option)
        text = displayText(property.get()).escapeUnderscores()
    }

    override fun createEdit(oldValue: E, newValue: E, actionDescription: String): Edit =
        PropertyEdit(property, oldValue, newValue, actionDescription)
}