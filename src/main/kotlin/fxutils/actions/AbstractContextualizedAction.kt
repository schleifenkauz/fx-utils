package fxutils.actions

import fxutils.Shortcut
import fxutils.undo.ToggleEdit
import fxutils.undo.compoundEdit
import javafx.event.Event
import org.kordamp.ikonli.Ikon
import reaktive.value.*

abstract class AbstractContextualizedAction<C> : ContextualizedAction {
    abstract val context: C
    abstract override val wrapped: Action<C>

    override val shortcuts: List<Shortcut>
        get() = wrapped.shortcuts
    override val ifNotApplicable: Action.IfNotApplicable
        get() = wrapped.ifNotApplicable

    final override val icon: ReactiveValue<Ikon?> by lazy { wrapped.icon(context) }

    final override val description: ReactiveString by lazy { wrapped.description(context) }

    final override val isApplicable: ReactiveBoolean by lazy { wrapped.applicability(context) }

    final override val toggleState: ReactiveBoolean? by lazy { wrapped.toggleState(context) }

    override fun execute(ev: Event?) {
        val undoManager = wrapped.undoManager(context)
        if (undoManager != null) {
            val toggleState = toggleState
            if (toggleState !is ReactiveVariable) {
                undoManager.compoundEdit(description.now) {
                    wrapped.execute.invoke(context, ev)
                }
            } else {
                val toggleStateBefore = toggleState.now
                wrapped.execute.invoke(context, ev)
                if (toggleState.now != toggleStateBefore) {
                    undoManager.record(ToggleEdit(description.now, toggleState))
                }
            }
        } else {
            wrapped.execute.invoke(context, ev)
        }
    }

    override fun toString(): String = "ContextualizedAction [$wrapped, context = $context]"
}