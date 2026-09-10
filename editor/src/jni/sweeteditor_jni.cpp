#include "sweeteditor_jni.h"

#include <jni.h>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <mutex>
#include <string>
#include <unordered_map>
#include <vector>

#include <sweeteditor/c_api.h>

namespace {

JavaVM* g_vm = nullptr;
jmethodID g_measure_text = nullptr;
jmethodID g_measure_inlay = nullptr;
jmethodID g_measure_icon = nullptr;
jmethodID g_font_ascent = nullptr;
jmethodID g_font_descent = nullptr;

std::mutex g_mu;
std::unordered_map<intptr_t, jobject> g_measurers;
thread_local jobject g_pending_measurer = nullptr;
thread_local intptr_t g_active_editor = 0;

JNIEnv* env_or_attach(bool* attached) {
  *attached = false;
  if (g_vm == nullptr) {
    return nullptr;
  }
  JNIEnv* env = nullptr;
  jint rc = g_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
  if (rc == JNI_OK) {
    return env;
  }
  if (rc == JNI_EDETACHED) {
    if (g_vm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr) != 0) {
      return nullptr;
    }
    *attached = true;
    return env;
  }
  return nullptr;
}

void maybe_detach(bool attached) {
  if (attached && g_vm != nullptr) {
    g_vm->DetachCurrentThread();
  }
}

jobject current_measurer() {
  if (g_pending_measurer != nullptr) {
    return g_pending_measurer;
  }
  std::lock_guard<std::mutex> lock(g_mu);
  auto it = g_measurers.find(g_active_editor);
  if (it == g_measurers.end()) {
    return nullptr;
  }
  return it->second;
}

jstring u16_to_jstring(JNIEnv* env, const U16Char* text) {
  if (env == nullptr || text == nullptr) {
    return env != nullptr ? env->NewString(static_cast<const jchar*>(nullptr), 0) : nullptr;
  }
  jsize len = 0;
  while (text[len] != 0) {
    len++;
  }
  return env->NewString(reinterpret_cast<const jchar*>(text), len);
}

float EDITOR_CALL measure_text_width(const U16Char* text, int32_t font_style) {
  bool attached = false;
  JNIEnv* env = env_or_attach(&attached);
  jobject measurer = current_measurer();
  if (env == nullptr || measurer == nullptr || g_measure_text == nullptr) {
    maybe_detach(attached);
    return 0.f;
  }
  jstring jtext = u16_to_jstring(env, text);
  jfloat width = env->CallFloatMethod(measurer, g_measure_text, jtext, static_cast<jint>(font_style));
  env->DeleteLocalRef(jtext);
  maybe_detach(attached);
  return width;
}

float EDITOR_CALL measure_inlay_hint_width(const U16Char* text) {
  bool attached = false;
  JNIEnv* env = env_or_attach(&attached);
  jobject measurer = current_measurer();
  if (env == nullptr || measurer == nullptr || g_measure_inlay == nullptr) {
    maybe_detach(attached);
    return 0.f;
  }
  jstring jtext = u16_to_jstring(env, text);
  jfloat width = env->CallFloatMethod(measurer, g_measure_inlay, jtext);
  env->DeleteLocalRef(jtext);
  maybe_detach(attached);
  return width;
}

float EDITOR_CALL measure_icon_width(int32_t icon_id) {
  bool attached = false;
  JNIEnv* env = env_or_attach(&attached);
  jobject measurer = current_measurer();
  if (env == nullptr || measurer == nullptr || g_measure_icon == nullptr) {
    maybe_detach(attached);
    return 0.f;
  }
  jfloat width = env->CallFloatMethod(measurer, g_measure_icon, static_cast<jint>(icon_id));
  maybe_detach(attached);
  return width;
}

void EDITOR_CALL get_font_metrics(float* arr, size_t length) {
  if (arr == nullptr || length < 2) {
    return;
  }
  bool attached = false;
  JNIEnv* env = env_or_attach(&attached);
  jobject measurer = current_measurer();
  if (env == nullptr || measurer == nullptr || g_font_ascent == nullptr || g_font_descent == nullptr) {
    arr[0] = 0.f;
    arr[1] = 0.f;
    maybe_detach(attached);
    return;
  }
  arr[0] = env->CallFloatMethod(measurer, g_font_ascent);
  arr[1] = env->CallFloatMethod(measurer, g_font_descent);
  maybe_detach(attached);
}

jbyteArray adopt_binary(JNIEnv* env, const uint8_t* payload, size_t size) {
  if (payload == nullptr) {
    return nullptr;
  }
  jbyteArray arr = env->NewByteArray(static_cast<jsize>(size));
  if (arr == nullptr) {
    free_binary_data(reinterpret_cast<intptr_t>(payload));
    return nullptr;
  }
  if (size > 0) {
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(size), reinterpret_cast<const jbyte*>(payload));
  }
  free_binary_data(reinterpret_cast<intptr_t>(payload));
  return arr;
}

struct BytesView {
  const uint8_t* ptr = nullptr;
  jsize len = 0;
  jbyteArray arr = nullptr;
  JNIEnv* env = nullptr;
  jbyte* raw = nullptr;

  BytesView(JNIEnv* e, jbyteArray a) : arr(a), env(e) {
    if (a == nullptr) {
      return;
    }
    len = env->GetArrayLength(a);
    raw = env->GetByteArrayElements(a, nullptr);
    ptr = reinterpret_cast<const uint8_t*>(raw);
  }

  ~BytesView() {
    if (env != nullptr && arr != nullptr && raw != nullptr) {
      env->ReleaseByteArrayElements(arr, raw, JNI_ABORT);
    }
  }
};

std::string bytes_to_string(JNIEnv* env, jbyteArray arr) {
  BytesView view(env, arr);
  if (view.ptr == nullptr || view.len <= 0) {
    return std::string();
  }
  return std::string(reinterpret_cast<const char*>(view.ptr), static_cast<size_t>(view.len));
}

jbyteArray utf8_to_bytes(JNIEnv* env, char* text) {
  if (text == nullptr) {
    return env->NewByteArray(0);
  }
  const size_t n = std::strlen(text);
  jbyteArray arr = env->NewByteArray(static_cast<jsize>(n));
  if (n > 0) {
    env->SetByteArrayRegion(arr, 0, static_cast<jsize>(n), reinterpret_cast<const jbyte*>(text));
  }
  free_u8_string(reinterpret_cast<intptr_t>(text));
  return arr;
}

jlong create_document_from_utf8(JNIEnv* env, jclass, jbyteArray utf8) {
  const std::string text = bytes_to_string(env, utf8);
  return static_cast<jlong>(::create_document_from_utf8(text.c_str()));
}

void free_document_jni(JNIEnv*, jclass, jlong handle) {
  free_document(static_cast<intptr_t>(handle));
}

jbyteArray get_document_utf8_jni(JNIEnv* env, jclass, jlong handle) {
  return utf8_to_bytes(env, get_document_utf8(static_cast<intptr_t>(handle)));
}

jlong create_editor_jni(JNIEnv* env, jclass, jobject measurer, jbyteArray options) {
  jobject global_measurer = env->NewGlobalRef(measurer);
  g_pending_measurer = global_measurer;
  BytesView opts(env, options);
  text_measurer_t native_measurer{};
  native_measurer.measure_text_width = measure_text_width;
  native_measurer.measure_inlay_hint_width = measure_inlay_hint_width;
  native_measurer.measure_icon_width = measure_icon_width;
  native_measurer.get_font_metrics = get_font_metrics;
  const intptr_t handle = create_editor(native_measurer, opts.ptr, static_cast<size_t>(opts.len));
  g_pending_measurer = nullptr;
  if (handle == 0) {
    env->DeleteGlobalRef(global_measurer);
    return 0;
  }
  std::lock_guard<std::mutex> lock(g_mu);
  g_measurers[handle] = global_measurer;
  return static_cast<jlong>(handle);
}

void free_editor_jni(JNIEnv* env, jclass, jlong handle) {
  const intptr_t native = static_cast<intptr_t>(handle);
  free_editor(native);
  std::lock_guard<std::mutex> lock(g_mu);
  auto it = g_measurers.find(native);
  if (it != g_measurers.end()) {
    env->DeleteGlobalRef(it->second);
    g_measurers.erase(it);
  }
}

class ActiveEditor {
 public:
  explicit ActiveEditor(intptr_t handle) : previous_(g_active_editor) {
    g_active_editor = handle;
  }
  ~ActiveEditor() { g_active_editor = previous_; }

 private:
  intptr_t previous_;
};

jbyteArray set_document_jni(JNIEnv* env, jclass, jlong editor, jlong document) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_document(static_cast<intptr_t>(editor), static_cast<intptr_t>(document), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_viewport_jni(JNIEnv* env, jclass, jlong editor, jint width, jint height) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_viewport(static_cast<intptr_t>(editor), width, height, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray on_font_metrics_changed_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_on_font_metrics_changed(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray build_render_model_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_build_render_model(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray handle_gesture_jni(JNIEnv* env, jclass, jlong editor, jbyteArray payload) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  BytesView view(env, payload);
  size_t size = 0;
  const uint8_t* result = editor_handle_gesture_event(
      static_cast<intptr_t>(editor), view.ptr, static_cast<size_t>(view.len), &size);
  return adopt_binary(env, result, size);
}

jbyteArray handle_key_jni(JNIEnv* env, jclass, jlong editor, jint key_code, jbyteArray text, jint modifiers) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  const std::string utf8 = bytes_to_string(env, text);
  size_t size = 0;
  const uint8_t* result = editor_handle_key_event(
      static_cast<intptr_t>(editor),
      static_cast<uint16_t>(key_code),
      utf8.empty() ? nullptr : utf8.c_str(),
      static_cast<uint8_t>(modifiers),
      &size);
  return adopt_binary(env, result, size);
}

jbyteArray tick_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_tick_animations(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray insert_text_jni(JNIEnv* env, jclass, jlong editor, jbyteArray text) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  const std::string utf8 = bytes_to_string(env, text);
  size_t size = 0;
  const uint8_t* payload = editor_insert_text(static_cast<intptr_t>(editor), utf8.c_str(), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray backspace_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_backspace(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray undo_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_undo(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray redo_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_redo(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jboolean can_undo_jni(JNIEnv*, jclass, jlong editor) {
  return editor_can_undo(static_cast<intptr_t>(editor)) != 0 ? JNI_TRUE : JNI_FALSE;
}

jboolean can_redo_jni(JNIEnv*, jclass, jlong editor) {
  return editor_can_redo(static_cast<intptr_t>(editor)) != 0 ? JNI_TRUE : JNI_FALSE;
}

jbyteArray set_gutter_sticky_jni(JNIEnv* env, jclass, jlong editor, jboolean sticky) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_gutter_sticky(static_cast<intptr_t>(editor), sticky ? 1 : 0, &size);
  return adopt_binary(env, payload, size);
}

const JNINativeMethod kMethods[] = {
    {"createDocumentFromUtf8", "([B)J", (void*)create_document_from_utf8},
    {"freeDocument", "(J)V", (void*)free_document_jni},
    {"getDocumentUtf8", "(J)[B", (void*)get_document_utf8_jni},
    {"createEditor", "(Lio/github/lumkit/sweeteditor/core/HostTextMeasurer;[B)J", (void*)create_editor_jni},
    {"freeEditor", "(J)V", (void*)free_editor_jni},
    {"editorSetDocument", "(JJ)[B", (void*)set_document_jni},
    {"editorSetViewport", "(JII)[B", (void*)set_viewport_jni},
    {"editorOnFontMetricsChanged", "(J)[B", (void*)on_font_metrics_changed_jni},
    {"editorBuildRenderModel", "(J)[B", (void*)build_render_model_jni},
    {"editorHandleGestureEvent", "(J[B)[B", (void*)handle_gesture_jni},
    {"editorHandleKeyEvent", "(JI[BI)[B", (void*)handle_key_jni},
    {"editorTickAnimations", "(J)[B", (void*)tick_jni},
    {"editorInsertText", "(J[B)[B", (void*)insert_text_jni},
    {"editorBackspace", "(J)[B", (void*)backspace_jni},
    {"editorUndo", "(J)[B", (void*)undo_jni},
    {"editorRedo", "(J)[B", (void*)redo_jni},
    {"editorCanUndo", "(J)Z", (void*)can_undo_jni},
    {"editorCanRedo", "(J)Z", (void*)can_redo_jni},
    {"editorSetGutterSticky", "(JZ)[B", (void*)set_gutter_sticky_jni},
};

}  // namespace

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  g_vm = vm;
  JNIEnv* env = nullptr;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }
  jclass jni_class = env->FindClass("io/github/lumkit/sweeteditor/internal/jni/SweetEditorJni");
  if (jni_class == nullptr) {
    return JNI_ERR;
  }
  if (env->RegisterNatives(jni_class, kMethods, sizeof(kMethods) / sizeof(kMethods[0])) != 0) {
    return JNI_ERR;
  }
  jclass measurer_class = env->FindClass("io/github/lumkit/sweeteditor/core/HostTextMeasurer");
  if (measurer_class == nullptr) {
    return JNI_ERR;
  }
  g_measure_text = env->GetMethodID(measurer_class, "measureTextWidth", "(Ljava/lang/String;I)F");
  g_measure_inlay = env->GetMethodID(measurer_class, "measureInlayHintWidth", "(Ljava/lang/String;)F");
  g_measure_icon = env->GetMethodID(measurer_class, "measureIconWidth", "(I)F");
  g_font_ascent = env->GetMethodID(measurer_class, "fontAscent", "()F");
  g_font_descent = env->GetMethodID(measurer_class, "fontDescent", "()F");
  if (g_measure_text == nullptr || g_measure_inlay == nullptr || g_measure_icon == nullptr ||
      g_font_ascent == nullptr || g_font_descent == nullptr) {
    return JNI_ERR;
  }
#ifdef _WIN32
  init_unhandled_exception_handler();
#endif
  fprintf(stderr, "sweeteditor_compose JNI_OnLoad\n");
  return JNI_VERSION_1_6;
}
