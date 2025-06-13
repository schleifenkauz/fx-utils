package fxutils.actions

import fxutils.neverHGrow
import fxutils.setPseudoClassState
import fxutils.styleClass
import javafx.application.Platform
import javafx.event.Event
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

fun Event?.isShiftDown() = (this is KeyEvent && isShiftDown) || (this is MouseEvent && isShiftDown)
fun Event?.isAltDown() = (this is KeyEvent && isAltDown) || (this is MouseEvent && isAltDown)
fun Event?.isControlDown() = (this is KeyEvent && isControlDown) || (this is MouseEvent && isControlDown)
val Event?.isTargetTextInput
    get() = this is KeyEvent && (target is TextInputControl || target is Spinner<*> || target is TextArea)

internal fun buttonSize(style: String) = when (style) {
    "small-icon-button" -> 16.0
    "medium-icon-button" -> 24.0
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
    button.tooltip = Tooltip().also { tooltip ->
        val shortcutInfo = this.shortcuts
            .firstOrNull()
            ?.let { shortcut -> " ($shortcut)" }
            .orEmpty()
        tooltip.userData = this.description.forEach { desc ->
            Platform.runLater {
                tooltip.text = "$desc $shortcutInfo"
            }
        }
    }
    val size = buttonSize(style)
    button.setMinSize(size, size)
    button.styleClass("icon-button", style)
    button.setOnMouseClicked { ev -> this.execute(ev) }
    return button
}

fun Action<Unit>.makeButton(style: String) = withContext(Unit).makeButton(style)