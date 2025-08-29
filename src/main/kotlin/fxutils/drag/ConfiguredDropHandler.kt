package fxutils.drag

import javafx.scene.input.DataFormat
import javafx.scene.input.DragEvent
import javafx.scene.input.Dragboard
import javafx.scene.input.TransferMode
import java.io.File

open class ConfiguredDropHandler(setup: ConfiguredDropHandler.() -> Unit = {}) : DropHandler {
    private val handlers: MutableMap<DataFormat, DataFormatHandler> = mutableMapOf()
    private val fileHandlers: MutableMap<String, (DragEvent, List<File>) -> Boolean> = mutableMapOf()
    private val singleFileHandlers: MutableMap<String, (DragEvent, File) -> Boolean> = mutableMapOf()

    init {
        setup()
    }

    override fun acceptedTransferModes(event: DragEvent): Array<out TransferMode> {
        val files = event.dragboard.files
        return when (files.size) {
            0 -> {
                val acceptedFormat = event.dragboard.contentTypes.intersect(handlers.keys).firstOrNull()
                return if (acceptedFormat != null) handlers.getValue(acceptedFormat).acceptedModes(event)
                else emptyArray()
            }

            1 -> if (files[0].extension in singleFileHandlers.keys) arrayOf(TransferMode.COPY) else emptyArray()

            else -> {
                val extensions = files.mapTo(mutableSetOf(), File::extension)
                if (fileHandlers.keys.containsAll(extensions)) arrayOf(TransferMode.COPY)
                else emptyArray()
            }
        }
    }

    override fun drop(event: DragEvent): Boolean {
        val dragboard = event.dragboard
        for (contentType in dragboard.contentTypes) {
            if (contentType in handlers.keys) {
                val (_, handler) = handlers.getValue(contentType)
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

    fun handleFormat(
        format: DataFormat,
        acceptedModes: (DragEvent) -> Array<out TransferMode> = { arrayOf(TransferMode.COPY) },
        handler: (DragEvent, Dragboard) -> Boolean,
    ) {
        handlers[format] = DataFormatHandler(acceptedModes, handler)
    }

    fun handleFormat(
        format: DataFormat,
        vararg acceptedModes: TransferMode,
        handler: (DragEvent, Dragboard) -> Boolean,
    ) {
        handleFormat(format, { acceptedModes }, handler)
    }

    @JvmName("handleFormatCast")
    inline fun <reified T : Any> handleFormat(
        format: DataFormat,
        noinline acceptedModes: (DragEvent) -> Array<out TransferMode> = { arrayOf(TransferMode.COPY) },
        crossinline handler: (DragEvent, T) -> Boolean,
    ) {
        handleFormat(format, acceptedModes) { ev, dragboard ->
            val obj = dragboard.getContent(format) as? T
            obj?.let { handler(ev, it) } ?: false
        }
    }

    inline fun <reified T : Any> handleTypedFormat(
        format: TypedDataFormat<T>,
        noinline acceptedModes: (DragEvent) -> Array<out TransferMode> = { arrayOf(TransferMode.COPY) },
        crossinline handler: (DragEvent, T) -> Boolean,
    ) {
        handleFormat<T>(format as DataFormat, acceptedModes, handler)
    }

    inline fun <reified T : Any> handleTypedFormat(
        format: TypedDataFormat<T>,
        vararg acceptedModes: TransferMode,
        crossinline handler: (DragEvent, T) -> Boolean,
    ) {
        handleFormat<T>(format as DataFormat, { acceptedModes }, handler)
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

    private data class DataFormatHandler(
        val acceptedModes: (DragEvent) -> Array<out TransferMode>,
        val drop: (DragEvent, Dragboard) -> Boolean,
    )
}