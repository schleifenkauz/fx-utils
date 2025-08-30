package fxutils.prompt

import fxutils.PseudoClasses.SELECTED
import fxutils.controls.PropertySelectorButton
import fxutils.controls.VariableSelectorButton
import fxutils.shortcut
import fxutils.styleClass
import fxutils.undo.UndoManager
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Window
import org.controlsfx.control.textfield.CustomTextField
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.material2.Material2MZ
import reaktive.value.ReactiveVariable
import kotlin.reflect.KMutableProperty0

abstract class SelectorPrompt<E : Any>(public override val title: String) : Prompt<E?>() {
    private val searchText = CustomTextField().styleClass("sleek-text-field", "search-field")
    private val layout = VBox().styleClass("options-box")
    private val scrollPane = ScrollPane(layout)
    final override val content: VBox = VBox(searchText, scrollPane) styleClass "selector-prompt"

    private val optionBoxes = mutableMapOf<E, Region>()
    private var filteredOptions: List<E> = emptyList()
    var selectedOption: E? = null
        private set

    private var initialOption: E? = null //TODO is this needed

    private var filter: (E) -> Boolean = { true }

    private var result: E? = null

    protected abstract fun options(): List<E>

    fun withMaxHeight(height: Double) = also { scrollPane.maxHeight = height }

    init {
        setupSearchField()
        scrollPane.maxHeight = 400.0
        scrollPane.isFitToWidth = true
        scrollPane.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        registerShortcuts()
    }

    private fun setupSearchField() {
        searchText.promptText = "$title..."
        searchText.left = FontIcon(Material2MZ.SEARCH)
        searchText.textProperty().addListener { _ ->
            refilterOptions()
        }
    }

    fun setFilter(predicate: (E) -> Boolean): SelectorPrompt<E> {
        filter = predicate
        return this
    }

    fun addFilter(predicate: (E) -> Boolean): SelectorPrompt<E> {
        val oldFilter = filter
        filter = { e -> oldFilter(e) && predicate(e) }
        return this
    }

    fun removeOptions(options: List<E>): SelectorPrompt<E> {
        addFilter { option -> option !in options }
        return this
    }

    fun selectInitialOption(option: E?): SelectorPrompt<E> {
        initialOption = option
        return this
    }

    protected fun getBox(option: E) = optionBoxes[option]

    protected open fun makeOption(text: String): E? = null

    protected open fun createCell(option: E): Region = HBox(Label(displayText(option)).styleClass("option-label"))

    protected abstract fun extractText(option: E): String

    open fun displayText(option: E): String = extractText(option)

    override fun getDefault(): E? = null //TODO really? not initialOption?

    override fun onReceiveFocus() {
        searchText.requestFocus()
        searchText.selectAll()
    }

    override fun createLayout(): Parent = content

    private fun prepareOptionBoxes() {
        optionBoxes.clear()
        for (option in options()) {
            val box = createCell(option).styleClass("option-cell")
            box.setOnMouseClicked {
                commit(option)
            }
            optionBoxes[option] = box
        }
    }

    private fun refilterOptions() {
        prepareOptionBoxes()
        layout.children.clear()
        filteredOptions = options().filter { option ->
            extractText(option).contains(searchText.text, ignoreCase = true) && filter(option)
        }
        select(filteredOptions.firstOrNull())
        for (option in filteredOptions) {
            val box = optionBoxes.getValue(option)
            layout.children.add(box)
        }
        if (_window != null) window.sizeToScene()
    }

    private fun registerShortcuts() {
        content.addEventFilter(KeyEvent.KEY_PRESSED) { ev ->
            when {
                "Shift?+TAB".shortcut.matches(ev) -> {
                    val deltaIdx = if (ev.isShiftDown) -1 else +1
                    val selectedIndex = filteredOptions.indexOf(selectedOption)
                    if (selectedIndex + deltaIdx in filteredOptions.indices) {
                        select(filteredOptions[selectedIndex + deltaIdx])
                    }
                    ev.consume()
                }

                "Enter".shortcut.matches(ev) -> {
                    if (selectedOption != null) {
                        commit(selectedOption!!)
                    }
                    else confirmText(searchText.text)
                    ev.consume()
                }

                "Ctrl+Enter".shortcut.matches(ev) -> {
                    val text = searchText.text.takeIf { it.isNotBlank() }
                        ?: selectedOption?.let { extractText(it) } ?: return@addEventFilter
                    confirmText(text)
                    ev.consume()
                }
            }
        }
    }

    fun select(option: E?) {
        optionBoxes[selectedOption]?.pseudoClassStateChanged(SELECTED, false)
        selectedOption = option
        optionBoxes[selectedOption]?.pseudoClassStateChanged(SELECTED, true)
    }

    private fun confirmText(text: String) {
        val option = makeOption(text) ?: return
        commit(option)
    }

    fun enterText(text: String) {
        searchText.text = text
    }

    override fun beforeShowing() {
        refilterOptions()
        if (initialOption in filteredOptions) select(initialOption)
    }

    fun showPopup(
        anchor: Point2D? = null, owner: Window? = null,
        initialOption: E? = null,
    ): E? {
        selectInitialOption(initialOption)
        return showDialog(owner, anchor)
    }

    fun showPopup(anchorNode: Region, initialOption: E? = null): E? {
        selectInitialOption(initialOption)
        return showDialog(anchorNode)
    }

    fun showPopup(ownerWindow: Window, anchorNode: Region?, initialOption: E? = null): E? {
        val anchor = anchorNode?.localToScreen(0.0, anchorNode.height)
        selectInitialOption(initialOption)
        return showDialog(ownerWindow, anchor)
    }

    fun showPopup(
        ev: Event?, initialOption: E? = null,
        offset: Point2D? = null, preferMouseCoords: Boolean = false,
    ): E? {
        selectInitialOption(initialOption)
        return showDialog(ev, offset, preferMouseCoords)
    }

    fun selectorButton(
        property: KMutableProperty0<E>, default: E = property.get(),
        undoManager: UndoManager? = null, actionDescription: String? = null,
        displayText: (E) -> String = this::displayText,
        onUpdate: (E, E) -> Unit = { _, _ -> },
    ): Button = PropertySelectorButton(property, this, default)
        .onUpdate(onUpdate)
        .displayText(displayText)
        .withUndo(undoManager, actionDescription)

    fun selectorButton(
        property: ReactiveVariable<E>, default: E = property.get(),
        undoManager: UndoManager? = null, actionDescription: String? = null,
        displayText: (E) -> String = this::displayText,
        onUpdate: (E, E) -> Unit = { _, _ -> },
    ): Button = VariableSelectorButton(property, this, default)
        .onUpdate(onUpdate)
        .displayText(displayText)
        .withUndo(undoManager, actionDescription)
}