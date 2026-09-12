# 快速开始

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.rememberSweetEditorController
import io.github.lumkit.sweeteditor.EditorTheme

@Composable
fun CodeScreen() {
    val controller = rememberSweetEditorController(initialText = "fun main() {\n}\n")

    SweetEditor(
        modifier = Modifier.fillMaxSize(),
        controller = controller,
        theme = EditorTheme.dark(),
    )
}
```

`EditorTheme` 在 `io.github.lumkit.sweeteditor`（`EditorTheme.dark()` / `light()`）。

## Ready

Web 上 native 是异步加载的。Session 未就绪时发出的命令会被忽略，getter 返回默认值。

```kotlin
controller.whenReady {
    controller.loadDocument("print('hello')")
}
```

Composable 创建完 native 编辑器后 `isReady` 为 `true`。

## 自己持有 Controller

Controller 需要比单次组合更长寿（Tab、ViewModel）时：

```kotlin
val controller = remember { SweetEditorController(initialText = "") }
```

`rememberSweetEditorController` 就是 `remember { SweetEditorController(initialText) }`。

同一个 Controller 实例不能绑两个 `SweetEditor`。`attach` 会检查并抛错。

## 下一步

[架构约定](./architecture.md) · [SweetEditor API](/zh/api/sweet-editor)
