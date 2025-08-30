package fxutils.prompt

open class SimpleSelectorPrompt<E : Any>(private val options: List<E>, title: String) : SelectorPrompt<E>(title) {
    constructor(title: String) : this(emptyList(), title)

    override fun options(): List<E> = options

    override fun extractText(option: E): String = option.toString()
}