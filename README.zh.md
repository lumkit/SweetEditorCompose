# SweetEditor Compose

基于 [SweetEditor](https://github.com/FinalScave/SweetEditor) C++ 核心的 Compose Multiplatform 代码编辑器。语法高亮是可选模块，内核是 [SweetLine](https://github.com/FinalScave/SweetLine)。

支持 **Android**、**iOS**、**桌面（JVM）**、**Web（JS + Wasm）**。

## 引入

```kotlin
implementation("io.github.lumkit:sweeteditor-compose:0.1.4")
implementation("io.github.lumkit:sweetline-compose:0.1.4") // 可选，SweetLine 高亮
```

宿主必须使用 **Compose Multiplatform 1.12.0**、**Kotlin 2.4.20**，以及 Android **compileSdk 37**（或与之匹配的更新组合）。`targetSdk` / `minSdk` 可以更低。更旧的 Compose Gradle 插件会带上旧版 Skiko，缺少 `Paragraph.nGetUnresolvedCodepointsCount`，JS / Wasm 运行时会直接崩溃。

Gradle 会自动解析对应平台变体：

| 平台 | 产物 | 内含 native |
|---|---|---|
| Android | `sweeteditor-compose-android`（传递 `…-android-jni`） | 各 ABI 的 `libsweeteditor.so` + `libsweeteditor_compose.so` |
| 桌面 JVM | `sweeteditor-compose-jvm` | `/native/<os>-<arch>/` 下的 Core 与 compose JNI |
| iOS | `sweeteditor-compose-iosarm64` / `iosSimulatorArm64` | cinterop 静态链入 `libsweeteditor.a` |
| Web | `sweeteditor-compose-js` / `wasm-js` | `/native/web/` 下的 C ABI 模块 |

`sweetline-compose` 布局相同（`libsweetline` / `sweetline_c_abi`，外加 18 份语法 JSON）。不需要 XCFramework、JNA、宿主 `index.html` 脚本，也不需要在 Xcode 里加 `-liconv`。

## 快速开始

```kotlin
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.SweetEditorController
import io.github.lumkit.sweeteditor.theme.EditorTheme

@Composable
fun CodeScreen() {
    val controller = remember { SweetEditorController(initialText = "fun main() { }") }

    SweetEditor(
        modifier = Modifier.fillMaxSize(),
        controller = controller,
        theme = EditorTheme(),
    )
}
```

`SweetEditorController` 是宿主命令入口。Session 和 native `EditorCore`
在 `SweetEditor` Composable 内部 —— Controller 只转发命令，不持有 native 句柄。

```kotlin
controller.whenReady {
    controller.loadDocument("print('hello')")
    controller.insertText("world")
    controller.applyTheme(EditorTheme(dark = true))
    controller.canUndo()
    controller.setSelection(0, 0, 0, 5)
}
```

## SweetLine 高亮

```kotlin
import androidx.compose.runtime.DisposableEffect
import io.github.lumkit.sweeteditor.highlight.HighlightDocumentDescriptor
import io.github.lumkit.sweeteditor.highlight.SweetLineHighlight
import io.github.lumkit.sweeteditor.highlight.SweetLineHighlightConfig

val highlight = remember {
    SweetLineHighlight(
        SweetLineHighlightConfig(
            document = HighlightDocumentDescriptor(fileName = "Main.kt"),
        ),
    )
}
DisposableEffect(controller, highlight) {
    val binding = highlight.bind(controller)
    onDispose { binding.close() }
}
```

每个实例只能 `bind` 一次。说明见 [文档站 — SweetLine](https://lumkit.github.io/SweetEditorCompose/zh/guide/sweetline)。

## 功能

- 语法高亮：可选 `sweetline-compose`，或自己推 `StyleSpan`
- 搜索替换、差异渲染、折叠区域
- IME 输入法合成（Android、iOS、桌面、Web）
- 代码片段插入与联动编辑（Tab Stop）
- 补全弹窗，支持 `textEdit` / `insertText` / snippet 语义
- Copilot 风格内联建议
- 移动端选区菜单、桌面右键菜单
- 自定义装饰、Inlay Hint、CodeLens、行号区点击事件
- 主题、设置、键位映射作为 Composable 配置输入
- 自带 ProGuard / R8 混淆规则（Android AAR + JVM JAR）

## 平台说明

**Android** —— minSdk 24，compileSdk 37（Compose 1.12 的 AAR metadata 会强制检查）。AAR 自带 `consumer-rules.pro` 保留 JNI 入口，无需额外 keep 规则。

**桌面** —— JVM 11+。JAR 首次加载时把 Core 与 compose JNI 解压到用户缓存目录。JNI keep 规则打在 JAR 的 `META-INF/proguard/` 与 `META-INF/com.android.tools/r8/`。Android R8 会自动合并；Compose Desktop 自带的 ProGuard **不会**扫描依赖 JAR，若开启 release 混淆，把这些 `.pro` 加进 `configurationFiles`（或像本仓库 `desktopApp` 一样从 runtime classpath 收集）。

**iOS** —— cinterop 静态链 `libsweeteditor.a` / `libsweetline.a`。SweetLine 的静态库已带 `LC_LINKER_OPTION -liconv`，宿主 Xcode **不必**再加 `-liconv` 或 `-lsweeteditor`。

**Web** —— JS / Wasm 会自动加载 C ABI（Compose 资源目录
`/composeResources/…/files/`），宿主 `index.html` **不必**再手写
`<script src="sweeteditor_web_abi.js">`。Compose 必须是 1.12.0，否则 Skiko 对不上。

## 从源码构建

C++ 核心在同级目录：

```
SweetEditor/            # 编辑器 C++ 核心
SweetLine/              # 高亮 C++ 核心
SweetEditorCompose/     # 本仓库
```

为当前宿主构建 native 库：

```bash
./editor/scripts/prepare-release-natives.sh --host
./highlight/scripts/prepare-release-natives.sh --host
```

然后：

```bash
./gradlew \
  :editor:publishToMavenLocal :editor-android-jni:publishToMavenLocal \
  :highlight:publishToMavenLocal :highlight-android-jni:publishToMavenLocal
```

## 文档

- [使用文档站](https://lumkit.github.io/SweetEditorCompose/) —— 安装、快速开始与 public API（中 / 英）
- [PUBLISHING.md](./PUBLISHING.md) —— Maven 坐标、native 打包、Central Portal 配置
- [COMPLIANCE.md](./COMPLIANCE.md) —— 宿主 API 对照 SE 接入标准的覆盖情况

## 许可证

[GNU Affero General Public License v3.0](./LICENSE)

[English](./README.md)
