package fxutils

import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle

class SubWindow(
    private val root: Parent,
    title: String,
    private val type: Type = Type.ToolWindow,
) : Stage() {
    init {
        this.title = title
        scene = Scene(root)
        scene.stylesheets.addAll(globalStylesheets)
        initWindowType()
        registerShortcuts()
        setOnShown {
            root.requestFocus()
        }
    }

    private fun initWindowType() {
        when (type) {
            Type.Popup -> {
                var focusTimestamp = 0L
                focusedProperty().addListener { _, _, hasFocus ->
                    if (hasFocus) {
                        focusTimestamp = System.currentTimeMillis()
                    } else if (System.currentTimeMillis() - focusTimestamp > 100) { //ugly hack...
                        hide()
                    }
                }
                initModality(Modality.WINDOW_MODAL)
                initStyle(StageStyle.TRANSPARENT)
            }

            Type.Prompt -> {
                initModality(Modality.WINDOW_MODAL)
                initStyle(StageStyle.TRANSPARENT)
            }

            Type.ToolWindow -> {
                initStyle(StageStyle.DECORATED)
            }

            Type.Undecorated -> {
                initStyle(StageStyle.TRANSPARENT)
            }
        }
    }

    private fun registerShortcuts() {
        scene.registerShortcuts {
            if (type in setOf(Type.Popup, Type.Prompt)) {
                on("ESCAPE") { hide() }
            } else {
                on("Ctrl+W") { hide() }
            }
        }
    }

    fun showOrBringToFront() {
        if (!isShowing) show()
        else toFront()
    }

    enum class Type {
        Popup, Undecorated, ToolWindow, Prompt;
    }

    companion object {
        val globalStylesheets = mutableListOf<String>()
    }
}