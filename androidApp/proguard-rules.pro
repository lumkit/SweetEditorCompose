# App-specific Android R8 rules.
# Highlight JNI is registered by name in libsweetline_compose; keep it even if
# the library consumer-rules are not picked up from the KMP Android variant.

-keep class io.github.lumkit.sweeteditor.highlight.jni.SweetLineJni { *; }
-keep class io.github.lumkit.sweeteditor.highlight.internal.NativeLibraryLoader { *; }
-keep class io.github.lumkit.sweeteditor.highlight.HighlightException { *; }
-keepclasseswithmembernames class * { native <methods>; }
