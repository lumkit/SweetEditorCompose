# SweetEditor Compose

基于 [SweetEditor](https://github.com/FinalScave/SweetEditor) C++ 核心的 Compose Multiplatform 代码编辑器。

支持 **Android**、**iOS**、**桌面（JVM）**、**Web（JS + Wasm）** —— 一套 API，一个依赖。

## 引入

```kotlin
implementation("io.github.lumkit:sweeteditor-compose:0.1.1")
```

宿主必须使用 **Compose Multiplatform 1.12.0** 和 **Kotlin 2.4.20**（或与之匹配的更新版本）。更旧的 Compose Gradle 插件会带上旧版 Skiko，缺少 `Paragraph.nGetUnresolvedCodepointsCount`，JS / Wasm 运行时会直接崩溃。

Gradle 会自动解析对应平台变体：

| 平台 | 产物 | 内含 native |
|---|---|---|
| Android | `sweeteditor-compose-android`（传递 `…-android-jni`） | 各 ABI 的 `libsweeteditor.so` + `libsweeteditor_compose.so` |
| 桌面 JVM | `sweeteditor-compose-jvm` | `/native/<os>-<arch>/` 下的 Core 与 compose JNI |
| iOS | `sweeteditor-compose-iosarm64` / `iosSimulatorArm64` | cinterop 静态链入 `libsweeteditor.a` |
| Web | `sweeteditor-compose-js` / `wasm-js` | `/native/web/` 下的 C ABI 模块 |

不需要 XCFramework、不需要 JNA、不需要手动链 native —— 产物自带。

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

## 功能

- 语法高亮、括号匹配、自动闭合配对
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

**Android** —— minSdk 24。AAR 自带 `consumer-rules.pro` 保留 JNI 入口，无需额外 keep 规则。

**桌面** —— JVM 11+。JAR 首次加载时把 Core 与 compose JNI 解压到用户缓存目录。Compose Desktop R8/ProGuard 会自动读 JAR 里的 `META-INF/com.android.tools/r8/`；若用自带 ProGuard 任务，把 `editor/consumer-rules.pro` 加进 `buildTypes.release.proguard.configurationFiles`。

**iOS** —— cinterop 静态链 `libsweeteditor.a`；宿主 Xcode 工程**不得**加 `-lsweeteditor` 或链任何 SweetEditor dylib。

**Web** —— JS 与 Wasm 从 JAR 资源加载 C ABI 模块。

## 从源码构建

C++ 核心在同级的 [FinalScave/SweetEditor](https://github.com/FinalScave/SweetEditor) 检出里：

```
SweetEditor/            # C++ 核心（同级目录）
SweetEditorCompose/     # 本仓库
```

为当前宿主构建 native 库：

```bash
./editor/scripts/prepare-release-natives.sh --host          # 当前 OS+架构
./editor/scripts/prepare-release-natives.sh --macos-x86_64   # macOS 跨架构
./editor/scripts/prepare-release-natives.sh --ios            # iOS 静态库
./editor/scripts/prepare-release-natives.sh --android        # Android .so（需 NDK）
./editor/scripts/prepare-release-natives.sh --wasm          # Wasm C ABI（需 emsdk）
```

然后：

```bash
./gradlew :editor:publishToMavenLocal :editor-android-jni:publishToMavenLocal
```

## 文档

- [PUBLISHING.md](./PUBLISHING.md) —— Maven 坐标、native 打包、Central Portal 配置
- [COMPLIANCE.md](./COMPLIANCE.md) —— 宿主 API 对照 SE 接入标准的覆盖情况

## 许可证

[GNU Affero General Public License v3.0](./LICENSE)

[English](./README.md)
