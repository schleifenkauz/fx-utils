package fxutils.actions

import fxutils.*
import javafx.application.Platform
import javafx.event.Event
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.javafx.FontIcon
import reaktive.value.binding.and
import reaktive.value.binding.not
import reaktive.value.binding.notEqualTo
import reaktive.value.forEach
import reaktive.value.fx.asObservableValue
import reaktive.value.now

fun <C> collectActions(body: Action.Collector<C>.() -> Unit): Action.Collector<C> = Action.Collector<C>().apply(body)

fun KeyEventHandlerBody<*>.registerActions(actions: List<ContextualizedAction>) {
    for (action in actions) {
        for (shortcut in action.wrapped.shortcuts) {
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

fun Event?.isShiftDown() = (this is KeyEvent && isShiftDown) || (this is MouseEvent && isShiftDown)

fun Event?.isAltDown() = (this is KeyEvent && isAltDown) || (this is MouseEvent && isAltDown)

fun Event?.isControlDown() = (this is KeyEvent && isControlDown) || (this is MouseEvent && isControlDown)

fun <C> action(name: String, config: Action.Builder<C>.() -> Unit) = Action.Builder<C>(name).apply(config).build()

fun <C, T : SelectorBar.Option<C, T>> Action.Builder<SelectorBar<T, C>>.selects(value: T) {
    selects(value) { bar -> bar.selectedOption }
}

val Event?.isTargetTextInput
    get() = this is KeyEvent && (target is TextInputControl || target is Spinner<*> || target is TextArea)

const val DEFAULT_RADIUS: Double = 16.0

fun ContextualizedAction.makeButton(vararg style: String): Button {
    val button = Button()
    val iconObserver = this.icon.forEach { icon ->
        Platform.runLater {
            button.graphic = icon?.let(::FontIcon)
        }
    }
    val toggleState = this.toggleState
    button.userData = if (toggleState != null) {
        val toggleStateObserver = toggleState.forEach { active ->
            Platform.runLater { button.setPseudoClassState("selected", active) }
        }
        iconObserver and toggleStateObserver
    } else iconObserver
    val iconAvailable = this.icon.notEqualTo(null)
    val applicable = this.isApplicable
    if (this.wrapped.ifNotApplicable == Action.IfNotApplicable.Disable) {
        button.visibleProperty().bind(iconAvailable.asObservableValue())
        button.disableProperty().bind(applicable.not().asObservableValue())
    } else {
        button.visibleProperty().bind(iconAvailable.and(applicable).asObservableValue())
    }
    button.tooltip = Tooltip().also { tooltip ->
        val shortcutInfo = this.wrapped.shortcuts
            .firstOrNull()
            ?.let { shortcut -> " ($shortcut)" }
            .orEmpty()
        tooltip.userData = this.description.forEach { desc ->
            Platform.runLater {
                tooltip.text = "$desc $shortcutInfo"
            }
        }
    }
    button.setMinSize(DEFAULT_RADIUS * 2, DEFAULT_RADIUS * 2)
    button.setMaxSize(DEFAULT_RADIUS * 2, DEFAULT_RADIUS * 2)
    button.neverHGrow()
    button.styleClass.addAll(style.toList() + "icon-button")
    button.setOnMouseClicked { ev -> this.execute(ev) }
    return button
}

private fun ButtonBase.makeIconButton(ikon: Ikon, description: String) {
    styleClass("icon-button")
    graphic = FontIcon(ikon)
    tooltip = Tooltip(description)
    neverHGrow()
}

fun Ikon.button(action: String, execute: (MouseEvent) -> Unit = {}): Button {
    val button = Button()
    button.makeIconButton(this, action)
    button.setOnMouseClicked { ev -> execute(ev) }
    return button
}