# 发布物与 Maven 坐标

坐标：`io.github.lumkit:sweeteditor-compose`。版本见根目录 `gradle.properties` 的 `VERSION_NAME`（当前 SNAPSHOT 可发到 Central Snapshots；正式版必须去掉 `-SNAPSHOT`）。许可证：AGPL-3.0。

接入方只声明 **KMP 元数据坐标**：

```kotlin
implementation("io.github.lumkit:sweeteditor-compose:<version>")
```

Gradle 会解析 Android / JVM / iOS / JS / Wasm 变体。Android 变体 **传递依赖** `sweeteditor-compose-android-jni`（Core `.so` + `libsweeteditor_compose.so`）。不要再拆开只引某一个平台 JAR/AAR。

| 坐标 | 内容 |
|---|---|
| `io.github.lumkit:sweeteditor-compose` | KMP metadata |
| `…:sweeteditor-compose-android` | Android Kotlin + 传递 JNI AAR |
| `…:sweeteditor-compose-android-jni` | `libsweeteditor.so` + `libsweeteditor_compose.so`（`arm64-v8a` / `x86_64`） |
| `…:sweeteditor-compose-jvm` | JVM 类 + `/native/<os>-<arch>/`（Core **与** compose JNI） |
| `…:sweeteditor-compose-iosarm64` / `iosSimulatorArm64` | klib；cinterop **静态**链入 `libsweeteditor.a` |
| `…:sweeteditor-compose-js` / `wasm-js` | `/native/web/` 下的 C ABI 模块 |

没有单独的 XCFramework 坐标。宿主不要再链一套 Core。

## 本地仓库

不需要 Central 凭据：

```bash
export SWEETEDITOR_HOME=../SweetEditor   # 默认也是这个相对路径
./editor/scripts/prepare-release-natives.sh --host   # 本机 Core + compose JNI
./gradlew :editor:publishToMavenLocal :editor-android-jni:publishToMavenLocal
```

- `./gradlew :editor:publish :editor-android-jni:publish` → `build/maven/`
- SNAPSHOT 在 natives 不齐时 **警告** 仍可发到 local；非 SNAPSHOT 或缺 `-Psweeteditor.publish.requireCompleteNatives=true` 时 `verifyReleaseNatives` **失败**

Android JNI AAR 需要 `editor/natives/android/<abi>/libsweeteditor.so`。本机没有 NDK 时不要发 Android 变体，或先跑 `prepare-release-natives.sh --android`。

## Maven Central

使用 [Nmcp](https://github.com/GradleUp/nmcp) 走 Central Portal Publisher API（不是已关停的 OSSRH）。

1. 在 [central.sonatype.com](https://central.sonatype.com/) 认领 `io.github.lumkit`，生成 user token。
2. 准备 ASCII-armored GPG 私钥（Central 要签名）。
3. 环境变量（不要写进仓库）：

| 变量 | 含义 |
|---|---|
| `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` | Portal token（也可用 `mavenCentralUsername` Gradle 属性） |
| `MAVEN_CENTRAL_PUBLISHING_TYPE` | `USER_MANAGED`（默认，Portal 上手动 Release）或 `AUTOMATIC` |
| `SIGNING_KEY` | GPG 私钥全文；CI 里可把换行写成 `\n` |
| `SIGNING_PASSWORD` | 私钥口令 |

```bash
# 先用 CI 或矩阵机把 editor/natives 凑齐，再：
./gradlew :editor:verifyReleaseNatives
./gradlew nmcpPublishAggregationToCentralPortal
```

SNAPSHOT 走 `nmcpPublishAggregationToCentralPortalSnapshots`。

GitHub Actions：`.github/workflows/publish.yml`（`workflow_dispatch` 或 tag `v*`）。仓库 Secrets：`MAVEN_CENTRAL_USERNAME`、`MAVEN_CENTRAL_PASSWORD`、`SIGNING_KEY`、`SIGNING_PASSWORD`。勾选 “Publish to Maven Central” 才会上传；否则只构建并上传 `build/maven` artifact。

## 二进制从哪来

`editor/natives/` 是打包源（Core 预编译 + 本仓库 compose JNI）。脚本：

| 脚本 | 作用 |
|---|---|
| `editor/scripts/prepare-release-natives.sh` | 按本机能力编排（`--host` / `--macos-x86_64` / `--ios` / `--android` / `--wasm`） |
| `build-host-core.sh` | 当前桌面 Core → `natives/desktop/<os>-<arch>/` |
| `build-desktop-jni.sh` | `libsweeteditor_compose` 安装到同一目录 |
| `build-ios-static-core.sh` | `natives/ios/*/libsweeteditor.a` |
| `build-android-core.sh` | `natives/android/<abi>/libsweeteditor.so` |
| `build-web-c-abi.sh` | `natives/web/sweeteditor_c_abi.{js,wasm}` |

CI 在 macOS / Linux / Linux ARM / Windows 上分别构建后汇合，再在 macOS 上跑 KMP `publish`（iOS cinterop 需要 Xcode）。

## 禁止

接入方 **不得**：

- 给 Xcode / ld 加 `-lsweeteditor` 或再链 `libsweeteditor.a` / `.dylib`
- 把 Core / JNI 再拷进自己的 `jniLibs` 或 `Frameworks`（会双载）
- 用 JNA / FFM 直调 `c_api.h`
- 混用不同一次构建的 header 与 so

Android minify 时 AAR 已带 `consumer-rules.pro`。
