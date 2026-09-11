plugins {
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

group = "io.github.lumkit"
version = "0.1.0-SNAPSHOT"
description = "Android JNI + SweetEditor core native libraries for sweeteditor-compose"

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

    publishing {
        singleVariant("release")
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
                url.set("https://github.com/lumkit/SweetEditorCompose")
                licenses {
                    license {
                        name.set("GNU Affero General Public License v3.0")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("https://github.com/lumkit/SweetEditorCompose")
                    connection.set("scm:git:https://github.com/lumkit/SweetEditorCompose.git")
                    developerConnection.set("scm:git:ssh://git@github.com/lumkit/SweetEditorCompose.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "BuildDir"
            url = uri(rootProject.layout.buildDirectory.dir("maven"))
        }
    }
}
