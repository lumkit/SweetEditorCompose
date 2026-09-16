# JNI keep rules for SweetLine Compose (Android R8 + Compose Desktop R8/ProGuard).
# Native FindClass / RegisterNatives require the original class and method names.

-keep class io.github.lumkit.sweeteditor.highlight.jni.SweetLineJni { *; }
-keep class io.github.lumkit.sweeteditor.highlight.internal.NativeLibraryLoader { *; }
-keep class io.github.lumkit.sweeteditor.highlight.HighlightException { *; }
-keep class io.github.lumkit.sweeteditor.highlight.generated.resources.** { *; }
-keep class org.jetbrains.compose.resources.** { *; }
-keepclasseswithmembernames class * { native <methods>; }
