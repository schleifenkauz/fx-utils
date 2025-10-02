package fxutils.controls

import fxutils.prompt.SelectorPrompt
import fxutils.undo.Edit
import fxutils.undo.UndoManager
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.input.MouseButton
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.materialdesign2.MaterialDesignC

abstract class SelectorButton<E : Any>(
    protected val prompt: SelectorPrompt<E>,
    private val defaultValue: E,
) : Button() {
    private var undoManager: UndoManager? = null
    private var actionDescription: String? = null

    protected var displayText: (E) -> String = prompt::displayText
        private set

    private var onUpdate: (E, E) -> Unit = { _, _ -> }

    init {
        styleClass.add("selector-button")
        graphic = FontIcon(MaterialDesignC.CHEVRON_DOWN)
        contentDisplay = ContentDisplay.RIGHT
        showPopupOnClick()
    }

    fun withUndo(manager: UndoManager?, action: String?): SelectorButton<E> = also {
        if (manager != null) {
            undoManager = manager
            actionDescription = action
        }
    }

    fun onUpdate(block: (E, E) -> Unit): SelectorButton<E> = also { onUpdate = block }

    fun displayText(display: (E) -> String): SelectorButton<E> = also { displayText = display }

    protected abstract fun getCurrent(): E

    protected abstract fun update(option: E)

    protected abstract fun createEdit(oldValue: E, newValue: E, actionDescription: String): Edit

    private fun onSelect(option: E) {
        if (getCurrent() != option) {
            val oldValue = getCurrent()
            update(option)
            undoManager?.record(createEdit(oldValue, option, actionDescription ?: prompt.title))
            onUpdate(oldValue, option)
        }
    }

    private fun showPopupOnClick() {
        setOnMouseClicked { ev ->
            when (ev.button) {
                MouseButton.PRIMARY -> {
                    val result = prompt.showPopup(anchorNode = this, initialOption = getCurrent())
                    if (result != null) onSelect(result)
                }

                MouseButton.SECONDARY -> onSelect(defaultValue)

                else -> {}
            }
        }
    }
}