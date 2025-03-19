package fxutils.prompt

import fxutils.*
import fxutils.PseudoClasses.SELECTED
import javafx.geometry.Point2D
import javafx.scene.control.Button
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Window
import org.controlsfx.control.textfield.CustomTextField
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.material2.Material2MZ
import reaktive.event.event
import reaktive.value.ReactiveVariable
import reaktive.value.binding.map
import reaktive.value.fx.asObservableValue
import kotlin.reflect.KMutableProperty0

abstract class SearchableListView<E>(private val title: String) : VBox() {
    private val searchText = CustomTextField().styleClass("sleek-text-field", "search-field")
    private val layout = VBox().styleClass("options-box")

    private val optionBoxes = mutableMapOf<E, Region>()
    private var filteredOptions: List<E> = emptyList()
    var selectedOption: E? = null
        private set

    private val confirmOption = event<E>()

    val confirmedOption get() = confirmOption.stream

    private var filter: (E) -> Boolean = { true }

    protected abstract fun options(): List<E>

    init {
        styleClass("searchable-list")
        setupSearchField()
        layout.setMaxSize(300.0, 500.0)
        children.addAll(searchText, layout)
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
        hide()
        runFXWithTimeout {
            confirmOption.fire(option)
        }
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
        initialOption: E? = null, onConfirm: (E) -> Unit
    ) {
        prepareOptionBoxes()
        refilterOptions()
        if (initialOption in filteredOptions) select(initialOption)
        val w = SubWindow(this, title, type = SubWindow.Type.Popup)
        if (anchor != null) {
            w.x = anchor.x
            w.y = anchor.y
        } else w.centerOnScreen()
        userData = confirmedOption.observe { _, option -> onConfirm(option) }
        if (owner != null && w.owner == null) w.initOwner(owner)
        w.showAndWait()
    }

    fun showPopup(anchorNode: Region, initialOption: E? = null, onConfirm: (E) -> Unit) {
        val anchor = anchorNode.localToScreen(0.0, anchorNode.height)
        showPopup(anchor, anchorNode.scene.window, initialOption, onConfirm)
    }

    fun selectorButton(
        property: KMutableProperty0<E>, default: E = property.get(),
        displayText: (E) -> String = this::displayText
    ): Button = button(displayText(property.get())).apply {
        showPopupOnClick(default, property::get) { value ->
            property.set(value)
            text = displayText(value)
        }
    }

    fun selectorButton(
        property: ReactiveVariable<E>, default: E = property.get(),
        displayText: (E) -> String = this::displayText
    ): Button = button().apply {
        textProperty().bind(property.map(displayText).asObservableValue())
        showPopupOnClick(default, property::get) { value -> property.set(value) }
    }

    private fun Button.showPopupOnClick(default: E, get: () -> E, onSelect: (E) -> Unit) {
        setOnMouseClicked { ev ->
            when (ev.button) {
                MouseButton.PRIMARY -> {
                    showPopup(anchorNode = this, initialOption = get.invoke()) { option ->
                        onSelect(option)
                    }
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