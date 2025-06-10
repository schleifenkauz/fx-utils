package fxutils

import javafx.scene.control.TextField
import javafx.scene.text.Font
import javafx.scene.text.Text

fun TextField.autoSize(condition: () -> Boolean = { true }, minColumns: () -> Int = { 1 }) {
    val helper = Text()
    sceneProperty().addListener { _, _, sc ->
        if (sc != null) {
            runFXWithTimeout(10) {
                if (condition()) updateWidth(helper, minColumns())
                textProperty().addListener { _, _, _ ->
                    if (condition()) updateWidth(helper, minColumns())
                }
            }
        }
    }
}

fun TextField.updateWidth(helper: Text, minColumns: Int) {
    val textWidth = helper.computeTextWidth(text, font)
    prefWidth = 0.0
    minWidth = 0.0
    maxWidth = 0.0
    val min = helper.computeTextWidth("M", font) * minColumns
    val width = textWidth.coerceAtLeast(min)
    prefWidth = width
    maxWidth = width
    minWidth = width
}

private fun Text.computeTextWidth(text: String, font: Font): Double {
    wrappingWidth = 0.0
    this.text = text
    this.font = font
    val textWidth = prefWidth(-1.0) + 3.0
    return textWidth
}
