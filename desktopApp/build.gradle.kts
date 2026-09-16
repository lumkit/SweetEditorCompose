import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

val harvestedProguardDir = layout.buildDirectory.dir("generated/proguard-from-deps")
val harvestProguardFromDeps by tasks.registering {
    description = "Collect META-INF ProGuard/R8 consumer rules from runtime JARs"
    val classpath = configurations.named("runtimeClasspath")
    inputs.files(classpath)
    outputs.dir(harvestedProguardDir)
    doLast {
        val dest = harvestedProguardDir.get().asFile
        dest.deleteRecursively()
        dest.mkdirs()
        classpath.get().forEach { jar ->
            if (jar.extension != "jar" || !jar.isFile) return@forEach
            zipTree(jar).matching {
                include("META-INF/proguard/*.pro")
                include("META-INF/com.android.tools/r8/*.pro")
            }.forEach { rule ->
                dest.resolve("${jar.nameWithoutExtension}-${rule.name}").writeText(rule.readText())
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.lumkit.editor.MainKt"
        jvmArgs += listOf(
            "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.lumkit.editor"
            packageVersion = "1.0.0"
        }

        buildTypes.release.proguard {
            configurationFiles.from(harvestProguardFromDeps.map { it.outputs.files.asFileTree })
        }
    }
}
