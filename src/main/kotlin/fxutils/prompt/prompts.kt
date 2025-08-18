package fxutils.prompt

import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.stage.Window

fun <T : Any> showSelectorDialog(
    title: String,
    items: List<T>, initialValue: T? = null,
    anchor: Point2D? = null,
    owner: Window? = null,
    stringConverter: (T) -> String = { it.toString() },
): T? {
    val view = object : SimpleSearchableListView<T>(items, title) {
        override fun extractText(option: T): String = stringConverter(option)
    }
    return view.showPopup(anchor, owner, initialOption = initialValue)
}

fun <T : Any> showSelectorDialog(
    title: String,
    items: List<T>, initialValue: T? = null,
    anchorNode: Node? = null, stringConverter: (T) -> String = { it.toString() },
): T? = showSelectorDialog(
    title, items, initialValue,
    anchorNode?.localToScreen(0.0, 0.0), anchorNode?.scene?.window,
    stringConverter
)

fun <R : Any> compoundPrompt(
    title: String, labelWidth: Double = DetailPane.LABEL_WIDTH,
    okText: String = "_Ok", cancelText: String = "_Cancel",
    body: CompoundPrompt<R>.() -> Unit,
): CompoundPrompt<R> {
    val input = CompoundPrompt<R>(title, labelWidth, cancelText, okText)
    input.body()
    return input
}