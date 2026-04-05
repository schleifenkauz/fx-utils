package fxutils.controls

import fxutils.*
import fxutils.actions.Action
import fxutils.actions.ActionBar
import fxutils.actions.collectActions
import javafx.geometry.Pos
import javafx.scene.control.TextField
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import org.kordamp.ikonli.material2.Material2AL
import reaktive.value.ReactiveString
import reaktive.value.binding.not
import reaktive.value.now
import reaktive.value.reactiveVariable

abstract class EditableText(private val text: ReactiveString) : StackPane() {
    val isEditing = reactiveVariable(false)

    private val field = TextField().alwaysHGrow()
    val label = label(text).alwaysHGrow()

    val actionBar = ActionBar(actions.withContext(this), buttonStyle = "small-icon-button")

    private var isAutoSize = false

    init {
        styleClass("editable-text")
        field.text = text.now
        children.addAll(label, HBox(infiniteSpace(), actionBar))
        actionBar.visibleProperty().bind(hoverProperty())
        setAlignment(actionBar, Pos.CENTER_RIGHT)
        field.addEventFilter(KeyEvent.ANY) { ev ->
            if ("ENTER".shortcut.matches(ev)) {
                ev.consume()
                if (ev.eventType == KeyEvent.KEY_RELEASED) {
                    commitEdit()
                }
            } else if ("ESCAPE".shortcut.matches(ev)) {
                ev.consume()
                if (ev.eventType == KeyEvent.KEY_RELEASED) {
                    abandonEdit()
                }
            }
        }
        field.setOnMouseClicked { ev ->
            if (ev.clickCount >= 2 && !field.isEditable) startEdit()
        }
        field.focusedProperty().addListener { _, wasFocused, nowFocused ->
            if (wasFocused && !nowFocused) {
                abandonEdit()
            }
        }
        field.autoSize(::isAutoSize)
    }

    fun autoSize() = also { isAutoSize = true }

    override fun requestFocus() {
        field.requestFocus()
    }

    fun startEdit() {
        if (isEditing.now) return
        field.text = text.now
        children[0] = field
        isEditing.set(true)
        field.requestFocus()
        field.selectAll()
    }

    protected abstract fun isValid(text: String): Boolean

    private fun commitEdit() {
        if (!isEditing.now) return
        val value = field.text
        if (isValid(value)) {
            children[0] = label
            val old = text.now
            if (value != old) {
                updateText(value)
            }
            isEditing.set(false)
        }
    }

    protected abstract fun updateText(value: String)

    private fun abandonEdit() {
        if (!isEditing.now) return
        children[0] = label
        isEditing.set(false)
    }

    companion object {
        private val actions = collectActions<EditableText> {
            addAction("Edit name") {
                enableWhen { ctrl -> ctrl.isEditing.not() }
                ifNotApplicable(Action.IfNotApplicable.Hide)
                icon(Material2AL.EDIT)
                executes(EditableText::startEdit)
            }
            addAction("Commit edit") {
                enableWhen { ctrl -> ctrl.isEditing }
                ifNotApplicable(Action.IfNotApplicable.Hide)
                icon(Material2AL.CHECK)
                executes(EditableText::commitEdit)
            }
            addAction("Abandon edit") {
                enableWhen { ctrl -> ctrl.isEditing }
                //icon(Material2AL.CLOSE)
                executes(EditableText::abandonEdit)
            }
        }
    }
}