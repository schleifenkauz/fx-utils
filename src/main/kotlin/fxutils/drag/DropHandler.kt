package fxutils.drag

import fxutils.setPseudoClassState
import javafx.scene.Node
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode

interface DropHandler {
    fun acceptedTransferModes(event: DragEvent): Array<out TransferMode>

    fun drop(event: DragEvent): Boolean

    fun Node.updateDropPossible(possible: Boolean) {
        setPseudoClassState("drop-possible", possible)
    }
}