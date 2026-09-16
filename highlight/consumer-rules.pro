# JNI keep rules for SweetLine Compose (Android R8 + Compose Desktop R8/ProGuard).

-keep class io.github.lumkit.sweeteditor.highlight.internal.jni.SweetLineJni { *; }
-keep class io.github.lumkit.sweeteditor.highlight.internal.NativeLibraryLoader { *; }
-keepclasseswithmembernames class * { native <methods>; }
