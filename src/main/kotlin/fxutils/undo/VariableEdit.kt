/**
 *@author Nikolaus Knop
 */

package fxutils.undo

import reaktive.value.Variable

/**
 * An edit that indicates that the value of the given [variable] has changed.
 */
class VariableEdit<T>(
    private val variable: Variable<T>,
    private val oldValue: T,
    private val newValue: T,
    override val actionDescription: String,
) : AbstractEdit() {
    constructor(
        variable: Variable<T>, oldValue: T, actionDescription: String,
    ) : this(variable, oldValue, variable.get(), actionDescription)

    override fun doRedo() {
        variable.set(newValue)
    }

    override fun doUndo() {
        variable.set(oldValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun mergeWith(other: Edit): Edit? =
        if (other !is VariableEdit<*> || other.variable !== this.variable) null
        else VariableEdit(variable, this.oldValue, other.newValue as T, other.actionDescription)

    companion object {
        fun <T> updateVariable(
            variable: Variable<T>, newValue: T,
            manager: UndoManager, description: String = "Update",
        ) {
            if (variable.get() == newValue) return
            manager.record(VariableEdit(variable, variable.get(), newValue, description))
            variable.set(newValue)
        }
    }
}