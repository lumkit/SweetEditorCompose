plugins {
    alias(libs.plugins.androidLibrary)
    `maven-publish`
    signing
    alias(libs.plugins.nmcp)
}

group = rootProject.group
version = rootProject.version
description = "Android JNI + SweetEditor core native libraries for sweeteditor-compose"

val editorDir = rootProject.layout.projectDirectory.dir("editor")
val androidCoreDir = editorDir.dir("natives/android")
val includeDir = editorDir.dir("natives/include")

android {
    namespace = "io.github.lumkit.sweeteditor.jni"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        consumerProguardFiles(rootProject.file("editor/consumer-rules.pro"))
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DSWEETEDITOR_INCLUDE_DIR=${includeDir.asFile.absolutePath}",
                    "-DSWEETEDITOR_ANDROID_LIB_DIR=${androidCoreDir.asFile.absolutePath}",
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = editorDir.file("src/jni/CMakeLists.txt").asFile
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets.getByName("main") {
        jniLibs.srcDir(androidCoreDir.asFile)
        manifest.srcFile("src/main/AndroidManifest.xml")
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            artifactId = "sweeteditor-compose-android-jni"
            pom {
                name.set("SweetEditor Compose Android JNI")
                description.set(project.description)
            }
        }
    }
}

apply(from = rootProject.file("gradle/maven-publishing.gradle.kts"))

tasks.matching { it.name.startsWith("publish") }.configureEach {
    dependsOn(":editor:verifyReleaseNatives")
}
