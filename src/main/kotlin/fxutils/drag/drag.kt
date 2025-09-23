package fxutils.drag

import fxutils.modifiers
import javafx.event.EventType
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Point2D
import javafx.geometry.Rectangle2D
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.input.DragEvent
import javafx.scene.input.Dragboard
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.stage.Window
import kotlin.math.absoluteValue

fun Node.setupDragging(
    defaultCursor: Cursor = Cursor.OPEN_HAND, dragCursor: Cursor = Cursor.CLOSED_HAND,
    startDragEvent: EventType<MouseEvent> = MouseEvent.DRAG_DETECTED,
    onPressed: (ev: MouseEvent) -> Boolean = { true },
    onReleased: (ev: MouseEvent) -> Unit = {},
    relocateBy: (ev: MouseEvent, start: Point2D, old: Bounds, dx: Double, dy: Double) -> Unit,
) {
    var dragStart: Point2D? = null
    var localStart: Point2D? = null
    var oldBounds: Bounds? = null
    cursor = defaultCursor
    addEventHandler(MouseEvent.ANY) { ev ->
        when (ev.eventType) {
            startDragEvent -> {
                if (onPressed(ev)) {
                    onPressed(ev)
                    cursor = dragCursor
                }
            }

            MouseEvent.MOUSE_DRAGGED -> {
                val start = dragStart
                if (start == null) {
                    dragStart = Point2D(ev.screenX, ev.screenY)
                    localStart = Point2D(ev.x, ev.y)
                    oldBounds = boundsInParent
                } else {
                    val dx = ev.screenX - start.x
                    val dy = ev.screenY - start.y
                    relocateBy(ev, localStart!!, oldBounds!!, dx, dy)
                }
            }

            MouseEvent.MOUSE_RELEASED -> {
                cursor = defaultCursor
                if (dragStart != null) {
                    onReleased(ev)
                    dragStart = null
                    oldBounds = null
                    localStart = null
                }
            }

            else -> return@addEventHandler
        }
        ev.consume()
    }
}

fun Region.setupDraggingAndResizing(
    canUserChangeWidth: Boolean, canUserChangeHeight: Boolean, threshold: Double,
    drag: (x: Double, y: Double) -> Unit,
    resize: (MouseEvent, Cursor, Rectangle2D) -> Unit,
    startDrag: (MouseEvent, Cursor) -> Boolean = { _, _ -> true },
    finishDrag: (MouseEvent, Cursor) -> Unit = { _, _ -> },
) {
    cursor = Cursor.OPEN_HAND
    var dragStart: Point2D? = null
    var oldBounds: Bounds? = null
    addEventHandler(MouseEvent.ANY) { ev ->
        when (ev.eventType) {
            MouseEvent.DRAG_DETECTED -> {
                if (dragStart == null && cursor != null) {
                    if (startDrag(ev, cursor)) {
                        oldBounds = BoundingBox(layoutX, layoutY, width, height)
                        dragStart = Point2D(ev.screenX, ev.screenY)
                    }
                }
            }

            MouseEvent.MOUSE_DRAGGED -> {
                val start = dragStart ?: return@addEventHandler
                val dx = ev.screenX - start.x
                val dy = ev.screenY - start.y
                val b = oldBounds!!
                if (isResizeCursor(cursor)) {
                    val (x, w) = when (cursor) {
                        Cursor.E_RESIZE, Cursor.NE_RESIZE, Cursor.SE_RESIZE -> Pair(b.minX, b.width + dx)
                        Cursor.W_RESIZE, Cursor.NW_RESIZE, Cursor.SW_RESIZE -> Pair(b.minX + dx, b.width - dx)
                        else -> Pair(b.minX, b.width)
                    }
                    val (y, h) = when (cursor) {
                        Cursor.S_RESIZE, Cursor.SE_RESIZE, Cursor.SW_RESIZE -> Pair(b.minY, b.height + dy)
                        Cursor.N_RESIZE, Cursor.NE_RESIZE, Cursor.NW_RESIZE -> Pair(b.minY + dy, b.height - dy)
                        else -> Pair(b.minY, b.height)
                    }
                    resize(ev, cursor, Rectangle2D(x, y, w, h))
                } else {
                    val x = b.minX + dx
                    val y = b.minY + dy
                    drag(x, y)
                }
            }

            MouseEvent.MOUSE_MOVED -> {
                cursor = getCursor(ev, canUserChangeWidth, canUserChangeHeight, ev.isPrimaryButtonDown, threshold)
                return@addEventHandler
            }

            MouseEvent.MOUSE_RELEASED -> {
                if (dragStart != null) {
                    finishDrag(ev, cursor)
                    dragStart = null
                    oldBounds = null
                }
            }

            else -> return@addEventHandler
        }
        ev.consume()
    }
}

private fun Region.getCursor(
    ev: MouseEvent, canUserChangeWidth: Boolean, canUserChangeHeight: Boolean, closeHand: Boolean,
    threshold: Double,
): Cursor {
    val x = ev.x
    val y = ev.y
    val dx = (x - prefWidth).absoluteValue
    val dy = (y - prefHeight).absoluteValue
    return when {
        x.absoluteValue < threshold && y.absoluteValue < threshold && canUserChangeHeight && canUserChangeWidth -> Cursor.NW_RESIZE
        x.absoluteValue < threshold && dy.absoluteValue < threshold && canUserChangeHeight && canUserChangeWidth -> Cursor.SW_RESIZE
        dx < threshold && y.absoluteValue < threshold && canUserChangeHeight && canUserChangeWidth -> Cursor.NE_RESIZE
        dx < threshold && dy < threshold && canUserChangeHeight && canUserChangeWidth -> Cursor.SE_RESIZE
        x.absoluteValue < threshold && canUserChangeWidth -> Cursor.W_RESIZE
        dx < threshold && canUserChangeWidth -> Cursor.E_RESIZE
        y.absoluteValue < threshold && canUserChangeHeight -> Cursor.N_RESIZE
        dy < threshold && canUserChangeHeight -> Cursor.S_RESIZE
        closeHand -> Cursor.CLOSED_HAND
        else -> Cursor.OPEN_HAND
    }
}

fun isResizeCursor(cursor: Cursor?) = cursor.toString().endsWith("RESIZE")

fun Node.setupWindowDragging(cursor: Cursor, window: () -> Window?) {
    var startCords = Point2D(0.0, 0.0)
    setupDragging(
        defaultCursor = cursor,
        startDragEvent = MouseEvent.MOUSE_PRESSED,
        onPressed = {
            val w = window() ?: return@setupDragging false
            startCords = Point2D(w.x, w.y)
            true
        },
        relocateBy = { _, _, _, dx, dy ->
            val w = window() ?: return@setupDragging
            w.x = startCords.x + dx
            w.y = startCords.y + dy
        })
}

fun Node.setupDropArea(dropHandler: DropHandler) {
    addEventHandler(DragEvent.DRAG_OVER) { ev ->
        val acceptedTransferModes = dropHandler.acceptedTransferModes(ev)
        if (acceptedTransferModes.isNotEmpty()) {
            ev.acceptTransferModes(*acceptedTransferModes)
            ev.consume()
        }
    }
    addEventHandler(DragEvent.DRAG_ENTERED) { ev ->
        if (dropHandler.acceptedTransferModes(ev).isNotEmpty()) {
            dropHandler.run { updateDropPossible(true) }
            ev.consume()
        }
    }
    addEventHandler(DragEvent.DRAG_EXITED) { ev ->
        dropHandler.run { updateDropPossible(false) }
        ev.consume()
    }
    addEventHandler(DragEvent.DRAG_DROPPED) { ev ->
        if (dropHandler.acceptedTransferModes(ev).isNotEmpty()) {
            ev.consume()
            try {
                if (dropHandler.drop(ev)) {
                    ev.isDropCompleted = true
                }
            } catch (ex: Exception) {
                System.err.println("Exception while dropping")
                ex.printStackTrace()
            }
        }
    }
}

fun Dragboard.hasFiles(vararg extensions: String) =
    hasFiles() && files.all { f -> f.extension in extensions }

fun Dragboard.hasFile(vararg extensions: String): Boolean = hasFiles(*extensions) && files.size == 1