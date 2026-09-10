plugins {
    alias(libs.plugins.androidLibrary)
}

val editorDir = rootProject.layout.projectDirectory.dir("editor")
val androidCoreDir = editorDir.dir("natives/android")
val includeDir = editorDir.dir("natives/include")

android {
    namespace = "io.github.lumkit.sweeteditor.jni"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
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
}
