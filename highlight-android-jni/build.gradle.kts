plugins {
    alias(libs.plugins.androidLibrary)
    `maven-publish`
    signing
    alias(libs.plugins.nmcp)
}

group = rootProject.group
version = rootProject.version
description = "Android JNI + SweetLine core native libraries for sweetline-compose"

val highlightDir = rootProject.layout.projectDirectory.dir("highlight")
val androidCoreDir = highlightDir.dir("natives/android")
val includeDir = highlightDir.dir("natives/include")

android {
    namespace = "io.github.lumkit.sweeteditor.highlight.jni"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DSWEETLINE_INCLUDE_DIR=${includeDir.asFile.absolutePath}",
                    "-DSWEETLINE_ANDROID_LIB_DIR=${androidCoreDir.asFile.absolutePath}",
                )
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = highlightDir.file("src/jni/CMakeLists.txt").asFile
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
            artifactId = "sweetline-compose-android-jni"
            pom {
                name.set("SweetLine Compose Android JNI")
                description.set(project.description)
            }
        }
    }
}

apply(from = rootProject.file("gradle/maven-publishing.gradle.kts"))

tasks.matching { it.name.startsWith("publish") }.configureEach {
    dependsOn(":highlight:verifyReleaseNatives")
}
