package io.github.lumkit.editor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform