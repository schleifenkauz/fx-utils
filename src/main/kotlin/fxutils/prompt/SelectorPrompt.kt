package fxutils.prompt

import fxutils.PseudoClasses.SELECTED
import fxutils.controls.PropertySelectorButton
import fxutils.controls.VariableSelectorButton
import fxutils.registerShortcuts
import fxutils.shortcut
import fxutils.styleClass
import fxutils.undo.UndoManager
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode
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

    private val optionCells = mutableMapOf<Option<E>, Region>()
    private var filteredOptions: List<E> = emptyList()
    protected var selectedOption: Option<E> = Option.None
        private set

    private var filter: (E) -> Boolean = { true }

    protected open val canCreateItem: Boolean get() = false

    protected open val maxItems: Int get() = 10

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
        select(option)
        return this
    }

    protected fun getBox(option: E) = optionCells[Option.SelectItem(option)]

    protected open fun makeOption(text: String): E? = null

    protected open fun createCell(option: E): Region = HBox(Label(displayText(option)).styleClass("option-label"))

    protected abstract fun extractText(option: E): String

    open fun displayText(option: E): String = extractText(option)

    override fun getDefault(): E? = null

    override fun onReceiveFocus() {
        searchText.requestFocus()
        searchText.selectAll()
    }

    override fun createLayout(): Region = content

    private fun getOptionCell(option: Option<E>): Region = optionCells.getOrPut(option) {
        when (option) {
            Option.None -> HBox()
            is Option.SelectItem -> {
                val cell = createCell(option.obj).styleClass("option-cell")
                if (option == selectedOption) {
                    cell.pseudoClassStateChanged(SELECTED, true)
                }
                cell.setOnMouseClicked { commit(option) }
                cell
            }

            is Option.CreateItem -> {
                val label = Label().styleClass("option-label")
                label.textProperty().bind(searchText.textProperty().map { "Create '$it'" })
                val box = HBox(label).styleClass("option-cell")
                box.setOnMouseClicked {
                    confirmText(searchText.text)
                }
                box
            }
        }
    }

    private fun refilterOptions() {
        layout.children.clear()
        filteredOptions = options().filter(filter).distinct()
        val itemTexts = filteredOptions.map(::extractText).map(String::lowercase)
        val search = searchText.text.lowercase()
        if (search.isNotBlank()) {
            val sortedBySimilarity = filteredOptions.asSequence()
                .mapIndexed { i, option -> option to similarity(search, itemTexts[i]) }
                .filter { (_, similarity) -> similarity > 0 }
                .sortedByDescending { (item, similarity) ->
                    if (selectedOption == Option.SelectItem(item)) Int.MAX_VALUE
                    else similarity
                }
                .map { (option, _) -> option }
            filteredOptions = sortedBySimilarity.take(maxItems).toList()
        } else {
            filteredOptions = filteredOptions.take(maxItems)
        }
        for (option in filteredOptions) {
            val cell = getOptionCell(Option.SelectItem(option))
            layout.children.add(cell)
        }
        if (canCreateItem && filteredOptions.size < maxItems &&
            search.isNotBlank() && search !in itemTexts
        ) {
            val box = getOptionCell(Option.CreateItem)
            layout.children.add(box)
        }
        select(
            selectedOption.takeIf { it is Option.SelectItem && it.obj in filteredOptions }
                ?: filteredOptions.firstOrNull()?.let(Option<*>::SelectItem)
                ?: Option.CreateItem.takeIf { canCreateItem }
                ?: Option.None
        )
        if (_window != null) window.sizeToScene()
    }

    private fun registerShortcuts() {
        content.addEventFilter(KeyEvent.KEY_PRESSED) { ev ->
            if ("DOWN".shortcut.matches(ev) || "UP".shortcut.matches(ev)) {
                val deltaIdx = if (ev.code == KeyCode.UP) -1 else +1
                val selectedIndex = when (val option = selectedOption) {
                    Option.None -> -1
                    is Option.SelectItem -> filteredOptions.indexOf(option.obj)
                    Option.CreateItem -> filteredOptions.size
                }
                val nextIndex = (selectedIndex + deltaIdx).mod(filteredOptions.size)
                if (nextIndex in filteredOptions.indices) {
                    select(filteredOptions[nextIndex])
                } else if (nextIndex == filteredOptions.size) {
                    if (optionCells.containsKey(Option.CreateItem)) {
                        select(Option.CreateItem)
                    } else {
                        select(filteredOptions.firstOrNull())
                    }
                } else if (nextIndex == -1 || nextIndex == filteredOptions.lastIndex + 1) {
                    select(filteredOptions.lastOrNull())
                }
                ev.consume()
            }

        }
        content.registerShortcuts {
            on("Enter") { confirm() }
            on("Tab", consume = false) {}
            on("Ctrl+Enter") {
                val text = searchText.text.takeIf { it.isNotBlank() } ?: return@on
                confirmText(text)
            }
        }
    }

    private fun confirm() {
        if (selectedOption != Option.None) {
            commit(selectedOption)
        } else confirmText(searchText.text)
    }

    private fun commit(option: Option<E>) {
        when (option) {
            Option.None -> return
            is Option.SelectItem -> commit(option.obj)
            is Option.CreateItem -> {
                val text = searchText.text.takeIf { it.isNotBlank() } ?: return
                val newOption = makeOption(text) ?: return
                commit(newOption)
            }
        }
    }

    private fun select(option: Option<E>) {
        optionCells[selectedOption]?.pseudoClassStateChanged(SELECTED, false)
        selectedOption = option
        optionCells[option]?.pseudoClassStateChanged(SELECTED, true)
    }

    fun select(option: E?) {
        select(if (option == null) Option.None else Option.SelectItem(option))
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
    }

    fun showPopup(placement: PromptPlacement, initialOption: E? = null): E? =
        selectInitialOption(initialOption).showDialog(placement)

    fun showPopup(
        anchor: Point2D, owner: Window,
        initialOption: E? = null,
    ): E? = selectInitialOption(initialOption).showDialog(owner, anchor)

    fun showPopup(anchorNode: Region, initialOption: E? = null): E? {
        selectInitialOption(initialOption)
        return showDialog(anchorNode)
    }

    fun showPopup(ev: Event?, initialOption: E? = null, preferMouseCoords: Boolean = false): E? =
        selectInitialOption(initialOption).showDialog(ev, preferMouseCoords)

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

    protected sealed class Option<out E> {
        data object None : Option<Nothing>()
        data class SelectItem<E>(val obj: E) : Option<E>()
        data object CreateItem : Option<Nothing>()
    }

    companion object {
        private fun similarity(search: String, match: String): Int {
            var i = 0
            for (ch in search) {
                while (i < match.length && ch != match[i]) i++
                if (i == match.length) return 0
            }
            val minLength = minOf(search.length, match.length)
            var count = 0
            for (i in 0 until minLength) {
                if (search[i] == match[i]) count++
            }
            return count + 1
        }
    }
}