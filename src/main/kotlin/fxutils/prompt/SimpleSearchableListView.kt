package fxutils.prompt

import fxutils.styleClass
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Region

open class SimpleSearchableListView<E : Any>(private val options: List<E>, title: String) : SearchableListView<E>(title) {
    constructor(title: String) : this(emptyList(), title)

    override fun options(): List<E> = options

    override fun extractText(option: E): String = option.toString()

    override fun createCell(option: E): Region = HBox(Label(displayText(option)).styleClass("option-label"))
}