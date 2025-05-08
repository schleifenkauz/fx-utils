package fxutils.prompt

import fxutils.*
import fxutils.PseudoClasses.SELECTED
import fxutils.undo.PropertyEdit
import fxutils.undo.UndoManager
import fxutils.undo.VariableEdit
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Screen
import javafx.stage.Window
import org.controlsfx.control.textfield.CustomTextField
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.material2.Material2MZ
import reaktive.value.ReactiveVariable
import reaktive.value.binding.map
import reaktive.value.fx.asObservableValue
import reaktive.value.now
import kotlin.reflect.KMutableProperty0

abstract class SearchableListView<E : Any>(private val title: String) : VBox() {
    private val searchText = CustomTextField().styleClass("sleek-text-field", "search-field")
    private val layout = VBox().styleClass("options-box")
    private val scrollPane = ScrollPane(layout)

    private val optionBoxes = mutableMapOf<E, Region>()
    private var filteredOptions: List<E> = emptyList()
    var selectedOption: E? = null
        private set

    private var _window: SubWindow? = null
    protected val window get() = _window ?: error("Window for prompt $title not initialized")

    private var filter: (E) -> Boolean = { true }

    private var result: E? = null

    protected abstract fun options(): List<E>

    fun withMaxHeight(height: Double) = also { scrollPane.maxHeight = height }

    init {
        styleClass("searchable-list")
        setupSearchField()
        scrollPane.maxHeight = 400.0
        children.addAll(searchText, scrollPane)
        registerShortcuts()
    }

    private fun setupSearchField() {
        searchText.promptText = "$title..."
        searchText.left = FontIcon(Material2MZ.SEARCH)
        searchText.textProperty().addListener { _ ->
            refilterOptions()
        }
    }

    fun setFilter(predicate: (E) -> Boolean) {
        filter = predicate
    }

    fun addFilter(predicate: (E) -> Boolean) {
        val oldFilter = filter
        filter = { e -> oldFilter(e) && predicate(e) }
    }

    fun removeOptions(options: List<E>) {
        addFilter { option -> option !in options }
    }

    protected fun getBox(option: E) = optionBoxes[option]

    protected open fun makeOption(text: String): E? = null

    protected abstract fun createCell(option: E): Region

    protected abstract fun extractText(option: E): String

    protected open fun displayText(option: E): String = extractText(option)

    override fun requestFocus() {
        searchText.requestFocus()
        searchText.selectAll()
    }

    private fun prepareOptionBoxes() {
        optionBoxes.clear()
        for (option in options()) {
            val box = createCell(option).styleClass("option-cell")
            box.setOnMouseClicked {
                confirm(option)
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
        if (scene != null) scene.window.sizeToScene()
    }

    private fun registerShortcuts() {
        addEventFilter(KeyEvent.KEY_PRESSED) { ev ->
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
                    if (selectedOption != null) confirm(selectedOption!!)
                    else confirmText(searchText.text)
                    ev.consume()
                }

                "Ctrl+Enter".shortcut.matches(ev) -> {
                    confirmText(searchText.text)
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

    private fun confirm(option: E) {
        result = option
        hide()
    }

    private fun confirmText(text: String) {
        val option = makeOption(text) ?: return
        confirm(option)
    }

    fun enterText(text: String) {
        searchText.text = text
    }

    fun showPopup(
        anchor: Point2D? = null, owner: Window? = null,
        initialOption: E? = null,
    ): E? {
        refilterOptions()
        if (initialOption in filteredOptions) select(initialOption)
        if (_window == null) {
            _window = SubWindow(this, title, type = SubWindow.Type.Popup)
            if (owner != null && window.owner == null) window.initOwner(owner)
        }
        if (anchor != null) {
            window.x = anchor.x
            window.y = anchor.y
        } else window.centerOnScreen()
        val screen = Screen.getScreensForRectangle(window.x, window.y, 1.0, 1.0).first()
        window.maxHeight = screen.visualBounds.maxY - window.y
        window.showAndWait()
        return result
    }

    fun showPopup(anchorNode: Region, initialOption: E? = null): E? {
        val anchor = anchorNode.localToScreen(0.0, anchorNode.height)
        return showPopup(anchor, anchorNode.scene.window, initialOption)
    }

    fun showPopup(ev: Event?, initialOption: E? = null): E? {
        if (ev == null) return showPopup()
        val anchorNode = ev.source as? Region
        return if (anchorNode != null) showPopup(anchorNode, initialOption)
        else when (val target = ev.target) {
            is Scene -> showPopup(owner = target.window)
            is Region -> showPopup(target, initialOption = initialOption)
            is Window -> showPopup(owner = target, initialOption = initialOption)
            else -> showPopup(initialOption = initialOption)
        }
    }

    fun selectorButton(
        property: KMutableProperty0<E>, default: E = property.get(),
        undoManager: UndoManager? = null, actionDescription: String? = null,
        displayText: (E) -> String = this::displayText,
    ): Button = button(displayText(property.get()).escapeUnderscores()).apply {
        showPopupOnClick(default, property::get) { value ->
            if (property.get() != value) {
                val oldValue = property.get()
                property.set(value)
                undoManager?.record(PropertyEdit(property, oldValue, value, actionDescription ?: title))
                text = displayText(value).escapeUnderscores()
            }
        }
    }

    fun selectorButton(
        property: ReactiveVariable<E>, default: E = property.get(),
        undoManager: UndoManager? = null, actionDescription: String? = null,
        displayText: (E) -> String = this::displayText,
    ): Button = button().apply {
        textProperty().bind(property.map { txt -> displayText(txt).escapeUnderscores() }.asObservableValue())
        showPopupOnClick(default, property::get) { value ->
            if (property.now != value) {
                val oldValue = property.now
                property.set(value)
                undoManager?.record(VariableEdit(property, oldValue, value, actionDescription ?: title))
            }
        }
    }

    private fun Button.showPopupOnClick(default: E, get: () -> E, onSelect: (E) -> Unit) {
        setOnMouseClicked { ev ->
            when (ev.button) {
                MouseButton.PRIMARY -> {
                    val result = showPopup(anchorNode = this, initialOption = get.invoke())
                    if (result != null) onSelect(result)
                }

                MouseButton.SECONDARY -> {
                    onSelect(default)
                }

                else -> {}
            }
        }
    }

    protected fun hide() {
        scene.window.hide()
    }
}