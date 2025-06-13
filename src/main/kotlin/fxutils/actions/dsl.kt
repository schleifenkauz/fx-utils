package fxutils.actions

import fxutils.KeyEventHandlerBody
import fxutils.asPopup
import fxutils.prompt.DetailPane
import fxutils.registerShortcuts
import fxutils.show
import javafx.scene.Node
import javafx.scene.paint.Color
import org.kordamp.ikonli.materialdesign2.MaterialDesignD
import reaktive.value.ReactiveBoolean
import reaktive.value.now
import reaktive.value.reactiveValue

fun <C : Any> collectActions(body: Action.Collector<C>.() -> Unit): Action.Collector<C> = Action.Collector<C>().apply(body)

fun KeyEventHandlerBody<*>.registerActions(actions: List<ContextualizedAction>) {
    for (action in actions) {
        for (shortcut in action.shortcuts) {
            on(shortcut) { ev ->
                if (action.isApplicable.now) {
                    action.execute(ev)
                }
            }
        }
    }
}

fun Node.registerShortcuts(actions: List<ContextualizedAction>) {
    registerShortcuts {
        registerActions(actions)
    }
}

fun <C> action(name: String, config: Action.Builder<C>.() -> Unit) = Action.Builder<C>(name).apply(config).build()

fun <C, T : SelectorBar.Option<C, T>> Action.Builder<SelectorBar<T, C>>.selects(value: T) {
    selects(value) { bar -> bar.selectedOption }
}

fun <C> detailsAction(
    name: String = "Details", applicability: (C) -> ReactiveBoolean = { _ -> reactiveValue(true) },
    labelWidth: Double = DetailPane.LABEL_WIDTH, sceneFill: Color = Color.TRANSPARENT,
    setupDetailsPane: DetailPane.(C) -> Unit,
): Action<C> = action(name) {
    icon(MaterialDesignD.DOTS_VERTICAL)
    enableWhen(applicability)
    ifNotApplicable(Action.IfNotApplicable.Hide)
    executes { ctx, ev ->
        val detailPane = DetailPane(labelWidth).apply { setupDetailsPane(ctx) }
        val popup = detailPane.asPopup()
        popup.scene.fill = sceneFill
        popup.show(ev)
    }
}