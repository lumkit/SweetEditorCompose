# JNI keep rules for SweetEditor Compose (Android R8 + Compose Desktop R8/ProGuard).
# Native FindClass / GetMethodID and @JvmStatic external methods must keep their names.

-keep class io.github.lumkit.sweeteditor.internal.jni.SweetEditorJni { *; }
-keep class io.github.lumkit.sweeteditor.core.HostTextMeasurer { *; }
-keep class io.github.lumkit.sweeteditor.internal.NativeLibraryLoader { *; }
-keepclasseswithmembernames class * { native <methods>; }
