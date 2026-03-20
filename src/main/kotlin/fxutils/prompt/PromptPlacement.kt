package fxutils.prompt

import fxutils.centerX
import fxutils.centerY
import javafx.event.Event
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.robot.Robot
import javafx.stage.Screen
import javafx.stage.Window

sealed class PromptPlacement {
    abstract val parentWindow: Window?

    abstract fun getPosition(content: Region): Point2D

    data class Centered(override val parentWindow: Window? = null) : PromptPlacement() {
        private val screen =
            if (parentWindow != null) getScreen(parentWindow.x, parentWindow.y)
            else Screen.getPrimary()

        override fun getPosition(content: Region): Point2D =
            Point2D(
                screen.visualBounds.centerX - content.width / 2,
                screen.visualBounds.centerY - content.height / 2
            )
    }

    data class At(
        val screenX: Double, val screenY: Double,
        override val parentWindow: Window?
    ) : PromptPlacement() {
        override fun getPosition(content: Region): Point2D {
            val screen = getScreen(screenX, screenY)
            val y = if (screenY + content.height <= screen.bounds.maxY) screenY
            else (screenY - content.height).coerceAtLeast(0.0)
            val x = screenX.coerceAtMost(screen.bounds.maxX - content.width)
            return Point2D(x, y)
        }
    }

    data class RelativeTo(val anchor: Region) : PromptPlacement() {
        override val parentWindow: Window? = anchor.scene.window

        override fun getPosition(content: Region): Point2D {
            val anchorPos = anchor.localToScreen(0.0, 0.0)
            val screen = getScreen(anchorPos.x, anchorPos.y)
            val x = anchorPos.x.coerceAtMost(screen.bounds.maxX - content.width)
            val anchorMaxY = anchorPos.y + anchor.height
            val availableHeightBelow = screen.bounds.height - anchorMaxY
            val y =
                if (content.height <= availableHeightBelow) anchorMaxY
                else anchorPos.y - content.height
            return Point2D(x, y)
        }
    }

    companion object {
        fun atMouseCoords(parentWindow: Window? = null): PromptPlacement {
            val pos = Robot().mousePosition
            return At(pos.x, pos.y, parentWindow)
        }
    }
}

private fun getScreen(screenX: Double, screenY: Double): Screen =
    Screen.getScreensForRectangle(screenX, screenY, 1.0, 1.0).firstOrNull() ?: Screen.getPrimary()

fun MouseEvent.atMouseCoords(): PromptPlacement = PromptPlacement.At(screenX, screenY, sourceWindow)

fun Event?.atMouseCoords(): PromptPlacement = PromptPlacement.atMouseCoords(this?.sourceWindow)

fun Event.nextToTarget(): PromptPlacement = PromptPlacement.RelativeTo(target as Region)

val Event.sourceWindow: Window?
    get() = when (val src = source) {
        is Node -> src.scene.window
        is Scene -> src.window
        is Window -> src
        else -> null
    }