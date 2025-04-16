package fxutils.prompt

import fxutils.styleClass
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

open class DetailPane(private val labelWidth: Double = LABEL_WIDTH) : VBox() {
    private val items = mutableListOf<Item>()

    init {
        styleClass("detail-pane", "tool-pane")
    }

    fun addItem(name: String, control: Node) {
        val label = Label(name)
        label.prefWidth = labelWidth
        HBox.setHgrow(label, Priority.NEVER)
        HBox.setHgrow(control, Priority.ALWAYS)
        val box = HBox(5.0, label, control) styleClass "detail-item"
        children.add(box)
        items.add(Item(name, control))
    }

    fun addLargeItem(name: String, control: Node) {
        val label = Label(name)
        val box = VBox(5.0, label, control) styleClass "detail-item"
        children.add(box)
        items.add(Item(name, control))
    }

    fun clear() {
        children.clear()
        items.clear()
    }

    fun items(): List<Item> = items

    data class Item(val name: String, val control: Node)

    companion object {
        const val LABEL_WIDTH = 80.0
    }
}