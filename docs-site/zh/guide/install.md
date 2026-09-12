# 安装

只加一个依赖。Gradle 会解析对应平台变体。

```kotlin
implementation("io.github.lumkit:sweeteditor-compose:0.1.3")
```

## 宿主版本

宿主**必须**使用下面这组（或与之匹配的更新组合）：

| 项 | 版本 |
|---|---|
| Compose Multiplatform | 1.12.0 |
| Kotlin | 2.4.20 |
| Android `compileSdk` | 37 |

`targetSdk` 和 `minSdk` 可以更低。库的 `minSdk` 是 24。

更旧的 Compose Gradle 插件会带上旧版 Skiko，缺少 `Paragraph.nGetUnresolvedCodepointsCount`，JS / Wasm 运行时会崩溃。

## 各平台产物

| 平台 | 产物 | 内含 native |
|---|---|---|
| Android | `sweeteditor-compose-android`（传递 `…-android-jni`） | 各 ABI 的 `libsweeteditor.so` + `libsweeteditor_compose.so` |
| 桌面 JVM | `sweeteditor-compose-jvm` | `/native/<os>-<arch>/` 下的 Core 与 compose JNI |
| iOS | `iosarm64` / `iosSimulatorArm64` | cinterop 静态链入 `libsweeteditor.a` |
| Web | `js` / `wasm-js` | Compose 资源里的 C ABI 模块 |

不需要 XCFramework、JNA，也不要再手动加 native 链接参数。

## 仓库

Maven Central。若使用 `-SNAPSHOT` 版本，走 Central Portal 的 snapshots 仓库。

## 下一步

[快速开始](./quick-start.md)
