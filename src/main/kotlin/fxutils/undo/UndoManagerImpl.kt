/**
 *@author Nikolaus Knop
 */

package fxutils.undo

import reaktive.value.ReactiveBoolean
import reaktive.value.ReactiveString
import reaktive.value.binding.notEqualTo
import reaktive.value.now
import reaktive.value.reactiveVariable
import java.util.*

internal class UndoManagerImpl(private val parent: UndoManagerImpl?) : UndoManager {
    private val redoable = LinkedList<AttachedEdit>()
    private val undoable = LinkedList<AttachedEdit>()
    private var compoundDescription: String? = null
    private var _compoundEdit: MutableList<Edit>? = null
    private val compoundEdit get() = _compoundEdit ?: parent?._compoundEdit

    private val _canUndo = reactiveVariable(false)
    private val _canRedo = reactiveVariable(false)
    private val _undoText = reactiveVariable("Cannot undo")
    private val _redoText = reactiveVariable("Cannot redo")

    override val canUndo: ReactiveBoolean get() = _canUndo
    override val canRedo: ReactiveBoolean get() = _canRedo
    override val undoText: ReactiveString
        get() = _undoText
    override val redoText: ReactiveString
        get() = _redoText

    private val editCount = reactiveVariable(0)

    override val hasUnsavedChanges: ReactiveBoolean
        get() = editCount.notEqualTo(0)

    override var isActive: Boolean = true
        get() = field && (parent?.isActive ?: true)

    override val accumulatesCompoundEdit: Boolean
        get() = compoundEdit != null

    private fun updateReactiveVariables() {
        _canRedo.now = redoable.isNotEmpty()
        _redoText.now = redoable.peek()?.run { "Redo ${edit.actionDescription}" } ?: "Cannot redo"
        _canUndo.now = undoable.isNotEmpty()
        _undoText.now = undoable.peek()?.run { "Undo ${edit.actionDescription}" } ?: "Cannot undo"
    }

    override fun undo() {
        check(canUndo.now) { "Cannot undo" }
        check(compoundEdit == null) { "Undo during compound edit is not possible" }
        val e = undoable.peekFirst() ?: error("Undoable stack is empty")
        e.manager.withoutUndo { e.edit.undo() }
        e.manager.parentChain {
            editCount.now -= 1
            undoable.remove(e)
            redoable.push(e)
            updateReactiveVariables()
        }
    }

    override fun redo() {
        check(canRedo.now) { "Cannot redo" }
        check(compoundEdit == null) { "Redo during compound edit is not possible" }
        val e = redoable.peekFirst() ?: error("Redoable stack is empty")
        e.manager.withoutUndo { e.edit.redo() }
        e.manager.parentChain {
            editCount.now += 1
            redoable.remove(e)
            undoable.push(e)
            updateReactiveVariables()
        }
    }

    override fun savedChanges() {
        editCount.now = 0
    }

    private inline fun parentChain(action: UndoManagerImpl.() -> Unit) {
        var manager = this
        while (true) {
            manager.action()
            manager = manager.parent ?: break
        }
    }

    override fun record(edit: Edit) {
        if (!isActive) return
        check(edit.canUndo) { "Attempt to push non-undoable edit to UndoManager" }
        if (compoundEdit != null) {
            compoundEdit!!.add(edit)
            return
        }
        val last = undoable.peekFirst()
        val merged = last?.takeIf { it.manager == this }?.edit?.mergeWith(edit)
        val attached = AttachedEdit(merged ?: edit, this)
        parentChain {
            redoable.clear()
            if (merged != null) {
                undoable.remove(last)
                undoable.push(attached)
            } else {
                undoable.push(attached)
                editCount.now += 1
            }
            updateReactiveVariables()
        }
    }

    override fun createSubManager(): UndoManager = UndoManagerImpl(parent = this)

    override fun beginCompoundEdit(description: String?) {
        if (compoundEdit != null) {
            System.err.println("Compound edit already begun description: $compoundDescription, new: $description")
            return
        }
        _compoundEdit = mutableListOf()
        compoundDescription = description
    }

    override fun finishCompoundEdit(description: String?) {
        val edits = _compoundEdit
        if (edits == null) {
            System.err.println("No compound already begun (description: $description)")
            return
        }
        _compoundEdit = null
        val desc = compoundDescription ?: description ?: error("no description for compound edit provided")
        compoundDescription = null
        if (edits.isNotEmpty()) {
            val e = CompoundEdit(edits, desc)
            record(e)
        }
    }

    private data class AttachedEdit(val edit: Edit, val manager: UndoManagerImpl)
}