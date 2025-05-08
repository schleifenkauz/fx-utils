package fxutils.undo

import reaktive.value.Variable

class ToggleEdit(
    private val variableDescription: String,
    private val variable: Variable<Boolean>,
) : AbstractEdit() {
    override val actionDescription: String
        get() = "Toggle $variableDescription"

    override fun doRedo() {
        variable.set(!variable.get())
    }

    override fun doUndo() {
        variable.set(!variable.get())
    }
}