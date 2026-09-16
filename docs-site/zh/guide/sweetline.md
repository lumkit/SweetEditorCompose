# SweetLine 高亮

`io.github.lumkit:sweetline-compose` 封装 [SweetLine](https://github.com/FinalScave/SweetLine)，通过 `DecorationProvider` 写进编辑器。这是**可选**模块；editor 本身没有词法器。

每个 `SweetLineHighlight` 实例只能 `bind` **一次**。编辑器离开组合时调用 `close()`（或放在 `DisposableEffect` 里）。

```kotlin
implementation("io.github.lumkit:sweeteditor-compose:0.1.4")
implementation("io.github.lumkit:sweetline-compose:0.1.4")
```

```kotlin
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.lumkit.sweeteditor.highlight.HighlightDocumentDescriptor
import io.github.lumkit.sweeteditor.highlight.HighlightFeatureFlags
import io.github.lumkit.sweeteditor.highlight.HighlightTheme
import io.github.lumkit.sweeteditor.highlight.SweetLineHighlight
import io.github.lumkit.sweeteditor.highlight.SweetLineHighlightConfig

val highlight = remember {
    SweetLineHighlight(
        SweetLineHighlightConfig(
            document = HighlightDocumentDescriptor(fileName = "Main.kt"),
            features = HighlightFeatureFlags(
                syntaxHighlight = true,
                indentGuides = false,
                bracketGuides = false,
                matchedBrackets = false,
                rainbowBrackets = false,
            ),
        ),
    )
}

DisposableEffect(controller, highlight) {
    val binding = highlight.bind(controller)
    onDispose { binding.close() }
}
```

语言由 `fileName` 后缀、`languageId` 或 `syntaxName` 决定。绑定之后可用 `updateDocument`、`updateTheme`（`HighlightTheme.dark()` / `light()`）、`updateFeatures`，或 `registerSyntaxJson` 追加语法（例如 Lua）。

## 内置语法

库的 composeResources 带 18 份 JSON：Kotlin、Java、JavaScript、TypeScript、Python、JSON、XML、HTML、Markdown、C、C++、Rust、Go、TOML、YAML、Shell、Properties、Gradle。

Web 的 C ABI 从**库自己的** composeResources 加载，宿主不必改 `index.html`，也不必拷 wasm。

Android R8 keep 规则在 KMP AAR 和 JNI AAR 里。iOS 静态库 `libsweetline.a` 已带 `LC_LINKER_OPTION -liconv`，Xcode 不用再写。

手写 span 见 [语法高亮](./highlights.md)。
