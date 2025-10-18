package fxutils.controls

import reaktive.value.ReactiveVariable
import reaktive.value.reactiveVariable

class IntSpinner(
    value: ReactiveVariable<Int>,
    min: Int, max: Int, private val step: Int = 1,
) : AbstractSpinner<Int>(value, min, max) {
    init {
        bind()
    }

    override fun increment(value: Int): Int = value + step

    override fun decrement(value: Int): Int = value - step

    override fun parseValue(text: String): Int? = text.toIntOrNull()

    constructor(
        value: ReactiveVariable<Int>, range: IntRange, step: Int = 1,
    ) : this(value, range.first, range.last, step)

    constructor(
        min: Int, max: Int, initialValue: Int, step: Int = 1,
    ) : this(reactiveVariable(initialValue), min, max, step)

    constructor(
        range: IntRange, initialValue: Int, step: Int = 1,
    ) : this(range.first, range.last, initialValue, step)
}