# Hello World

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

`EditorTheme` lives in `io.github.lumkit.sweeteditor` (`EditorTheme.dark()` / `light()`). Import the package that your IDE resolves; the type is public on the library.

## Ready

Native load is asynchronous on Web. Commands issued before the session is ready are ignored. Getters return defaults.

```kotlin
controller.whenReady {
    controller.loadDocument("print('hello')")
}
```

`isReady` is `true` after the composable has created the native editor.

## Own the controller

If the controller must outlive a single composition (tabs, ViewModel):

```kotlin
val controller = remember { SweetEditorController(initialText = "") }
```

`rememberSweetEditorController` is `remember { SweetEditorController(initialText) }`.

Do not attach the same controller instance to two `SweetEditor` composables. `attach` checks and throws.

## Next

[Architecture](./architecture.md) · [SweetLine highlight](./sweetline.md) · [SweetEditor API](/api/sweet-editor)
