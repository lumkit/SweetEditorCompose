# Compose 接入对照 SE 标准（P3-10）

对照 [SweetEditor 接入实现标准](https://github.com/FinalScave/SweetEditor/blob/main/docs/zh/platform-implementation-standard.md) **§1.1 / §1.2 / §3.2**（并带上声明式载体 **§3.0**）。  
实现：`:editor`（`io.github.lumkit.sweeteditor`）。日期：2026-09-11。

约束级别按标准：MUST 未覆盖视为缺口；SHOULD 未做须写理由；MAY 可省略。

---

## 结论

声明式载体（`SweetEditor` 拥有 session，`SweetEditorController` 只转发、`whenReady`、不持有 `EditorCore`）已对齐 **§3.0**。

**§1.1 / §1.2 / §3.2** 宿主可见 API、点击/手势事件、文档加载与补全插入语义已补齐。下列项仍是有意偏离或 MAY 省略，不是未完成功能。

---

## §3.0 声明式载体

| 规则 | 状态 |
|---|---|
| `SweetEditorController` 为宿主命令入口 | 已实现 |
| `SweetEditor(controller, …)` | 已实现 |
| Session / `EditorCore` 在 Composable，不在 Controller | 已实现 |
| 同一 Controller 不可绑两个 editor | `attach` 会 `check` |
| `whenReady`；ready 前变更忽略、getter 给默认值 | 已实现 |
| Controller `dispose()` 只清回调，不 `free_editor` | 已实现 |
| `getDocument()` / `loadDocument` | 已实现（`EditorDocument` 为文本快照，不外泄 native 句柄） |
| `theme` / `settings` / `keyMap` 作 composable 配置输入 | 已实现；Controller 另有 `applyTheme` / `getTheme` / `setSettings` / `getSettings` / `setKeyMap` / `getKeyMap` |

---

## §1.1 Core 层

协议类型在 `core/protocol/GeneratedProtocol.kt`（`internal`）。宿主可见的几何/装饰有一份 public 镜像。`EditorCore` / `Document` 为 `internal`，符合「3.1 默认不进 Controller」。

**生成器偏离（书面）：** 本仓库用 Kotlin 生成器读同一份 `schema.snapshot.json`，不改 SE 生成器。

**命名偏离：** 若干公共类型加了 `Editor` 前缀。

**3.1 仍只在 Core/JNI、不进 Controller 的视觉/IME 配置：** `setHandleConfig`、`setScrollbarConfig`、`setContentStartPadding`、`setShowSplitLine`、`getLayoutMetrics`、`moveCursor*`、`applyImeTextUpdates`（IME 走 command session）、`isReadOnly` / `getAutoIndentMode` 查询（由 `EditorSettings` 持有）。

---

## §1.2 Widget 层

选区菜单、右键菜单、Copilot、Decoration / Completion / NewLine 已实现。`CompletionResult.isIncomplete` 已加。`DecorationProvider.getCapabilities()` 为 `capabilities()` 的别名。`setCompletionItemRenderer` 已接。Perf overlay（MAY）未做。

### 事件（§1.2 / §11）

`TextChanged` / `CursorChanged` / `SelectionChanged` / `ScrollChanged` / `ScaleChanged`、`ContextMenu*`、`SelectionMenuItemClick`、`LinkClick`、`DocumentLoaded`、`FoldToggle`、`GutterIconClick` / `InlayHintClick` / `CodeLensClick`、`LongPress`、`DoubleTap` 均已分发。

---

## §3.2 宿主可见 API

文档：`loadDocument` / `getDocument`。外观：`applyTheme` / `getTheme` / `setSettings` / `getSettings` / `setKeyMap` / `getKeyMap`。编辑：`insertText` / `insertTextAt` / `replaceText` / `deleteText` / `applyTextEdits`。撤销：`undo` / `redo` / `canUndo` / `canRedo`。光标选区：`setCursorPosition` / `getCursorPosition` / `setSelection` / `getSelection` / `getWordRangeAtCursor` / `getWordAtCursor` / `selectAll`。导航：`gotoPosition` / `scrollToLine` / `setScroll` / `ensureCursorVisible` / `getTotalLineCount`。剪贴板短名 `copy`/`cut`/`paste`，并提供 `copyToClipboard` 等别名。

---

## 仍列出的偏离

| 项 | 级别 | 现状 | 理由 |
|---|---|---|---|
| `DecorationApplyMode` 非 `ApplyMode` | 命名 | 避免与 Compose 撞名 | 语义相同 |
| ContextMenu `LONG_PRESS` | SHOULD | 移动端不用 ContextMenu | 避免与选区菜单抢长按；原始长发 `LongPressEvent` |
| 选区菜单桌面关闭 | MAY | `platformSelectionMenuEnabled=false` | 改由右键菜单 |
| Web 无 `create_document_from_file` | 产品 | 有意 | 与 SE Web 子集一致 |
| 多光标 | — | 不做 | C API 无 |
| 语法/LSP 引擎 | — | 不做 | 始终走宿主 Provider |
| Perf overlay | MAY | 不做 | 调试用 |
| `getDocument` 不返回 native Document | 3.0 | `EditorDocument` 文本快照 | Controller 不持有 handle |

---

## 已对齐且不再列为偏离

- 补全无 `textEdit` 时在光标处插入；`SNIPPET` 走 `insertSnippet`
- `CompletionInsertTextFormat`：PLAIN_TEXT=1、SNIPPET=2
- 统一 `dispatchActionResult`；查询 API 不返回 `EditorActionResult`
- Range effect 绘制顺序；选区菜单生命周期；内联建议关闭规则
- iOS Core 以静态 `.a` 打进 klib；Android/JVM 自带 so；Web C ABI
- Maven 坐标见 [PUBLISHING.md](./PUBLISHING.md)
