package fxutils.actions

import fxutils.Shortcut
import fxutils.shortcut
import fxutils.undo.UndoManager
import javafx.event.Event
import org.kordamp.ikonli.Ikon
import reaktive.value.*
import reaktive.value.binding.equalTo
import reaktive.value.binding.map

class Action<in C> private constructor(
    val name: String,
    val category: Category,
    val description: (C) -> ReactiveString,
    val shortcuts: List<Shortcut>, val icon: (C) -> ReactiveValue<Ikon?>,
    val applicability: (C) -> ReactiveBoolean,
    val ifNotApplicable: IfNotApplicable,
    val toggleState: (C) -> ReactiveValue<Boolean>?,
    val execute: (C, Event?) -> Unit,
    val undoManager: (C) -> UndoManager? = { null },
) {
    fun withContext(context: C): ContextualizedAction = Contextualized(this, context)

    fun <D> map(name: String = this.name, f: (D) -> C?) =
        Action<D>(
            name, category,
            description = { ctx -> f(ctx)?.let(description) ?: reactiveValue("<not applicable>") },
            shortcuts,
            icon = { ctx -> f(ctx)?.let(icon) ?: reactiveValue(null) },
            applicability = { c -> f(c)?.let(applicability) ?: reactiveValue(false) }, ifNotApplicable,
            toggleState = { c -> f(c)?.let(toggleState) ?: reactiveVariable(false) }, { c, ev ->
                val target = f(c)
                if (target != null) execute(target, ev)
                else System.err.println("Action $name is not applicable on $c")
            },
            undoManager = { ctx -> f(ctx)?.let(undoManager) }
        )

    override fun toString(): String = "Action #$name"

    enum class Category {
        Unknown, File, Edit, View;
    }

    class Builder<C>(
        val name: String,
        var category: Category = Category.Unknown,
        private var description: (C) -> ReactiveString = { _ -> reactiveValue(name) },
        private val shortcuts: MutableList<Shortcut> = mutableListOf(),
        private var icon: (C) -> ReactiveValue<Ikon?> = { reactiveValue(null) },
        private var applicability: (C) -> ReactiveBoolean = { reactiveValue(true) },
        private var ifNotApplicable: IfNotApplicable = IfNotApplicable.Hide,
        private var toggleState: (C) -> ReactiveValue<Boolean>? = { null },
        private var execute: (C, ev: Event?) -> Unit = { _, _ -> },
        private var undoManager: (C) -> UndoManager? = { null },
    ) {
        fun description(desc: String) {
            description = { _ -> reactiveValue(desc) }
        }

        fun description(description: (C) -> ReactiveString) {
            this.description = description
        }

        fun shortcut(literal: String) {
            shortcuts.add(literal.shortcut)
        }

        fun shortcuts(vararg literals: String) {
            for (literal in literals) {
                shortcut(literal)
            }
        }

        fun icon(ikon: Ikon) {
            icon = { reactiveValue(ikon) }
        }

        fun icon(react: (C) -> ReactiveValue<Ikon?>) {
            icon = react
        }

        fun executes(body: (C, ev: Event?) -> Unit) {
            execute = body
        }

        fun executes(body: (C) -> Unit) {
            execute = { ctx, _ -> body(ctx) }
        }

        fun executesFirst(body: (C, ev: Event?) -> Unit) {
            val after = execute
            execute = { ctx, ev ->
                body.invoke(ctx, ev)
                after.invoke(ctx, ev)
            }
        }

        inline fun <reified T : C> executesOn(crossinline action: (obj: T, ev: Event?) -> Unit) {
            applicableIf { obj -> obj is T }
            executes { obj, ev -> action(obj as T, ev) }
        }

        inline fun <reified T : C> executesOn(crossinline action: (obj: T) -> Unit) {
            applicableIf { obj -> obj is T }
            executes { obj -> action(obj as T) }
        }

        fun toggleState(state: (C) -> ReactiveValue<Boolean>) {
            toggleState = state
        }

        fun toggles(
            variable: (C) -> ReactiveVariable<Boolean>,
            toggle: (ev: Event?, ctx: C, now: Boolean) -> Boolean = { _, _, now -> !now },
        ) {
            toggleState(variable)
            executes { ctx, ev ->
                val v = variable(ctx)
                v.now = toggle(ev, ctx, v.now)
            }
        }

        fun toggles(
            variable: (C) -> ReactiveVariable<Boolean>,
            toggle: (ev: Event?, ctx: C, now: Boolean) -> Boolean = { _, _, now -> !now },
            whenTrue: Ikon, whenFalse: Ikon,
        ) {
            toggles(variable, toggle)
            icon { ctx -> variable(ctx).map { now -> if (now) whenTrue else whenFalse } }
        }

        fun <T> selects(value: T, variable: (C) -> ReactiveVariable<T>) {
            toggleState = { ctx -> variable(ctx).equalTo(value) }
            executes { ctx ->
                val v = variable(ctx)
                v.set(value)
            }
        }

        fun enableWhen(predicate: (C) -> ReactiveBoolean) {
            applicability = predicate
            ifNotApplicable = IfNotApplicable.Disable
        }

        fun enableWhen(predicate: (ReactiveBoolean, C) -> ReactiveBoolean) {
            enableWhen { ctx -> predicate(applicability(ctx), ctx) }
        }

        fun applicableIf(predicate: (C) -> Boolean) {
            applicability = { ctx -> reactiveValue(predicate(ctx)) }
            ifNotApplicable = IfNotApplicable.Hide
        }

        fun ifNotApplicable(consequence: IfNotApplicable) {
            ifNotApplicable = consequence
        }

        fun undoable(manager: (C) -> UndoManager?) {
            undoManager = manager
        }

        fun <D> buildFrom(action: Action<D>, f: (C) -> D) {
            description { c -> action.description(f(c)) }
            category = action.category
            icon { c -> action.icon(f(c)) }
            shortcuts.addAll(action.shortcuts)
            enableWhen { c -> action.applicability(f(c)) }
            toggleState = { c -> action.toggleState(f(c)) }
            executes { c, ev -> action.execute(f(c), ev) }
            ifNotApplicable = action.ifNotApplicable
            undoManager = { c -> action.undoManager(f(c)) }
        }

        fun build(): Action<C> = Action(
            name, category, description,
            shortcuts, icon,
            applicability, ifNotApplicable, toggleState, execute,
            undoManager
        )

        companion object {
            fun <C: Any> from(action: Action<C>) = Builder(
                action.name, category = action.category,
                action.description, action.shortcuts.toMutableList(),
                action.icon, action.applicability, action.ifNotApplicable,
                action.toggleState, action.execute, action.undoManager
            )
        }
    }

    open class Collector<C : Any>() {
        @PublishedApi internal val actions = mutableListOf<Action<C>>()

        var category: Category = Category.Unknown

        constructor(collect: Collector<C>.() -> Unit) : this() {
            collect()
        }

        fun add(action: Action<C>) {
            actions.add(action)
        }

        fun add(action: Action<C>, configure: Builder<C>.() -> Unit) {
            val builder = Builder.from(action)
            builder.configure()
            add(builder.build())
        }

        fun addAll(collector: Collector<C>) {
            actions.addAll(collector.actions)
        }

        @JvmName("addAllConfigured")
        inline fun addAll(collector: Collector<C>, configure: Builder<C>.() -> Unit) {
            for (action in collector.actions) {
                val builder = Builder.from(action)
                builder.configure()
                add(builder.build())
            }
        }

        fun <D : Any> addAll(collector: Collector<D>, f: (C) -> D?) {
            for (action in collector.actions) {
                add(action.map(action.name, f))
            }
        }

        fun <D : Any> add(action: Action<D>, name: String = action.name, f: (C) -> D?) {
            add(action.map(name, f))
        }

        inline fun addAction(name: String, configure: Builder<C>.() -> Unit) {
            val builder = Builder<C>(name, category = category)
            builder.configure()
            add(builder.build())
        }

        fun getAction(name: String): Action<C> =
            actions.find { a -> a.name == name } ?: error("No action with name $name")

        fun withContext(context: C): List<ContextualizedAction> =
            actions.map { action -> Contextualized(action, context) }

        fun <D : Any> map(f: (D) -> C?): Collector<D> {
            val collector = Collector<D>()
            collector.addAll(this, f)
            return collector
        }

        fun <D : Any> withContext(context: D, f: (D) -> C?): List<ContextualizedAction> =
            map(f).withContext(context)
    }

    private class Contextualized<C>(
        override val wrapped: Action<C>,
        override val context: C,
    ) : AbstractContextualizedAction<C>()

    enum class IfNotApplicable {
        Hide, Disable;
    }
}