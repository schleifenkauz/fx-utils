package fxutils

import javafx.scene.Node
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem

/**
 * Return a [MenuBar] configured with the given [builder].
 */
inline fun menuBar(builder: MenuBarBuilder.() -> Unit) = MenuBarBuilder().apply(builder).build()

inline fun menu(name: String, builder: MenuBuilder.() -> Unit) = MenuBuilder(name).apply(builder).build()

fun menuItem(name: String, graphic: Node? = null, action: () -> Unit): MenuItem {
    val item = MenuItem(name, graphic)
    item.setOnAction { action() }
    return item
}

/**
 * Builder class for [MenuBar]s
 */
class MenuBarBuilder {
    private val menus = mutableListOf<Menu>()

    /**
     * Add the given [menu]
     */
    fun menu(menu: Menu) {
        menus.add(menu)
    }

    /**
     * Add a menu with the given [name] and configure it with the given [block].
     */
    inline fun menu(name: String, block: MenuBuilder.() -> Unit) {
        menu(MenuBuilder(name).apply(block).build())
    }

    fun build() = MenuBar(*menus.toTypedArray())
}

/**
 * Builder class for [Menu]s
 */
class MenuBuilder(private val name: String) {
    private var items = mutableListOf<MenuItem>()

    /**
     * Add an item with the specified [name] and [shortcut], which executes the given [action] when clicked.
     */
    fun item(name: String, shortcut: Shortcut? = null, action: () -> Unit) {
        val item = MenuItem(name)
        item.accelerator = shortcut?.toCombination()
        item.setOnAction { action() }
        items.add(item)
    }

    /**
     * Add an item with the specified [name] and [shortcut], which executes the given [action] when clicked.
     */
    fun item(name: String, shortcut: String, action: () -> Unit) {
        item(name, shortcut.shortcut, action)
    }

    fun build() = Menu(name, null, *items.toTypedArray())
}