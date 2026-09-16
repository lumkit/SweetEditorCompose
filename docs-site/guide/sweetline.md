# SweetLine highlight

`io.github.lumkit:sweetline-compose` wraps [SweetLine](https://github.com/FinalScave/SweetLine) and drives the editor through `DecorationProvider`. It is optional. The editor itself has no lexer.

`SweetLineHighlight.bind` may run **once** per instance. Call `close()` (or unbind in `DisposableEffect`) when the editor leaves composition.

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

Language is chosen from `fileName` suffix, `languageId`, or `syntaxName`. After bind, update with `updateDocument`, `updateTheme` (`HighlightTheme.dark()` / `light()`), `updateFeatures`, or `registerSyntaxJson` for extra grammars (for example Lua).

## Built-in grammars

Eighteen JSON files ship in the library compose resources: Kotlin, Java, JavaScript, TypeScript, Python, JSON, XML, HTML, Markdown, C, C++, Rust, Go, TOML, YAML, Shell, Properties, Gradle.

Web loads the C ABI from the **library** compose resources. Do not add a script tag or copy wasm into the host `index.html`.

Android R8 keep rules ship in the AAR and the JNI AAR. iOS static `libsweetline.a` already requests `-liconv`; do not add it in Xcode.

See also [manual StyleSpan highlights](./highlights.md).
