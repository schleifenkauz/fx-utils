/**
 * @author Nikolaus Knop
 */

package fxutils.undo

import fxutils.undo.UndoManager

/**
 * Calls [UndoManager.beginCompoundEdit] before [UndoManager.finishCompoundEdit] after executing the given [actions].
 * @param description the [Edit.actionDescription] of the resulting compound edit.
 */
inline fun <T> UndoManager.compoundEdit(description: String, actions: () -> T): T {
    beginCompoundEdit()
    val res = try {
        actions()
    } finally {
        finishCompoundEdit(description)
    }
    return res
}

/**
 * Deactivates the [UndoManager] while executing the given [action] and then reactivates it.
 * @see UndoManager.isActive
 * @see UndoManager.record
 */
inline fun <T> UndoManager.withoutUndo(action: () -> T): T {
    if (!isActive) return action()
    isActive = false
    try {
        return action()
    } catch (ex: Throwable) {
        throw ex
    } finally {
        isActive = true
    }
}