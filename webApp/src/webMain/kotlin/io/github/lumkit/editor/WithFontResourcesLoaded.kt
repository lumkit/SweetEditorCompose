package io.github.lumkit.editor

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

import sweeteditorcompose.webapp.generated.resources.NotoSansSC
import sweeteditorcompose.webapp.generated.resources.Res

@OptIn(ExperimentalResourceApi::class)
@Composable
internal inline fun WithFontResourcesLoaded(
    content: @Composable () -> Unit
) {
    val font by preloadFont(Res.font.NotoSansSC)

    var fontFallbackInitialized by remember { mutableStateOf(false) }
    val fontFamilyResolver = LocalFontFamilyResolver.current

    LaunchedEffect(fontFamilyResolver, font) {
        font?.let { font ->
            fontFamilyResolver.preload(FontFamily(font))
            fontFallbackInitialized = true
        }
    }

    if (fontFallbackInitialized) {
        content()
    }
}