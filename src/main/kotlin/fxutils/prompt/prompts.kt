package fxutils.prompt

fun <T : Any> showSelectorDialog(
    title: String, placement: PromptPlacement,
    items: List<T>, initialValue: T? = null,
    stringConverter: (T) -> String = { it.toString() },
): T? {
    val view = object : SimpleSelectorPrompt<T>(items, title) {
        override fun extractText(option: T): String = stringConverter(option)
    }
    return view.showPopup(placement, initialValue)
}

fun <R : Any> compoundPrompt(
    title: String, labelWidth: Double = DetailPane.LABEL_WIDTH,
    okText: String = "_Ok", cancelText: String = "_Cancel",
    body: CompoundPrompt<R>.() -> Unit,
): CompoundPrompt<R> {
    val input = CompoundPrompt<R>(title, labelWidth, cancelText, okText)
    input.body()
    return input
}