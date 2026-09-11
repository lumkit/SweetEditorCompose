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
            configurationFiles.from(rootProject.file("editor/consumer-rules.pro"))
        }
    }
}