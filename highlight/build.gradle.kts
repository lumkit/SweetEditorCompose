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
    `maven-publish`
    signing
    alias(libs.plugins.nmcp)
}

group = rootProject.group
version = rootProject.version
description = "Compose Multiplatform SweetLine highlight bindings for SweetEditor"

val mavenArtifactId = "sweetline-compose"

val sweetLineHome: File = resolveSweetLineHome()
val nativesRoot: File = layout.projectDirectory.dir("natives").asFile
val generatedNatives: Provider<Directory> = layout.buildDirectory.dir("generated/natives")
val jvmNativeResourcesDir: Provider<Directory> = generatedNatives.map { it.dir("jvmResources") }
val jvmKeepRulesDir: Provider<Directory> = generatedNatives.map { it.dir("jvmKeepRules") }
val webNativeResourcesDir: Provider<Directory> = generatedNatives.map { it.dir("webResources") }
val webComposeResourcesDir: Provider<Directory> = generatedNatives.map { it.dir("webComposeResources") }
val androidJniLibsDir: Directory = layout.projectDirectory.dir("src/androidMain/jniLibs")

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        configureSweetLineCinterop(target)
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
        namespace = "io.github.lumkit.sweeteditor.highlight"
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
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-rules.pro")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":editor"))
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
                implementation(project(":highlight-android-jni"))
                implementation(libs.compose.uiToolingPreview)
            }
        }
        named("jvmMain") {
            dependsOn(jniMain)
            resources.srcDir(jvmNativeResourcesDir)
            resources.srcDir(jvmKeepRulesDir)
        }
        val iosMain by creating {
            dependsOn(commonMain)
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
            resources.srcDir("src/webResources")
        }
        named("wasmJsMain") {
            dependsOn(stubNativeMain)
            resources.srcDir(webNativeResourcesDir)
            resources.srcDir("src/webResources")
        }
        named("jvmTest") {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

val syncSweetLineNatives by tasks.registering(Sync::class) {
    group = "sweetline"
    description =
        "Copy SweetLine release prebuilts into highlight/natives (packaging). Desktop run/test builds the host core with CMake instead."

    into(nativesRoot)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    preserve {
        include(".gitkeep")
        include("**/.gitkeep")
    }

    from(File(sweetLineHome, "prebuilt/android")) {
        into("android")
        include("**/*.so")
    }
    from(File(sweetLineHome, "prebuilt/ios/arm64")) {
        into("ios/arm64")
        include("*.dylib", "*.a")
    }
    from(File(sweetLineHome, "prebuilt/ios/simulator-arm64")) {
        into("ios/simulator-arm64")
        include("*.dylib", "*.a")
    }
    from(File(sweetLineHome, "prebuilt/macos/arm64")) {
        into("desktop/macos-aarch64")
        include("*.dylib")
    }
    from(File(sweetLineHome, "prebuilt/macos/x86_64")) {
        into("desktop/macos-x86_64")
        include("*.dylib")
    }
    from(File(sweetLineHome, "prebuilt/linux/x86_64")) {
        into("desktop/linux-x86_64")
        include("*.so")
    }
    from(File(sweetLineHome, "prebuilt/linux/aarch64")) {
        into("desktop/linux-aarch64")
        include("*.so")
    }
    from(File(sweetLineHome, "prebuilt/windows/x64")) {
        into("desktop/windows-x86_64")
        include("*.dll")
    }
    from(File(sweetLineHome, "prebuilt/wasm")) {
        into("web")
        include("sweetline_c_abi.js", "sweetline_c_abi.wasm")
    }
    from(File(sweetLineHome, "include/sweetline")) {
        into("include/sweetline")
        include("**/*.h", "**/*.hpp")
    }
}

val prepareAndroidJniLibs by tasks.registering(Sync::class) {
    group = "sweetline"
    description = "Stage Android JNI libraries under androidMain/jniLibs so they ship inside the AAR."
    into(androidJniLibsDir)
    from(resolveNativeSource("android", "prebuilt/android")) {
        include("**/*.so")
    }
}

val prepareJvmNativeResources by tasks.registering(Sync::class) {
    group = "sweetline"
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
    group = "sweetline"
    description = "Stage WebAssembly C ABI modules as JS/Wasm resources under /native/web/."
    into(webNativeResourcesDir.map { it.dir("native/web") })
    from(resolveNativeSource("web", "prebuilt/wasm")) {
        include("sweetline_c_abi.js", "sweetline_c_abi.wasm")
    }
}

val prepareWebComposeResources by tasks.registering(Sync::class) {
    group = "sweetline"
    description = "Stage C ABI + loader so Compose copies them into the webpack output."
    into(webComposeResourcesDir.map { it.dir("files") })
    from(resolveNativeSource("web", "prebuilt/wasm")) {
        include("sweetline_c_abi.js", "sweetline_c_abi.wasm")
    }
    from(layout.projectDirectory.file("src/webResources/sweetline_web_abi.js"))
}

val stageJvmKeepRules by tasks.registering(Sync::class) {
    group = "sweetline"
    description = "Stage JNI keep rules for Compose Desktop R8/ProGuard under META-INF."
    into(jvmKeepRulesDir)
    from("consumer-rules.pro") {
        into("META-INF/proguard")
        rename { "sweetline-compose.pro" }
    }
    from("consumer-rules.pro") {
        into("META-INF/com.android.tools/r8")
        rename { "sweetline-compose.pro" }
    }
}

val desktopJniBuildDir: Provider<Directory> = layout.buildDirectory.dir("jni/desktop")
val hostDesktopFolder: String = currentDesktopResourceFolder()
val jniBuildScript = layout.projectDirectory.file("scripts/build-desktop-jni.sh")
val hostCoreBuildScript = layout.projectDirectory.file("scripts/build-host-core.sh")
val hostCoreBuildDir = File(sweetLineHome, "build/compose-host")
val iosStaticCoreScript = layout.projectDirectory.file("scripts/build-ios-static-core.sh")

val buildHostSweetLineCore by tasks.registering(Exec::class) {
    group = "sweetline"
    description =
        "CMake-build the SweetLine C++ core for this desktop host (README local-compile flow) and install it into highlight/natives/"
    environment("SWEETLINE_HOME", sweetLineHome.absolutePath)
    inputs.dir(File(sweetLineHome, "src"))
    inputs.dir(File(sweetLineHome, "include"))
    inputs.file(File(sweetLineHome, "CMakeLists.txt"))
    inputs.file(hostCoreBuildScript)
    outputs.dir(File(nativesRoot, "desktop/$hostDesktopFolder"))
    outputs.dir(File(nativesRoot, "include/sweetline"))
    outputs.dir(hostCoreBuildDir)
    commandLine("bash", hostCoreBuildScript.asFile.absolutePath, "all")
}

private fun registerIosStaticCoreTask(taskName: String, abi: String) = tasks.register<Exec>(taskName) {
    group = "sweetline"
    description = "CMake-build SweetLine static archive for iOS $abi into highlight/natives/"
    environment("SWEETLINE_HOME", sweetLineHome.absolutePath)
    inputs.dir(File(sweetLineHome, "src"))
    inputs.dir(File(sweetLineHome, "include"))
    inputs.dir(File(sweetLineHome, "cmake"))
    inputs.dir(File(sweetLineHome, "3dparty"))
    inputs.file(File(sweetLineHome, "CMakeLists.txt"))
    inputs.file(iosStaticCoreScript)
    outputs.file(File(nativesRoot, "ios/$abi/libsweetline.a"))
    commandLine("bash", iosStaticCoreScript.asFile.absolutePath, abi)
}

val buildIosSweetLineStaticSimulatorArm64 =
    registerIosStaticCoreTask("buildIosSweetLineStaticSimulatorArm64", "simulator-arm64")
val buildIosSweetLineStaticArm64 =
    registerIosStaticCoreTask("buildIosSweetLineStaticArm64", "arm64")
val buildIosSweetLineStatic by tasks.registering {
    group = "sweetline"
    description = "Build iOS static archives for device and simulator"
    dependsOn(buildIosSweetLineStaticSimulatorArm64, buildIosSweetLineStaticArm64)
}

val configureDesktopJni by tasks.registering(Exec::class) {
    group = "sweetline"
    description = "Configure CMake for libsweetline_compose on the current desktop host"
    inputs.files(
        file("src/jni/CMakeLists.txt"),
        file("src/jni/sweetline_jni.cpp"),
        file("src/jni/sweetline_jni.h"),
        jniBuildScript,
    )
    outputs.dir(desktopJniBuildDir)
    dependsOn(buildHostSweetLineCore)
    commandLine("bash", jniBuildScript.asFile.absolutePath, "configure")
}

val compileDesktopJni by tasks.registering(Exec::class) {
    group = "sweetline"
    description = "Build libsweetline_compose for the current desktop host"
    dependsOn(buildHostSweetLineCore, configureDesktopJni)
    inputs.dir(desktopJniBuildDir)
    commandLine("bash", jniBuildScript.asFile.absolutePath, "build")
}

prepareJvmNativeResources {
    dependsOn(buildHostSweetLineCore, compileDesktopJni)
    from(desktopJniBuildDir) {
        include("libsweetline_compose.dylib", "libsweetline_compose.so", "sweetline_compose.dll")
        include("Release/sweetline_compose.dll")
        into(hostDesktopFolder)
    }
}

val verifyReleaseNatives = tasks.register("verifyReleaseNatives") {
    group = "sweetline"
    description = "Fail non-SNAPSHOT publishes when packaged native binaries are incomplete"
    val nativesPath = layout.projectDirectory.dir("natives").asFile.absolutePath
    val versionName = providers.gradleProperty("VERSION_NAME")
    val requireComplete = providers.gradleProperty("sweetline.publish.requireCompleteNatives")
    doLast {
        val natives = File(nativesPath)
        val required = listOf(
            "include/sweetline/c_sweetline.h",
            "desktop/macos-aarch64/libsweetline.dylib",
            "desktop/macos-aarch64/libsweetline_compose.dylib",
            "desktop/macos-x86_64/libsweetline.dylib",
            "desktop/macos-x86_64/libsweetline_compose.dylib",
            "desktop/linux-x86_64/libsweetline.so",
            "desktop/linux-x86_64/libsweetline_compose.so",
            "desktop/linux-aarch64/libsweetline.so",
            "desktop/linux-aarch64/libsweetline_compose.so",
            "desktop/windows-x86_64/sweetline.dll",
            "desktop/windows-x86_64/sweetline_compose.dll",
            "android/arm64-v8a/libsweetline.so",
            "android/x86_64/libsweetline.so",
            "ios/arm64/libsweetline.a",
            "ios/simulator-arm64/libsweetline.a",
            "web/sweetline_c_abi.js",
            "web/sweetline_c_abi.wasm",
        )
        val missing = required.filter { !File(natives, it).isFile }
        if (missing.isEmpty()) {
            logger.lifecycle("Release natives complete under ${natives.absolutePath}")
        }
        val resolvedVersion = versionName.get()
        val force = requireComplete.orNull == "true"
        val release = !resolvedVersion.endsWith("-SNAPSHOT")
        val message = buildString {
            appendLine("Incomplete natives for publishing $resolvedVersion:")
            missing.forEach { appendLine("  - natives/$it") }
            append("Run highlight/scripts/prepare-release-natives.sh on CI (or locally) before a non-SNAPSHOT release.")
        }
        if (missing.isNotEmpty()) {
            if (release || force) {
                error(message)
            }
            logger.warn(message)
        }

        val isMac = System.getProperty("os.name").orEmpty().lowercase().let {
            it.contains("mac") || it.contains("darwin")
        }
        if (isMac && File("/usr/bin/xcrun").isFile) {
            fun platformOf(archive: File): String? {
                if (!archive.isFile) return null
                val tmp = File.createTempFile("se-ios-ar-", "").also { scratch ->
                    scratch.delete()
                    scratch.mkdirs()
                }
                return try {
                    ProcessBuilder("ar", "-x", archive.absolutePath, "c_sweetline.cpp.o")
                        .directory(tmp)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor()
                    val obj = tmp.resolve("c_sweetline.cpp.o").takeIf { it.isFile }
                        ?: tmp.listFiles()?.firstOrNull { it.extension == "o" }
                        ?: return null
                    val out = ProcessBuilder("xcrun", "vtool", "-show-build", obj.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                        .inputStream
                        .bufferedReader()
                        .readText()
                    Regex("""platform\s+(\S+)""").find(out)?.groupValues?.get(1)
                } finally {
                    tmp.deleteRecursively()
                }
            }
            val device = platformOf(File(natives, "ios/arm64/libsweetline.a"))
            val simulator = platformOf(File(natives, "ios/simulator-arm64/libsweetline.a"))
            if (device != null && device != "IOS") {
                error("natives/ios/arm64/libsweetline.a is $device, expected IOS")
            }
            if (simulator != null && simulator != "IOSSIMULATOR") {
                error("natives/ios/simulator-arm64/libsweetline.a is $simulator, expected IOSSIMULATOR")
            }
            if (device != null && simulator != null) {
                logger.lifecycle("iOS archives: device=$device simulator=$simulator")
            }
        }
    }
}

tasks.matching { it.name.startsWith("publish") }.configureEach {
    dependsOn(verifyReleaseNatives)
}

tasks.withType<Jar>().configureEach {
    if (name == "jvmJar") {
        manifest.attributes["Implementation-Version"] = version.toString()
    }
}

tasks.configureEach {
    if (name.contains("ProcessResources")) {
        if (name.contains("jvm", ignoreCase = true)) {
            dependsOn(prepareJvmNativeResources, stageJvmKeepRules)
        }
        if (name.contains("js", ignoreCase = true) || name.contains("wasm", ignoreCase = true)) {
            dependsOn(prepareWebNativeResources, prepareWebComposeResources)
        }
    }
}

private fun resolveSweetLineHome(): File {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }

    val configured = providers.gradleProperty("sweetLine.home").orNull
        ?: localProperties.getProperty("sweetLine.home")
        ?: "../SweetLine"
    val configuredFile = File(configured)
    return if (configuredFile.isAbsolute) configuredFile else rootProject.file(configured)
}

private fun resolveNativeSource(vendoredRelative: String, prebuiltRelative: String): File {
    val vendored = File(nativesRoot, vendoredRelative)
    return if (vendored.exists()) vendored else File(sweetLineHome, prebuiltRelative)
}

private fun configureSweetLineCinterop(target: KotlinNativeTarget) {
    val archDir = when (target.name) {
        "iosArm64" -> "arm64"
        "iosSimulatorArm64" -> "simulator-arm64"
        else -> return
    }

    val includeDir = sequenceOf(
        File(nativesRoot, "include"),
        File(sweetLineHome, "include"),
    ).firstOrNull { it.resolve("sweetline/c_sweetline.h").isFile }
        ?: error("SweetLine headers missing (expected natives/include/sweetline/c_sweetline.h or \$sweetLine.home/include)")

    val libraryDir = File(nativesRoot, "ios/$archDir")
    val staticArchive = sequenceOf(
        libraryDir.resolve("libsweetline.a"),
        File(sweetLineHome, "prebuilt/ios/$archDir/libsweetline.a"),
    ).firstOrNull { it.isFile } ?: libraryDir.resolve("libsweetline.a")

    target.compilations.getByName("main").cinterops.create("sweetline") {
        defFile(file("src/nativeInterop/cinterop/sweetline.def"))
        includeDirs(includeDir)
        extraOpts("-libraryPath", staticArchive.parentFile.absolutePath)
    }
}

tasks.matching { it.name == "cinteropSweetlineIosSimulatorArm64" }.configureEach {
    dependsOn(buildIosSweetLineStaticSimulatorArm64)
    mustRunAfter(buildHostSweetLineCore)
    inputs.file(File(nativesRoot, "ios/simulator-arm64/libsweetline.a"))
}
tasks.matching { it.name == "cinteropSweetlineIosArm64" }.configureEach {
    dependsOn(buildIosSweetLineStaticArm64)
    mustRunAfter(buildHostSweetLineCore)
    inputs.file(File(nativesRoot, "ios/arm64/libsweetline.a"))
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

val emptyJavadocJar by tasks.registering(Jar::class) {
    group = "documentation"
    description = "Empty javadoc JAR required by Maven Central for the JVM artifact"
    archiveClassifier.set("javadoc")
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = when {
            artifactId == "highlight" -> mavenArtifactId
            artifactId.startsWith("highlight-") -> mavenArtifactId + artifactId.removePrefix("highlight")
            else -> artifactId
        }
        pom {
            name.set("SweetLine Compose")
            description.set(project.description)
        }
        if (name == "jvm") {
            artifact(emptyJavadocJar)
        }
    }
}

compose {
    resources {
        packageOfResClass = "io.github.lumkit.sweeteditor.highlight.generated.resources"
        customDirectory(
            sourceSetName = "jsMain",
            directoryProvider = webComposeResourcesDir,
        )
        customDirectory(
            sourceSetName = "wasmJsMain",
            directoryProvider = webComposeResourcesDir,
        )
    }
}

tasks.matching {
    val n = it.name
    n != "prepareWebComposeResources" &&
        (
            n.contains("ComposeResource", ignoreCase = true) ||
                n.contains("generateResource", ignoreCase = true) ||
                n.contains("NonXmlValueResources", ignoreCase = true) ||
                n.contains("prepareComposeResources", ignoreCase = true)
            )
}.configureEach {
    dependsOn(prepareWebComposeResources)
}

apply(from = rootProject.file("gradle/maven-publishing.gradle.kts"))
