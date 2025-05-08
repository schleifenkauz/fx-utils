package fxutils.undo

import reaktive.value.Variable

class ToggleEdit(
    override val actionDescription: String,
    private val variable: Variable<Boolean>,
) : AbstractEdit() {
    override fun doRedo() {
        variable.set(!variable.get())
    }

    override fun doUndo() {
        variable.set(!variable.get())
    }
}