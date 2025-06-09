package fxutils

import javafx.scene.control.TextField
import javafx.scene.text.Text

fun TextField.autoSize(condition: () -> Boolean) {
    textProperty().addListener { _, _, new ->
        if (condition()) updateWidth()
    }
    if (condition()) updateWidth()
}

fun TextField.updateWidth() {
    val helper = Text()
    helper.wrappingWidth = 0.0
    helper.text = text
    helper.font = font
    val textWidth = helper.prefWidth(-1.0) + 3.0
    prefWidth = 0.0
    minWidth = 0.0
    maxWidth = 0.0
    prefWidth = textWidth.coerceAtLeast(10.0)
    maxWidth = prefWidth
    minWidth = prefWidth
}
