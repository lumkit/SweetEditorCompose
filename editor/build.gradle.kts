import org.gradle.api.file.DuplicatesStrategy
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

group = "io.github.lumkit"
version = "0.1.0-SNAPSHOT"
description = "Compose Multiplatform code editor backed by the SweetEditor C++ core"

// Reserved for Maven Central wiring. Do not add publish tasks yet.
extra["mavenArtifactId"] = "sweeteditor-compose"

val sweetEditorHome: File = resolveSweetEditorHome()
val nativesRoot: File = layout.projectDirectory.dir("natives").asFile
val generatedNatives: Provider<Directory> = layout.buildDirectory.dir("generated/natives")
val jvmNativeResourcesDir: Provider<Directory> = generatedNatives.map { it.dir("jvmResources") }
val webNativeResourcesDir: Provider<Directory> = generatedNatives.map { it.dir("webResources") }
val androidJniLibsDir: Directory = layout.projectDirectory.dir("src/androidMain/jniLibs")

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        configureSweetEditorCinterop(target)
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "io.github.lumkit.sweeteditor"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.compose.runtime)
                api(libs.compose.foundation)
                api(libs.compose.ui)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        val jniMain by creating {
            dependsOn(commonMain)
        }
        val stubNativeMain by creating {
            dependsOn(commonMain)
        }
        androidMain {
            dependsOn(jniMain)
            dependencies {
                implementation(project(":editor-android-jni"))
                implementation(libs.compose.uiToolingPreview)
            }
        }
        named("jvmMain") {
            dependsOn(jniMain)
            resources.srcDir(jvmNativeResourcesDir)
        }
        val iosMain by creating {
            dependsOn(commonMain)
            dependsOn(stubNativeMain)
        }
        named("iosArm64Main") {
            dependsOn(iosMain)
        }
        named("iosSimulatorArm64Main") {
            dependsOn(iosMain)
        }
        named("jsMain") {
            dependsOn(stubNativeMain)
            resources.srcDir(webNativeResourcesDir)
        }
        named("wasmJsMain") {
            dependsOn(stubNativeMain)
            resources.srcDir(webNativeResourcesDir)
        }
        named("jvmTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

val syncSweetEditorNatives by tasks.registering(Sync::class) {
    group = "sweeteditor"
    description =
        "Copy SweetEditor release prebuilts into editor/natives (packaging). Desktop run/test builds the host core with CMake instead."

    into(nativesRoot)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    preserve {
        include(".gitkeep")
        include("**/.gitkeep")
    }

    from(File(sweetEditorHome, "prebuilt/android")) {
        into("android")
        include("**/*.so")
    }
    from(File(sweetEditorHome, "prebuilt/ios/arm64")) {
        into("ios/arm64")
        include("*.dylib", "*.a")
    }
    from(File(sweetEditorHome, "prebuilt/ios/simulator-arm64")) {
        into("ios/simulator-arm64")
        include("*.dylib", "*.a")
    }
    from(File(sweetEditorHome, "prebuilt/macos/arm64")) {
        into("desktop/macos-aarch64")
        include("*.dylib")
    }
    from(File(sweetEditorHome, "prebuilt/macos/x86_64")) {
        into("desktop/macos-x86_64")
        include("*.dylib")
    }
    from(File(sweetEditorHome, "prebuilt/linux/x86_64")) {
        into("desktop/linux-x86_64")
        include("*.so")
    }
    from(File(sweetEditorHome, "prebuilt/linux/aarch64")) {
        into("desktop/linux-aarch64")
        include("*.so")
    }
    from(File(sweetEditorHome, "prebuilt/windows/x64")) {
        into("desktop/windows-x86_64")
        include("*.dll")
    }
    from(File(sweetEditorHome, "prebuilt/wasm")) {
        into("web")
        include("sweeteditor_c_abi.js", "sweeteditor_c_abi.wasm")
    }
    from(File(sweetEditorHome, "include/sweeteditor")) {
        into("include/sweeteditor")
        include("**/*.h", "**/*.hpp")
    }
}

val prepareAndroidJniLibs by tasks.registering(Sync::class) {
    group = "sweeteditor"
    description = "Stage Android JNI libraries under androidMain/jniLibs so they ship inside the AAR."
    into(androidJniLibsDir)
    from(resolveNativeSource("android", "prebuilt/android")) {
        include("**/*.so")
    }
}

val prepareJvmNativeResources by tasks.registering(Sync::class) {
    group = "sweeteditor"
    description = "Stage desktop native libraries as JVM JAR resources under /native/<os>-<arch>/."
    into(jvmNativeResourcesDir.map { it.dir("native") })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    from(resolveNativeSource("desktop/macos-aarch64", "prebuilt/macos/arm64")) {
        include("*.dylib")
        into("macos-aarch64")
    }
    from(resolveNativeSource("desktop/macos-x86_64", "prebuilt/macos/x86_64")) {
        include("*.dylib")
        into("macos-x86_64")
    }
    from(resolveNativeSource("desktop/linux-x86_64", "prebuilt/linux/x86_64")) {
        include("*.so")
        into("linux-x86_64")
    }
    from(resolveNativeSource("desktop/linux-aarch64", "prebuilt/linux/aarch64")) {
        include("*.so")
        into("linux-aarch64")
    }
    from(resolveNativeSource("desktop/windows-x86_64", "prebuilt/windows/x64")) {
        include("*.dll")
        into("windows-x86_64")
    }
}

val prepareWebNativeResources by tasks.registering(Sync::class) {
    group = "sweeteditor"
    description = "Stage WebAssembly C ABI modules as JS/Wasm resources under /native/web/."
    into(webNativeResourcesDir.map { it.dir("native/web") })
    from(resolveNativeSource("web", "prebuilt/wasm")) {
        include("sweeteditor_c_abi.js", "sweeteditor_c_abi.wasm")
    }
}

val generatedProtocolFile =
    layout.projectDirectory.file("src/commonMain/kotlin/io/github/lumkit/sweeteditor/core/protocol/GeneratedProtocol.kt")

val generateCoreProtocol by tasks.registering(Exec::class) {
    group = "sweeteditor"
    description = "Generate Kotlin CoreProtocol types from SE schema.snapshot.json"
    val schema = File(sweetEditorHome, "tools/se_protocol_gen/schema.snapshot.json")
    val generator = rootProject.file("tools/kotlin_protocol_gen/generate.py")
    inputs.file(schema)
    inputs.file(generator)
    outputs.file(generatedProtocolFile)
    commandLine(
        "python3",
        generator.absolutePath,
        "--schema",
        schema.absolutePath,
        "--out",
        generatedProtocolFile.asFile.absolutePath,
    )
    doFirst {
        check(schema.isFile) {
            "Missing protocol schema at ${schema.absolutePath}. Set sweetEditor.home."
        }
    }
}

val desktopJniBuildDir: Provider<Directory> = layout.buildDirectory.dir("jni/desktop")
val hostDesktopFolder: String = currentDesktopResourceFolder()
val jniBuildScript = layout.projectDirectory.file("scripts/build-desktop-jni.sh")
val hostCoreBuildScript = layout.projectDirectory.file("scripts/build-host-core.sh")
val hostCoreBuildDir = File(sweetEditorHome, "build/compose-host")

val buildHostSweetEditorCore by tasks.registering(Exec::class) {
    group = "sweeteditor"
    description =
        "CMake-build the SweetEditor C++ core for this desktop host (README local-compile flow) and install it into editor/natives/"
    environment("SWEETEDITOR_HOME", sweetEditorHome.absolutePath)
    inputs.dir(File(sweetEditorHome, "src"))
    inputs.dir(File(sweetEditorHome, "include"))
    inputs.file(File(sweetEditorHome, "CMakeLists.txt"))
    inputs.file(hostCoreBuildScript)
    outputs.dir(File(nativesRoot, "desktop/$hostDesktopFolder"))
    outputs.dir(File(nativesRoot, "include/sweeteditor"))
    outputs.dir(hostCoreBuildDir)
    commandLine("bash", hostCoreBuildScript.asFile.absolutePath, "all")
}

val configureDesktopJni by tasks.registering(Exec::class) {
    group = "sweeteditor"
    description = "Configure CMake for libsweeteditor_compose on the current desktop host"
    inputs.files(
        file("src/jni/CMakeLists.txt"),
        file("src/jni/sweeteditor_jni.cpp"),
        file("src/jni/sweeteditor_jni.h"),
        jniBuildScript,
    )
    outputs.dir(desktopJniBuildDir)
    dependsOn(buildHostSweetEditorCore)
    commandLine("bash", jniBuildScript.asFile.absolutePath, "configure")
}

val compileDesktopJni by tasks.registering(Exec::class) {
    group = "sweeteditor"
    description = "Build libsweeteditor_compose for the current desktop host"
    dependsOn(buildHostSweetEditorCore, configureDesktopJni)
    inputs.dir(desktopJniBuildDir)
    commandLine("bash", jniBuildScript.asFile.absolutePath, "build")
}

prepareJvmNativeResources {
    dependsOn(buildHostSweetEditorCore, compileDesktopJni)
    from(desktopJniBuildDir) {
        include("libsweeteditor_compose.dylib", "libsweeteditor_compose.so", "sweeteditor_compose.dll")
        include("Release/sweeteditor_compose.dll")
        into(hostDesktopFolder)
    }
}

tasks.configureEach {
    if (name.contains("ProcessResources")) {
        if (name.contains("jvm", ignoreCase = true)) {
            dependsOn(prepareJvmNativeResources)
        }
        if (name.contains("js", ignoreCase = true) || name.contains("wasm", ignoreCase = true)) {
            dependsOn(prepareWebNativeResources)
        }
    }
}

private fun resolveSweetEditorHome(): File {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    val configured = providers.gradleProperty("sweetEditor.home").orNull
        ?: localProperties.getProperty("sweetEditor.home")
        ?: "../SweetEditor"
    val configuredFile = File(configured)
    return if (configuredFile.isAbsolute) configuredFile else rootProject.file(configured)
}

private fun resolveNativeSource(vendoredRelative: String, prebuiltRelative: String): File {
    val vendored = File(nativesRoot, vendoredRelative)
    return if (vendored.exists()) vendored else File(sweetEditorHome, prebuiltRelative)
}

private fun configureSweetEditorCinterop(target: KotlinNativeTarget) {
    val archDir = when (target.name) {
        "iosArm64" -> "arm64"
        "iosSimulatorArm64" -> "simulator-arm64"
        else -> return
    }

    val includeDir = sequenceOf(
        File(nativesRoot, "include"),
        File(sweetEditorHome, "include"),
    ).firstOrNull { it.resolve("sweeteditor/c_api.h").isFile }
        ?: error("SweetEditor headers missing (expected natives/include/sweeteditor/c_api.h or \$sweetEditor.home/include)")

    val libraryDir = sequenceOf(
        File(nativesRoot, "ios/$archDir"),
        File(sweetEditorHome, "prebuilt/ios/$archDir"),
    ).firstOrNull { dir ->
        dir.resolve("libsweeteditor.a").isFile || dir.resolve("libsweeteditor.dylib").isFile
    } ?: error("SweetEditor iOS library missing for $archDir")

    target.compilations.getByName("main").cinterops.create("sweeteditor") {
        defFile(file("src/nativeInterop/cinterop/sweeteditor.def"))
        includeDirs(includeDir)
        linkerOpts("-L${libraryDir.absolutePath}", "-lsweeteditor")
    }
}

private fun currentDesktopResourceFolder(): String {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val arch = System.getProperty("os.arch").orEmpty().lowercase()
    val osName = when {
        os.contains("win") -> "windows"
        os.contains("mac") || os.contains("darwin") -> "macos"
        else -> "linux"
    }
    val archName = when {
        arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
        else -> "x86_64"
    }
    return "$osName-$archName"
}
