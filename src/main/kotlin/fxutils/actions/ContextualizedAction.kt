package fxutils.actions

import fxutils.Shortcut
import javafx.event.Event
import org.kordamp.ikonli.Ikon
import reaktive.value.ReactiveBoolean
import reaktive.value.ReactiveString
import reaktive.value.ReactiveValue

interface ContextualizedAction {
    val wrapped: Action<*>? get() = null

    val shortcuts: List<Shortcut>

    val description: ReactiveString

    val isApplicable: ReactiveBoolean

    val ifNotApplicable: Action.IfNotApplicable

    val toggleState: ReactiveBoolean?

    val icon: ReactiveValue<Ikon?>

    fun execute(ev: Event?)
}