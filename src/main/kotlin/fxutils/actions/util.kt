package fxutils.actions

import fxutils.*
import javafx.application.Platform
import javafx.event.Event
import javafx.scene.control.*
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign2.MaterialDesignA
import reaktive.value.ReactiveString
import reaktive.value.binding.and
import reaktive.value.binding.impl.notNull
import reaktive.value.binding.map
import reaktive.value.binding.not
import reaktive.value.binding.notEqualTo
import reaktive.value.forEach
import reaktive.value.fx.asObservableValue

fun Event?.isShiftDown() = (this is KeyEvent && isShiftDown) || (this is MouseEvent && isShiftDown)
fun Event?.isAltDown() = (this is KeyEvent && isAltDown) || (this is MouseEvent && isAltDown)
fun Event?.isControlDown() = (this is KeyEvent && isControlDown) || (this is MouseEvent && isControlDown)
val Event?.isTargetTextInput
    get() = this is KeyEvent && (target is TextInputControl || target is Spinner<*> || target is TextArea)

internal fun buttonSize(style: String) = when (style) {
    "small-icon-button" -> 16.0
    "medium-icon-button", "mute-solo-button" -> 24.0
    "large-icon-button" -> 32.0
    else -> throw AssertionError("Unknown icon button style: $style")
}

private fun ButtonBase.makeIconButton(ikon: Ikon, description: String, style: String) {
    styleClass("icon-button", style)
    graphic = FontIcon(ikon)
    tooltip = Tooltip(description)
    neverHGrow()
}

fun Ikon.button(action: String, style: String, execute: (MouseEvent) -> Unit = {}): Button {
    val button = Button()
    button.makeIconButton(this, action, style)
    button.setOnMouseClicked { ev -> execute(ev) }
    return button
}

fun ContextualizedAction.makeTextButton(style: String): Button {
    val button = button(style = style)
    val toggleState = this.toggleState
    if (toggleState != null) {
        button.userData = toggleState.forEach { active ->
            Platform.runLater { button.setPseudoClassState("selected", active) }
        }
    }
    button.textProperty().bind(description.asObservableValue())
    if (shortcuts.isNotEmpty()) {
        button.tooltip = Tooltip(shortcuts.first().toString())
    }
    button.setOnMouseClicked { ev ->
        execute(ev)
    }
    button.disableProperty().bind(isApplicable.not().asObservableValue())
    return button
}

fun ContextualizedAction.makeButton(style: String): Button {
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
    if (this.ifNotApplicable == Action.IfNotApplicable.Disable) {
        button.visibleProperty().bind(iconAvailable.asObservableValue())
        button.disableProperty().bind(applicable.not().asObservableValue())
    } else {
        button.visibleProperty().bind(iconAvailable.and(applicable).asObservableValue())
    }
    button.tooltip = Tooltip()
    button.tooltip.textProperty().bind(actionText().asObservableValue())
    val size = buttonSize(style)
    button.setMinSize(size, size)
    button.styleClass("icon-button", style)
    button.setOnMouseClicked { ev -> this.execute(ev) }
    return button
}

fun contextMenu(actions: List<ContextualizedAction>): ContextMenu {
    val menu = ContextMenu()
    for (action in actions) {
        val descriptionLabel = label(action.description) styleClass "menu-item-description"
        val shortcutLabel = label(action.shortcuts.firstOrNull()?.toString().orEmpty())
        shortcutLabel.styleClass("shortcut-info")
        val icon = FontIcon()
        icon.iconCodeProperty().bind(action.icon.map { it ?: MaterialDesignA.ARROW_RIGHT }.asObservableValue())
        icon.visibleProperty().bind(action.icon.notNull().asObservableValue())
        val layout = HBox(icon, descriptionLabel, hspace(25.0).alwaysHGrow(), shortcutLabel)
            .styleClass("menu-item-layout")
        val item = CustomMenuItem(layout)
        item.disableProperty().bind(action.isApplicable.not().asObservableValue())
        item.setOnAction { ev -> action.execute(ev) }
        menu.items.add(item)
    }
//    menu.scene.stylesheets.addAll(SubWindow.globalStylesheets)
    return menu
}

private fun ContextualizedAction.actionText(): ReactiveString {
    val shortcutInfo = this.shortcuts
        .firstOrNull()?.let { shortcut -> "($shortcut)" }
        .orEmpty()
    return description.map { desc -> "$desc $shortcutInfo" }
}

fun Action<Unit>.makeButton(style: String) = withContext(Unit).makeButton(style)