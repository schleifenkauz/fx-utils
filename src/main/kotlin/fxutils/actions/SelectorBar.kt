package fxutils.actions

import reaktive.value.now
import reaktive.value.reactiveVariable

class SelectorBar<T : SelectorBar.Option<C, T>, C>(
    val context: C,
    options: List<T>,
    initial: T,
    buttonStyle: String
) : ActionBar(buttonStyle) {
    private val _selected = reactiveVariable(initial)
    val selectedOption get() = _selected
    val selected get() = _selected.now

    init {
        addActions(options.map { opt -> opt.action.withContext(this) })
    }

    fun select(option: T) {
        _selected.now = option
    }

    override fun toString(): String = "${this::class.simpleName} [ selected = $selected ]"

    interface Option<C, T : Option<C, T>> {
        val action: Action<SelectorBar<T, C>>
    }
}