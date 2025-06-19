package fxutils.drag

import fxutils.setPseudoClassState
import javafx.scene.Node
import javafx.scene.input.DragEvent

interface DropHandler {
    fun canDrop(event: DragEvent): Boolean

    fun drop(event: DragEvent): Boolean

    fun Node.updateDropPossible(possible: Boolean) {
        setPseudoClassState("drop-possible", possible)
    }
}