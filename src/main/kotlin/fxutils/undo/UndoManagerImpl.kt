/**
 *@author Nikolaus Knop
 */

package fxutils.undo

import fxutils.undo.withoutUndo
import reaktive.value.ReactiveBoolean
import reaktive.value.ReactiveString
import reaktive.value.binding.notEqualTo
import reaktive.value.now
import reaktive.value.reactiveVariable
import java.util.*

internal class UndoManagerImpl(private val parent: UndoManagerImpl?) : UndoManager {
    private val redoable = LinkedList<Edit>()
    private val undoable = LinkedList<Edit>()
    private var compoundDescription: String? = null
    private var compound: MutableList<Edit>? = null

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

    override val accumulatesCompoundEdit: Boolean
        get() = compound != null

    private fun updateReactiveVariables() {
        _canRedo.now = redoable.isNotEmpty()
        _redoText.now = redoable.peek()?.run { "Redo $actionDescription" } ?: "Cannot redo"
        _canUndo.now = undoable.isNotEmpty()
        _undoText.now = undoable.peek()?.run { "Undo $actionDescription" } ?: "Cannot undo"
    }

    override fun undo() {
        check(canUndo.now) { "Cannot undo" }
        check(compound == null) { "Undo during compound edit is not possible" }
        val e = undoable.poll()
        withoutUndo { e.undo() }
        undo(e)
    }

    override fun redo() {
        check(canRedo.now) { "Cannot redo" }
        check(compound == null) { "Redo during compound edit is not possible" }
        val e = redoable.poll()
        withoutUndo { e.redo() }
        redo(e)
    }

    private fun undo(e: Edit) {
        editCount.now -= 1
        redoable.push(e)
        updateReactiveVariables()
        if (parent != null) {
            parent.undoable.remove(e)
            parent.undo(e)
        }
    }

    private fun redo(e: Edit) {
        editCount.now += 1
        undoable.push(e)
        updateReactiveVariables()
        if (parent != null) {
            parent.redoable.remove(e)
            parent.redo(e)
        }
    }

    override fun reset() {
        redoable.clear()
        undoable.clear()
        compoundDescription = null
        compound = null
        updateReactiveVariables()
    }

    override fun savedChanges() {
        editCount.now = 0
    }

    override fun record(edit: Edit) {
        if (!isActive) return
        check(edit.canUndo) { "Attempt to push non-undoable edit to UndoManager" }
        if (compound != null) {
            compound!!.add(edit)
            return
        }
        redoable.clear()
        val last = undoable.peekFirst()
        val merged = last?.mergeWith(edit)
        if (merged != null) {
            undoable.poll()
            undoable.push(merged)
        } else {
            undoable.push(edit)
            editCount.now += 1
        }
        updateReactiveVariables()
    }

    override fun createSubManager(): UndoManager = UndoManagerImpl(parent = this)

    override fun beginCompoundEdit(description: String?) {
        if (compound != null) {
            System.err.println("Compound edit already begun description: $compoundDescription, new: $description")
            return
        }
        compound = mutableListOf()
        compoundDescription = description
    }

    override fun finishCompoundEdit(description: String?) {
        val edits = compound
        if (edits == null) {
            System.err.println("No compound already begun (description: $description)")
            return
        }
        compound = null
        val desc = compoundDescription ?: description ?: error("no description for compound edit provided")
        compoundDescription = null
        when (edits.size) {
            0 -> return
            1 -> record(edits[0])
            else -> {
                val e = CompoundEdit(edits, desc)
                record(e)
            }
        }
    }
}