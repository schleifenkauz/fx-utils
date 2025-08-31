package fxutils.controls

import reaktive.value.ReactiveVariable

class DoubleSpinner(
    value: ReactiveVariable<Double>,
    min: Double, max: Double,
    private val step: Double = 1.0
) : AbstractSpinner<Double>(value, min, max) {
    override fun increment(value: Double): Double = value + step

    override fun decrement(value: Double): Double = value + step

    override fun parseValue(text: String): Double? = text.toDoubleOrNull()
}