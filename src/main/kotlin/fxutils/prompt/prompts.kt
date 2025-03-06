package fxutils.prompt

import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.stage.Window

fun <T : Any> showSelectorDialog(
    title: String,
    items: List<T>, initialValue: T? = null,
    anchor: Point2D? = null,
    owner: Window? = null,
    stringConverter: (T) -> String = { it.toString() }
): T? {
    val view = object : SimpleSearchableListView<T>(items, title) {
        override fun extractText(option: T): String = stringConverter(option)
    }
    var value = initialValue
    view.showPopup(anchor, owner, initialOption = initialValue) { v -> value = v }
    return value
}

fun <T : Any> showSelectorDialog(
    title: String,
    items: List<T>, initialValue: T? = null,
    anchorNode: Node? = null, stringConverter: (T) -> String = { it.toString() }
): T? = showSelectorDialog(
    title, items, initialValue,
    anchorNode?.localToScreen(0.0, 0.0), anchorNode?.scene?.window,
    stringConverter
)

fun <R : Any> compoundPrompt(title: String, body: CompoundPrompt<R>.() -> Unit): CompoundPrompt<R> {
    val input = CompoundPrompt<R>(title)
    input.body()
    return input
}