# 发布物与 Maven 坐标（P3-11）

坐标前缀：`io.github.lumkit`。当前版本：`0.1.0-SNAPSHOT`。许可证：AGPL-3.0。

本仓库 **不向 Maven Central 推送**（无 Sonatype 凭据任务）。发布目标：

- `./gradlew :editor:publish :editor-android-jni:publish` → `build/maven/`
- `./gradlew :editor:publishToMavenLocal :editor-android-jni:publishToMavenLocal` → `~/.m2`

接入方依赖 **KMP 元数据坐标**，不要拆开只引某一个平台 JAR/AAR 却漏掉 native：

```kotlin
implementation("io.github.lumkit:sweeteditor-compose:0.1.0-SNAPSHOT")
```

| 坐标 | 内容 |
|---|---|
| `io.github.lumkit:sweeteditor-compose` | KMP metadata |
| `…:sweeteditor-compose-android` | Android Kotlin + 传递依赖 JNI AAR |
| `…:sweeteditor-compose-android-jni` | `libsweeteditor.so` + `libsweeteditor_compose.so`（`arm64-v8a` / `x86_64`） |
| `…:sweeteditor-compose-jvm` | JVM 类 + 资源 `/native/<os>-<arch>/`（Core 与 compose JNI） |
| `…:sweeteditor-compose-iosarm64` / `iosSimulatorArm64` | klib；cinterop **静态**链入 `libsweeteditor.a` |
| `…:sweeteditor-compose-js` / `wasm-js` | JS/Wasm + `/native/web/` 下的 C ABI 模块 |

没有单独的 **XCFramework** 坐标。Compose Multiplatform 宿主走上述 iOS klib，不要再链一套 Core。

## 禁止

接入方 **不得**：

- 给 Xcode / ld 加 `-lsweeteditor` 或再链 `libsweeteditor.a` / `.dylib`
- 把 Core / JNI dylib、so 拷进自己的 `jniLibs` 或 `Frameworks`（与 AAR/JAR/klib 各一份会双载）
- 用 JNA / FFM 直调 `c_api.h`
- 混用不同一次构建的 header 与 so（运行期 `UnsatisfiedLinkError`）

Android minify 时 AAR 已带 `consumer-rules.pro`（keep JNI 与 `HostTextMeasurer`）。

## 自检

发布到 `build/maven` 后：

- Android JNI AAR 内应有两套 ABI 的两个 `.so`
- JVM JAR 内应有 `native/macos-aarch64/`（或当前宿主目录）下的 Core 与 `libsweeteditor_compose`
- iOS klib 由 cinterop 带上 `staticLibraries = libsweeteditor.a`，宿主工程 **不再** 出现 SweetEditor 链接项
