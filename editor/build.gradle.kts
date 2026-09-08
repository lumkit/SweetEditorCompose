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
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
        named("jvmMain") {
            resources.srcDir(jvmNativeResourcesDir)
        }
        named("jsMain") {
            resources.srcDir(webNativeResourcesDir)
        }
        named("wasmJsMain") {
            resources.srcDir(webNativeResourcesDir)
        }
    }
}

val syncSweetEditorNatives by tasks.registering(Sync::class) {
    group = "sweeteditor"
    description =
        "Copy SweetEditor prebuilt natives and public headers into editor/natives for local use and later Maven packaging."

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

tasks.configureEach {
    if (name != prepareAndroidJniLibs.name) {
        val isAndroidNativeConsume = name.contains("Android") &&
            (name.contains("Jni", ignoreCase = true) || name.startsWith("compile") || name.contains("jniLibs", ignoreCase = true))
        if (isAndroidNativeConsume) {
            dependsOn(prepareAndroidJniLibs)
        }
    }
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
    ).firstOrNull { it.resolve("sweeteditor/c_api.h").isFile } ?: return

    val libraryDir = sequenceOf(
        File(nativesRoot, "ios/$archDir"),
        File(sweetEditorHome, "prebuilt/ios/$archDir"),
    ).firstOrNull { dir ->
        dir.resolve("libsweeteditor.a").isFile || dir.resolve("libsweeteditor.dylib").isFile
    }

    target.compilations.getByName("main").cinterops.create("sweeteditor") {
        defFile(file("src/nativeInterop/cinterop/sweeteditor.def"))
        includeDirs(includeDir)
        if (libraryDir != null) {
            linkerOpts("-L${libraryDir.absolutePath}", "-lsweeteditor")
        }
    }
}
