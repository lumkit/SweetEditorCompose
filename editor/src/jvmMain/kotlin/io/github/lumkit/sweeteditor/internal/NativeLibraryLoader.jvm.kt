package io.github.lumkit.sweeteditor.internal

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

internal actual object NativeLibraryLoader {
    @Volatile
    private var loaded = false

    actual fun bundledLibraryResourcePath(): String? {
        return "${NativeBundle.RESOURCE_ROOT}/${platformDir()}/${System.mapLibraryName(NativeBundle.LIBRARY_NAME)}"
    }

    actual fun loadIfAvailable(): Boolean {
        return try {
            loadComposeJni()
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    actual fun loadComposeJni() {
        synchronized(this) {
            if (loaded) return
            val dir = resolveNativeDir()
            System.load(dir.resolve(System.mapLibraryName(NativeBundle.LIBRARY_NAME)).toAbsolutePath().toString())
            System.load(
                dir.resolve(System.mapLibraryName(NativeBundle.COMPOSE_JNI_LIBRARY_NAME)).toAbsolutePath().toString(),
            )
            loaded = true
        }
    }

    private fun resolveNativeDir(): Path {
        val override = System.getProperty("sweeteditor.lib.path")
        if (!override.isNullOrBlank()) {
            val dir = Path.of(override)
            requireCoreAndJni(dir)
            return dir
        }
        val cache = nativeCacheDir()
        Files.createDirectories(cache)
        extractLibrary(cache, NativeBundle.LIBRARY_NAME)
        extractLibrary(cache, NativeBundle.COMPOSE_JNI_LIBRARY_NAME)
        return cache
    }

    private fun extractLibrary(targetDir: Path, libraryName: String) {
        val resourcePath = "/${NativeBundle.RESOURCE_ROOT}/${platformDir()}/${System.mapLibraryName(libraryName)}"
        val resource = NativeLibraryLoader::class.java.getResource(resourcePath)
            ?: throw UnsatisfiedLinkError("Missing native resource $resourcePath")
        val targetFile = targetDir.resolve(System.mapLibraryName(libraryName))
        resource.openStream().use { input ->
            Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun requireCoreAndJni(dir: Path) {
        val core = dir.resolve(System.mapLibraryName(NativeBundle.LIBRARY_NAME))
        val jni = dir.resolve(System.mapLibraryName(NativeBundle.COMPOSE_JNI_LIBRARY_NAME))
        if (!Files.isRegularFile(core) || !Files.isRegularFile(jni)) {
            throw UnsatisfiedLinkError(
                "sweeteditor.lib.path=$dir must contain ${core.fileName} and ${jni.fileName}",
            )
        }
    }

    private fun platformDir(): String {
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
