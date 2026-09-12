# 菜单

编辑器上层有两种由宿主提供的菜单。

## 选区菜单

选区手势后出现（多为移动端）。提供条目，再处理点击。

```kotlin
controller.setSelectionMenuItemProvider { context ->
    buildList {
        if (context.hasSelection) {
            add(SelectionMenuItem(SelectionMenuItem.ACTION_CUT, "剪切"))
            add(SelectionMenuItem(SelectionMenuItem.ACTION_COPY, "复制"))
        }
        add(SelectionMenuItem(SelectionMenuItem.ACTION_PASTE, "粘贴"))
        add(SelectionMenuItem(SelectionMenuItem.ACTION_SELECT_ALL, "全选"))
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

内置 id：`cut`、`copy`、`delete`、`paste`、`select_all`。

## 右键 / 长按菜单

桌面右键或触控长按。请求里带命中目标和可选链接。

```kotlin
controller.setContextMenuItemProvider { request ->
    listOf(
        ContextMenuSection(
            listOf(
                ContextMenuItem(ContextMenuItem.ACTION_COPY, "复制"),
                ContextMenuItem(ContextMenuItem.ACTION_PASTE, "粘贴"),
            ),
        ),
    )
}

controller.onContextMenu { /* 已打开 */ }
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

激活 [LinkSpan](/zh/api/overlays) 时会发 `onLinkClick`。

任一 Provider 传 `null` 即去掉自定义菜单。
