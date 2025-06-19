package fxutils.drag

import javafx.scene.input.DataFormat
import javafx.scene.input.DragEvent
import javafx.scene.input.Dragboard
import java.io.File

open class ConfiguredDropHandler(setup: ConfiguredDropHandler.() -> Unit = {}) : DropHandler {
    private val handlers: MutableMap<DataFormat, (DragEvent, Dragboard) -> Boolean> = mutableMapOf()
    private val fileHandlers: MutableMap<String, (DragEvent, List<File>) -> Boolean> = mutableMapOf()
    private val singleFileHandlers: MutableMap<String, (DragEvent, File) -> Boolean> = mutableMapOf()

    init {
        setup()
    }

    override fun canDrop(event: DragEvent): Boolean {
        val formatAccepted = event.dragboard.contentTypes.intersect(handlers.keys).isNotEmpty()
        val files = event.dragboard.files
        val filesAccepted = when (files.size) {
            0 -> false
            1 -> files[0].extension in singleFileHandlers.keys
            else -> fileHandlers.keys.containsAll(files.mapTo(mutableSetOf(), File::extension))
        }
        return formatAccepted || filesAccepted
    }

    override fun drop(event: DragEvent): Boolean {
        val dragboard = event.dragboard
        for (contentType in dragboard.contentTypes) {
            if (contentType in handlers.keys) {
                val handler = handlers.getValue(contentType)
                if (handler(event, dragboard)) return true
            }
        }
        val files = dragboard.files
        when (files.size) {
            0 -> return false
            1 -> {
                val handler = singleFileHandlers[files[0].extension] ?: return false
                return handler(event, files[0])
            }

            else -> {
                val byExtension = files.groupBy(File::extension)
                if (!fileHandlers.keys.containsAll(byExtension.keys)) return false
                for ((extension, list) in byExtension) {
                    fileHandlers.getValue(extension).invoke(event, list)
                }
                return true
            }
        }
    }

    fun handleFormat(format: DataFormat, handler: (DragEvent, Dragboard) -> Boolean) {
        handlers[format] = handler
    }

    @JvmName("handleFormatCast")
    inline fun <reified T : Any> handleFormat(format: DataFormat, crossinline handler: (DragEvent, T) -> Boolean) {
        handleFormat(format) { ev, dragboard ->
            val obj = dragboard.getContent(format) as? T
            obj?.let { handler(ev, it) } ?: false
        }
    }

    inline fun <reified T : Any> handleTypedFormat(
        format: TypedDataFormat<T>, crossinline handler: (DragEvent, T) -> Boolean,
    ) {
        handleFormat<T>(format as DataFormat, handler)
    }

    fun handleFiles(vararg extension: String, handler: (DragEvent, List<File>) -> Boolean) {
        for (ext in extension) {
            fileHandlers[ext] = { ev, files -> handler(ev, files) }
            singleFileHandlers[ext] = { ev, file -> handler(ev, listOf(file)) }
        }
    }

    fun handleSingleFile(vararg extensions: String, handler: (DragEvent, File) -> Boolean) {
        for (ext in extensions) {
            singleFileHandlers[ext] = handler
        }
    }
}