package io.github.lumkit.sweeteditor.internal

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal actual object NativeLibraryLoader {
    actual fun bundledLibraryResourcePath(): String? {
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
        return "${NativeBundle.RESOURCE_ROOT}/$osName-$archName/${System.mapLibraryName(NativeBundle.LIBRARY_NAME)}"
    }

    actual fun loadIfAvailable(): Boolean {
        val resourcePath = "/${bundledLibraryResourcePath()}"
        val resource = NativeLibraryLoader::class.java.getResource(resourcePath) ?: return false
        val targetDir = nativeCacheDir()
        val targetFile = targetDir.resolve(System.mapLibraryName(NativeBundle.LIBRARY_NAME))
        Files.createDirectories(targetDir)
        resource.openStream().use { input ->
            Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }
        System.load(targetFile.toAbsolutePath().toString())
        return true
    }

    private fun nativeCacheDir(): Path {
        val version = NativeLibraryLoader::class.java.`package`?.implementationVersion ?: "dev"
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val userHome = System.getProperty("user.home")
        val base = when {
            os.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA")
                if (!localAppData.isNullOrBlank()) {
                    Path.of(localAppData, "SweetEditor", "native", version)
                } else {
                    Path.of(userHome, "AppData", "Local", "SweetEditor", "native", version)
                }
            }
            os.contains("mac") || os.contains("darwin") ->
                Path.of(userHome, "Library", "Application Support", "SweetEditor", "native", version)
            else -> {
                val xdg = System.getenv("XDG_DATA_HOME")
                if (!xdg.isNullOrBlank()) {
                    Path.of(xdg, "sweeteditor", "native", version)
                } else {
                    Path.of(userHome, ".local", "share", "sweeteditor", "native", version)
                }
            }
        }
        return base
    }
}
