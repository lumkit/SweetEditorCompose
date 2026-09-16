import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "io.github.lumkit.editor.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            implementation(project(":editor"))
            implementation(project(":highlight"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val generatedDemoResources: Provider<Directory> = layout.buildDirectory.dir("generated/demoResources")
val sweetEditorHome: File = resolveSiblingHome("sweetEditor.home", "../SweetEditor")
val sweetLineHome: File = resolveSiblingHome("sweetLine.home", "../SweetLine")
val demoSampleFiles = listOf("example.java", "example.kt", "example.lua", "gc.cpp")

val syncDemoSamples by tasks.registering(Sync::class) {
    group = "demo"
    description = "Copy SweetEditor demo sample files and lua.json into shared composeResources."
    val sampleNames = demoSampleFiles.toList()
    val samplesSource = File(sweetEditorHome, "platform/_res/files")
    val luaJson = File(sweetLineHome, "syntaxes/lua.json")
    into(generatedDemoResources)
    from(samplesSource) {
        include(sampleNames)
        into("files/samples")
    }
    from(luaJson.parentFile) {
        include("lua.json")
        into("files/syntaxes")
    }
    inputs.property("sampleNames", sampleNames)
    inputs.dir(samplesSource)
    inputs.file(luaJson)
    doFirst {
        val missing = sampleNames.map { name -> File(samplesSource, name) }.filterNot { it.isFile }
        if (missing.isNotEmpty()) {
            error("Missing SweetEditor demo samples:\n${missing.joinToString("\n")}")
        }
        if (!luaJson.isFile) {
            error("Missing SweetLine syntax file: ${luaJson.absolutePath}")
        }
    }
}

compose {
    resources {
        packageOfResClass = "io.github.lumkit.editor.generated.resources"
        customDirectory(
            sourceSetName = "commonMain",
            directoryProvider = generatedDemoResources,
        )
    }
}

tasks.matching {
    val n = it.name
    n != "syncDemoSamples" &&
        (
            n.contains("XmlValueResources", ignoreCase = true) ||
                n.contains("ComposeResource", ignoreCase = true) ||
                n.contains("generateResource", ignoreCase = true) ||
                n.contains("prepareComposeResources", ignoreCase = true)
            )
}.configureEach {
    dependsOn(syncDemoSamples)
}

private fun resolveSiblingHome(propertyName: String, defaultRelative: String): File {
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }
    val configured = providers.gradleProperty(propertyName).orNull
        ?: localProperties.getProperty(propertyName)
        ?: defaultRelative
    val configuredFile = File(configured)
    return if (configuredFile.isAbsolute) configuredFile else rootProject.file(configured)
}