package fxutils

import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Point2D
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.stage.Window
import kotlin.math.absoluteValue

fun Node.setupDragging(
    draggingActive: () -> Boolean = { true },
    defaultCursor: Cursor = Cursor.OPEN_HAND, dragCursor: Cursor = Cursor.CLOSED_HAND,
    onPressed: (ev: MouseEvent) -> Unit = {},
    onReleased: (ev: MouseEvent) -> Unit = {},
    relocateBy: (ev: MouseEvent, start: Point2D, old: Bounds, dx: Double, dy: Double) -> Unit
) {
    var dragStart: Point2D? = null
    var localStart: Point2D? = null
    var oldBounds: Bounds? = null
    cursor = defaultCursor
    addEventHandler(MouseEvent.ANY) { ev ->
        if (!draggingActive()) return@addEventHandler
        when (ev.eventType) {
            MouseEvent.MOUSE_PRESSED -> {
                cursor = dragCursor
                onPressed(ev)
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
                onReleased(ev)
                dragStart = null
                oldBounds = null
                localStart = null
            }

            else -> return@addEventHandler
        }
        ev.consume()
    }
}

fun Region.setupDraggingAndResizing(
    canUserChangeWidth: Boolean, canUserChangeHeight: Boolean,
    moveActive: () -> Boolean, resizeActive: () -> Boolean,
    drag: (x: Double, y: Double) -> Unit,
    resize: (Bounds, Double, Double, Cursor, MouseEvent) -> Unit,
    startDrag: (MouseEvent, Cursor) -> Boolean = { _, _ -> true },
    finishDrag: (MouseEvent, Cursor) -> Unit = { _, _ -> }
) {
    cursor = Cursor.OPEN_HAND
    var dragStart: Point2D? = null
    var oldBounds: Bounds? = null
    addEventHandler(MouseEvent.ANY) { ev ->
        if (!moveActive() && !resizeActive()) return@addEventHandler
        when (ev.eventType) {
            MouseEvent.MOUSE_PRESSED -> {
                if (dragStart == null && cursor != null) {
                    oldBounds = BoundingBox(layoutX, layoutY, width, height)
                    dragStart = Point2D(ev.screenX, ev.screenY)
                    startDrag(ev, cursor)

                }
            }

            MouseEvent.MOUSE_DRAGGED -> {
                val start = dragStart ?: return@addEventHandler
                val dx = ev.screenX - start.x
                val dy = ev.screenY - start.y
                if (isResizeCursor(cursor)) {
                    resize(oldBounds!!, dx, dy, cursor, ev)
                } else {
                    val x = oldBounds!!.minX + dx
                    val y = oldBounds!!.minY + dy
                    drag(x, y)
                }
            }

            MouseEvent.MOUSE_MOVED -> {
                cursor = getCursor(
                    moveActive(), resizeActive(),
                    ev, canUserChangeWidth, canUserChangeHeight, ev.isPrimaryButtonDown
                )
                return@addEventHandler
            }

            MouseEvent.MOUSE_RELEASED -> {
                if (dragStart != null) {
                    if (ev.screenX != dragStart!!.x || ev.screenY != dragStart!!.y) {
                        finishDrag(ev, cursor)
                    }
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
    moveActive: Boolean, resizeActive: Boolean,
    ev: MouseEvent, canUserChangeWidth: Boolean, canUserChangeHeight: Boolean, closeHand: Boolean
): Cursor {
    val x = ev.x
    val y = ev.y
    val tx = 5
    val ty = 5
    val dx = (x - prefWidth).absoluteValue
    val dy = (y - prefHeight).absoluteValue
    return when {
        moveActive -> if (closeHand) Cursor.CLOSED_HAND else Cursor.OPEN_HAND
        !resizeActive -> if (closeHand) cursor else Cursor.DEFAULT
        x.absoluteValue < tx && y.absoluteValue < ty && canUserChangeHeight && canUserChangeWidth -> Cursor.NW_RESIZE
        x.absoluteValue < tx && dy.absoluteValue < ty && canUserChangeHeight && canUserChangeWidth -> Cursor.SW_RESIZE
        dx < tx && y.absoluteValue < ty && canUserChangeHeight && canUserChangeWidth -> Cursor.NE_RESIZE
        dx < tx && dy < ty && canUserChangeHeight && canUserChangeWidth -> Cursor.SE_RESIZE
        x.absoluteValue < tx && canUserChangeWidth -> Cursor.W_RESIZE
        dx < tx && canUserChangeWidth -> Cursor.E_RESIZE
        y.absoluteValue < ty && canUserChangeHeight -> Cursor.N_RESIZE
        dy < ty && canUserChangeHeight -> Cursor.S_RESIZE
        closeHand -> Cursor.CLOSED_HAND
        else -> Cursor.OPEN_HAND
    }
}

fun isResizeCursor(cursor: Cursor?) = cursor.toString().endsWith("RESIZE")

fun Button.setupWindowDragButton(window: () -> Window) {
    var startCords = Point2D(0.0, 0.0)
    setupDragging(
        draggingActive = { true },
        onPressed = { startCords = Point2D(window().x, window().x) },
        relocateBy = { _, _, _, dx, dy ->
            window().x = startCords.x + dx
            window().y = startCords.y + dy
        })
}
