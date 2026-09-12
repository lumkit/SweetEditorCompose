# Menus

Two host-owned menus sit on top of the editor.

## Selection menu

Shown after a selection gesture (typically mobile). Provide items, then handle clicks.

```kotlin
controller.setSelectionMenuItemProvider { context ->
    buildList {
        if (context.hasSelection) {
            add(SelectionMenuItem(SelectionMenuItem.ACTION_CUT, "Cut"))
            add(SelectionMenuItem(SelectionMenuItem.ACTION_COPY, "Copy"))
        }
        add(SelectionMenuItem(SelectionMenuItem.ACTION_PASTE, "Paste"))
        add(SelectionMenuItem(SelectionMenuItem.ACTION_SELECT_ALL, "Select all"))
    }
}

controller.onSelectionMenuItemClick { event ->
    when (event.itemId) {
        SelectionMenuItem.ACTION_CUT -> controller.cut()
        SelectionMenuItem.ACTION_COPY -> controller.copy()
        SelectionMenuItem.ACTION_PASTE -> controller.paste()
        SelectionMenuItem.ACTION_SELECT_ALL -> controller.selectAll()
        SelectionMenuItem.ACTION_DELETE -> {
            val sel = controller.getSelection() ?: return@onSelectionMenuItemClick
            controller.deleteText(sel.start, sel.end)
        }
    }
}
```

Built-in ids: `cut`, `copy`, `delete`, `paste`, `select_all`.

## Context menu

Right-click (desktop) or long-press (touch). The request includes hit target and optional link.

```kotlin
controller.setContextMenuItemProvider { request ->
    listOf(
        ContextMenuSection(
            listOf(
                ContextMenuItem(ContextMenuItem.ACTION_COPY, "Copy"),
                ContextMenuItem(ContextMenuItem.ACTION_PASTE, "Paste"),
            ),
        ),
    )
}

controller.onContextMenu { /* opened */ }
controller.onContextMenuItemClick { event ->
    when (event.item.id) {
        ContextMenuItem.ACTION_COPY -> controller.copy()
        ContextMenuItem.ACTION_PASTE -> controller.paste()
        ContextMenuItem.ACTION_OPEN_LINK -> { /* event.request.linkTarget */ }
    }
}

controller.dismissContextMenu()
controller.isContextMenuShowing
```

`onLinkClick` fires when a [LinkSpan](/api/overlays) is activated.

Pass `null` to either provider to remove the custom menu.
