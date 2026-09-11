# Compose 接入对照 SE 标准（P3-10）

对照 [SweetEditor 接入实现标准](https://github.com/FinalScave/SweetEditor/blob/main/docs/zh/platform-implementation-standard.md) **§1.1 / §1.2 / §3.2**（并带上声明式载体 **§3.0**）。  
实现：`:editor`（`io.github.lumkit.sweeteditor`）。日期：2026-09-11。

约束级别按标准：MUST 未覆盖视为缺口；SHOULD 未做须写理由；MAY 可省略。

---

## 结论

声明式载体（`SweetEditor` 拥有 session，`SweetEditorController` 只转发、`whenReady`、不持有 `EditorCore`）已对齐 **§3.0**。

**§1.1 / §1.2** 逻辑分类齐：协议从本仓库 Kotlin 生成器读 SE `schema.snapshot.json`，不维护私有 wire schema。Widget 层含 Decoration / Completion / NewLine、选区菜单、右键菜单、内联建议。

**§3.2** 装饰写入/清理、折叠、查找、Provider、剪贴板、Copilot、选区/右键菜单已接。仍缺一批文档/光标/导航类宿主 API（见下表），以及若干点击类事件。这些是已知缺口，不是「做不到」，优先补 Controller 转发。

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
| `getDocument()` / `loadDocument(doc)` | **缺口**（仅 `initialText` 物化 `Document`） |
| `theme` / `settings` / `keyMap` 作 composable 配置输入 | 已实现 |

---

## §1.1 Core 层

协议类型在 `core/protocol/GeneratedProtocol.kt`（`internal`）。宿主可见的几何/装饰有一份 public 镜像（如 `TextPosition`、`StyleSpan`）。`EditorCore` / `Document` 为 `internal`，符合「3.1 默认不进 Controller」。

| 分类 | 状态 | 备注 |
|---|---|---|
| Core Bridge：`EditorCore`, `Document`, `CoreProtocol`, `TextMeasurer` | 有 | 测字为内部 `HostTextMeasurer`（标准允许 adapter） |
| Action | 有 | 公共 `EditorActionSource` / `TextChangeKind`；协议侧另有同名 `internal` 枚举 |
| Config | 有 | `EditorOptions` 等在生成协议内；部分只 internally 随 `createEditor` 走 |
| Foundation | 有 | 公共 `TextPosition`/`TextRange`/`TabStopGroup`；协议 `PointF`/`Rect`/`IntRange`/`TextEdit` 为 internal |
| Interaction | 有 | 协议 `GestureEvent` 等 internal；公共 `HitTarget` 给 ContextMenu 用 |
| IME | 有 | 协议类型齐全；Android/iOS 走 command session |
| Adornment | 有 | 公共装饰类型 + 协议编解码 |
| Visual | 有 | `EditorRenderModel` 等 internal，Canvas 消费 |
| Keymap | 有 | 协议 `KeyCode`/`EditorBuiltinCommand` internal；Widget `EditorKeyMap` 公共 |

**生成器偏离（书面）：** 标准写 MUST 用 SE `tools/se_protocol_gen`。本仓库用 **Kotlin 生成器**读同一份 `schema.snapshot.json`，不改 SE 生成器。字段顺序/宽度与 golden 对齐。这是本项目锁定决策，不是第二套 schema。

**命名偏离：** 若干公共类型加了 `Editor` 前缀以免与协议/Compose 撞名：`EditorSpanLayer`、`EditorTextStyle`、`EditorCursorRect`、`EditorSearchOptions`、`EditorKeyMap` 的 `EditorKeyChord`/`EditorKeyBinding`。语义与标准一致。

**`EditorCore` 3.1 未桥 JNI 的方法（内部缺口，宿主暂不可达）：**  
`deleteText`、`deleteForward`、`getLayoutMetrics`、`setHandleConfig`、`setScrollbarConfig`、`setContentStartPadding`、`setShowSplitLine`、`setCursorPosition`、`setSelection`/`getSelection`、`selectAll`（Core 侧）、`getWordAtCursor`、光标 `moveCursor*`、`scrollToLine`、`gotoPosition`、`ensureCursorVisible`、`setScroll`、`isReadOnly`/`getAutoIndentMode` 查询、`applyImeTextUpdates`。  
其中一部分由键盘/手势进 Core，或由 `EditorSettings` SideEffect 写入。`loadDocument` 在桥上叫 `setDocument`。

---

## §1.2 Widget 层

| 分类 | 状态 | 备注 |
|---|---|---|
| Widget：`SweetEditor`, `SweetEditorController`, `EditorTheme`, `EditorSettings`, `EditorIconProvider`, `EditorMetadata`, `LanguageConfiguration` | 有 | |
| Decoration：Provider / Manager / Context / Result / Type / Receiver | 有 | `getCapabilities()` 落成 `capabilities()`；`ApplyMode` 落成 `DecorationApplyMode` |
| Completion：Provider / Manager / Context / Item / Result / Receiver | 有 | `CompletionResult` **无** `isIncomplete` |
| Event + `EditorEventBus` | 部分 | 见事件表 |
| NewLine | 有 | |
| `EditorKeyMap` | 有 | `getKeyMap()` 未暴露（标准 SHOULD） |
| Copilot SHOULD | 有 | `InlineSuggestion` + Listener；Tab/Esc；phantom + 操作条 |
| Selection 移动端 SHOULD | 有 | Android/iOS 开；桌面/Web 关（标准 MAY 省略） |
| ContextMenu 桌面 SHOULD | 有 | JVM/Web 右键；Android/iOS 关。仅 `RIGHT_CLICK`，无 `LONG_PRESS` |
| Perf MAY | **未做** | 省略 `setPerfOverlayEnabled` |

### 事件（§1.2 / §11）

| 事件 | 状态 |
|---|---|
| `TextChangedEvent`（`changes`/`kind`/`source`） | 有 |
| `CursorChangedEvent` / `SelectionChangedEvent` / `ScrollChangedEvent` / `ScaleChangedEvent` | 有 |
| `ContextMenuEvent` / `ContextMenuItemClickEvent` / `SelectionMenuItemClickEvent` / `LinkClickEvent` | 有（后三个为目标特定） |
| `DocumentLoadedEvent` | **无** |
| `FoldToggleEvent` | **无** |
| `GutterIconClickEvent` / `InlayHintClickEvent` / `CodeLensClickEvent` | **无**（TAP 命中未转事件） |
| `LongPressEvent` | **无**（移动端 SHOULD 原始长按） |
| `DoubleTapEvent` | **无**（Core 有 `DOUBLE_TAP`，未发宿主事件） |

---

## §3.2 宿主可见 API（`SweetEditorController`）

已实现的能力族不逐条展开：行操作、撤销/重做、剪贴板（`copy`/`cut`/`paste`/`selectAll`，名称短于标准 `*ToClipboard`）、折叠、查找替换、语言/元数据、Provider add/remove、补全 trigger/show/dismiss、样式与几乎全部 decoration 写入/清理、guides、括号、diff、snippet/linked editing、inline suggestion、选区/右键菜单、`getVisibleLineRange`/`getScrollMetrics`/`getCursorRect`/`getPositionRect`/`getSelectedText`/`getCursorPosition`/`getLinkTargetAt`。

### 未暴露或名称不等价

| 标准 API | 状态 | 说明 |
|---|---|---|
| `loadDocument` / `getDocument` | 缺口 | 无换文档 API；Document 句柄不外泄 |
| `applyTheme` / `getTheme` | 偏离 | 主题走 `SweetEditor(theme=)`，无 getter |
| `getSettings` | 偏离 | 走 composable `settings` 参数 |
| `getKeyMap` | SHOULD 缺口 | 可 `setKeyMap` 等价：`SweetEditor(keyMap=)` |
| `insertTextAt` / `replaceText` / `deleteText` / `applyTextEdits` | 缺口 | 仅 `insertText`；补全内部会 `replaceText`/`applyTextEdits` |
| `canUndo` / `canRedo` | 缺口 | Core 有查询，Controller 未转发 |
| `setSelection` / `getSelection` / `setCursorPosition` | 缺口 | |
| `getWordRangeAtCursor` / `getWordAtCursor` | 缺口 | Core 有 word range |
| `gotoPosition` / `scrollToLine` / `setScroll` | 缺口 | |
| `setCompletionItemRenderer` | 缺口 | 补全列表固定绘制 |
| `getTotalLineCount` | 缺口 | DecorationContext 内部有行数 |
| `copyToClipboard` 等 | MAY 已做 | 方法名为 `copy`/`cut`/`paste` |

---

## 行为偏离（已实现模块）

| 项 | 级别 | 现状 | 理由 / 计划 |
|---|---|---|---|
| 补全无 `textEdit` 时用 word range 替换 | MUST §5 | `applyCompletionItem` 会 `getWordRangeAtCursor` | 应改为光标处插入；记为 bug |
| `CompletionInsertTextFormat` 枚举序 | MUST §5 | `PLAIN_TEXT` ordinal 0，标准值 1 | 仅内部枚举，未当 wire int 发出；若对外序列化需对齐 |
| `CompletionResult.isIncomplete` | MUST §4.2 | 未做 | 单次 snapshot 够用，增量补全未做 |
| `DecorationProvider.getCapabilities` | MUST 命名 | `capabilities()` | Kotlin 属性风格，语义相同 |
| 点击装饰事件 | MUST §11 | 未从 TAP `hitTarget` 分发 | 与 §3.2 查询已有 `getLinkTargetAt` 不对称 |
| ContextMenu `LONG_PRESS` | SHOULD | 移动端不用 ContextMenu | 避免与选区菜单抢长按；桌面只右键 |
| 选区菜单桌面关闭 | MAY | `platformSelectionMenuEnabled=false` | 改由右键菜单 |
| Web 无 `create_document_from_file` | 产品 | 有意 | 与 SE Web 子集一致 |
| 多光标 | — | 不做 | C API 无 |
| 语法/LSP 引擎 | — | 不做 | 始终走宿主 Provider |
| Perf overlay | MAY | 不做 | 调试用，未排期 |
| Maven/XCFramework 坐标 | P3-11 | 本地 Maven 已接线 | 见 [PUBLISHING.md](./PUBLISHING.md)；无独立 XCFramework，iOS 走 klib 内嵌 `.a` |

---

## 已对齐且不再列为偏离

- 统一 `dispatchActionResult`；查询 API 不返回 `EditorActionResult`；`isInLinkedEditing()` 为查询
- Range effect：背景在文字下，边框/下划线在文字与 guide 后、光标前；不画 range foreground
- 选区菜单生命周期 HIDDEN / PENDING_SHOW / VISIBLE / SUSPENDED；与 ContextMenu 互斥
- 内联建议：文本/光标关闭、滚动只重定位；替换当前建议静默；dispose 不再回调
- iOS Core 以静态 `.a` 打进 klib；Android/JVM 自带 so；Web 用 Emscripten 已导出 C ABI
