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
#ifdef __ANDROID__
    if (g_vm->AttachCurrentThread(&env, nullptr) != 0) {
#else
    if (g_vm->AttachCurrentThread(reinterpret_cast<void**>(&env), nullptr) != 0) {
#endif
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

jbyteArray update_pointer_modifiers_jni(JNIEnv* env, jclass, jlong editor, jint modifiers) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload =
      editor_update_pointer_modifiers(static_cast<intptr_t>(editor), static_cast<uint8_t>(modifiers), &size);
  return adopt_binary(env, payload, size);
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

jbyteArray set_keymap_jni(JNIEnv* env, jclass, jlong editor, jbyteArray payload) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  BytesView view(env, payload);
  size_t size = 0;
  const uint8_t* result = editor_set_keymap(
      static_cast<intptr_t>(editor), view.ptr, static_cast<size_t>(view.len), &size);
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

jbyteArray move_line_up_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_move_line_up(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray move_line_down_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_move_line_down(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray copy_line_up_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_copy_line_up(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray copy_line_down_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_copy_line_down(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray delete_line_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_delete_line(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray insert_line_above_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_insert_line_above(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray insert_line_below_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_insert_line_below(static_cast<intptr_t>(editor), &size);
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

jbyteArray set_gutter_visible_jni(JNIEnv* env, jclass, jlong editor, jboolean visible) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_gutter_visible(static_cast<intptr_t>(editor), visible ? 1 : 0, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_wrap_mode_jni(JNIEnv* env, jclass, jlong editor, jint mode) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_wrap_mode(static_cast<intptr_t>(editor), mode, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_tab_size_jni(JNIEnv* env, jclass, jlong editor, jint tab_size) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_tab_size(static_cast<intptr_t>(editor), tab_size, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_insert_spaces_jni(JNIEnv* env, jclass, jlong editor, jboolean enabled) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_insert_spaces(static_cast<intptr_t>(editor), enabled ? 1 : 0, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_auto_indent_mode_jni(JNIEnv* env, jclass, jlong editor, jint mode) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_auto_indent_mode(static_cast<intptr_t>(editor), mode, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_backspace_unindent_jni(JNIEnv* env, jclass, jlong editor, jboolean enabled) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_backspace_unindent(static_cast<intptr_t>(editor), enabled ? 1 : 0, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_scale_jni(JNIEnv* env, jclass, jlong editor, jfloat scale) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_scale(static_cast<intptr_t>(editor), scale, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_line_spacing_jni(JNIEnv* env, jclass, jlong editor, jfloat add, jfloat mult) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_line_spacing(static_cast<intptr_t>(editor), add, mult, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_read_only_jni(JNIEnv* env, jclass, jlong editor, jboolean read_only) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_read_only(static_cast<intptr_t>(editor), read_only ? 1 : 0, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_current_line_render_mode_jni(JNIEnv* env, jclass, jlong editor, jint mode) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_set_current_line_render_mode(static_cast<intptr_t>(editor), mode, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray set_editor_render_colors_jni(JNIEnv* env, jclass, jlong editor, jbyteArray payload) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  BytesView view(env, payload);
  size_t size = 0;
  const uint8_t* result = editor_set_editor_render_colors(
      static_cast<intptr_t>(editor), view.ptr, static_cast<size_t>(view.len), &size);
  return adopt_binary(env, result, size);
}

jbyteArray set_editor_range_effect_styles_jni(JNIEnv* env, jclass, jlong editor, jbyteArray payload) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  BytesView view(env, payload);
  size_t size = 0;
  const uint8_t* result = editor_set_editor_range_effect_styles(
      static_cast<intptr_t>(editor), view.ptr, static_cast<size_t>(view.len), &size);
  return adopt_binary(env, result, size);
}

jbyteArray ime_begin_session_jni(JNIEnv* env, jclass, jlong editor, jint mutation_model) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_ime_begin_session(static_cast<intptr_t>(editor), mutation_model, &size);
  return adopt_binary(env, payload, size);
}

jbyteArray ime_end_session_jni(JNIEnv* env, jclass, jlong editor, jlong session_id) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload =
      editor_ime_end_session(static_cast<intptr_t>(editor), static_cast<uint64_t>(session_id), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray ime_apply_commands_jni(JNIEnv* env, jclass, jlong editor, jbyteArray payload) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  BytesView view(env, payload);
  size_t size = 0;
  const uint8_t* result = editor_ime_apply_commands(
      static_cast<intptr_t>(editor), view.ptr, static_cast<size_t>(view.len), &size);
  return adopt_binary(env, result, size);
}

jbyteArray ime_get_state_jni(JNIEnv* env, jclass, jlong editor, jlong session_id) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload =
      editor_ime_get_state(static_cast<intptr_t>(editor), static_cast<uint64_t>(session_id), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray ime_get_context_jni(
    JNIEnv* env,
    jclass,
    jlong editor,
    jlong session_id,
    jint source,
    jlong start_utf16,
    jlong length_utf16) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_ime_get_context(
      static_cast<intptr_t>(editor),
      static_cast<uint64_t>(session_id),
      source,
      static_cast<int64_t>(start_utf16),
      static_cast<int64_t>(length_utf16),
      &size);
  return adopt_binary(env, payload, size);
}

jfloatArray get_cursor_rect_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  float x = 0;
  float y = 0;
  float height = 0;
  editor_get_cursor_rect(static_cast<intptr_t>(editor), &x, &y, &height);
  jfloatArray result = env->NewFloatArray(3);
  if (result == nullptr) {
    return nullptr;
  }
  const jfloat data[3] = {x, y, height};
  env->SetFloatArrayRegion(result, 0, 3, data);
  return result;
}

jfloatArray get_position_rect_jni(JNIEnv* env, jclass, jlong editor, jint line, jint column) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  float x = 0;
  float y = 0;
  float height = 0;
  editor_get_position_rect(
      static_cast<intptr_t>(editor),
      static_cast<size_t>(line),
      static_cast<size_t>(column),
      &x,
      &y,
      &height);
  jfloatArray result = env->NewFloatArray(3);
  if (result == nullptr) {
    return nullptr;
  }
  const jfloat data[3] = {x, y, height};
  env->SetFloatArrayRegion(result, 0, 3, data);
  return result;
}

jintArray get_visible_line_range_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  int32_t start_line = 0;
  int32_t end_line = -1;
  editor_get_visible_line_range(static_cast<intptr_t>(editor), &start_line, &end_line);
  jintArray result = env->NewIntArray(2);
  if (result == nullptr) {
    return nullptr;
  }
  const jint data[2] = {static_cast<jint>(start_line), static_cast<jint>(end_line)};
  env->SetIntArrayRegion(result, 0, 2, data);
  return result;
}

jbyteArray get_scroll_metrics_jni(JNIEnv* env, jclass, jlong editor) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  size_t size = 0;
  const uint8_t* payload = editor_get_scroll_metrics(static_cast<intptr_t>(editor), &size);
  return adopt_binary(env, payload, size);
}

jbyteArray get_selected_text_jni(JNIEnv* env, jclass, jlong editor) {
  if (editor == 0) {
    return env->NewByteArray(0);
  }
  ActiveEditor active(static_cast<intptr_t>(editor));
  const char* text = editor_get_selected_text(static_cast<intptr_t>(editor));
  return utf8_to_bytes(env, const_cast<char*>(text));
}

jbyteArray decoration_op_jni(
    JNIEnv* env,
    jclass,
    jlong editor,
    jint op,
    jbyteArray payload,
    jint a,
    jint b,
    jint c,
    jint d) {
  ActiveEditor active(static_cast<intptr_t>(editor));
  BytesView view(env, payload);
  size_t size = 0;
  const intptr_t handle = static_cast<intptr_t>(editor);
  const uint8_t* result = nullptr;
  switch (op) {
    case 1:
      result = editor_set_line_spans(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 2:
      result = editor_set_batch_line_spans(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 3:
      result = editor_register_batch_text_styles(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 4:
      result = editor_set_line_inlay_hints(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 5:
      result = editor_set_batch_line_inlay_hints(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 6:
      result = editor_set_line_phantom_texts(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 7:
      result = editor_set_batch_line_phantom_texts(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 8:
      result = editor_set_line_gutter_icons(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 9:
      result = editor_set_batch_line_gutter_icons(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 10:
      result = editor_set_line_codelens(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 11:
      result = editor_set_batch_line_codelens(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 12:
      result = editor_set_line_links(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 13:
      result = editor_set_batch_line_links(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 14:
      result = editor_set_line_diagnostics(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 15:
      result = editor_set_batch_line_diagnostics(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 16:
      result = editor_set_line_document_highlights(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 17:
      result = editor_set_batch_line_document_highlights(handle, view.ptr, static_cast<size_t>(view.len), &size);
      break;
    case 18:
      result = editor_clear_highlights(handle, &size);
      break;
    case 19:
      result = editor_clear_highlights_layer(handle, static_cast<uint8_t>(a), &size);
      break;
    case 20:
      result = editor_clear_line_spans(handle, static_cast<size_t>(a), static_cast<uint8_t>(b), &size);
      break;
    case 21:
      result = editor_clear_inlay_hints(handle, &size);
      break;
    case 22:
      result = editor_clear_phantom_texts(handle, &size);
      break;
    case 23:
      result = editor_clear_gutter_icons(handle, &size);
      break;
    case 24:
      result = editor_clear_codelens(handle, &size);
      break;
    case 25:
      result = editor_clear_links(handle, &size);
      break;
    case 26:
      result = editor_clear_diagnostics(handle, &size);
      break;
    case 27:
      result = editor_clear_document_highlights(handle, &size);
      break;
    case 28:
      result = editor_clear_all_decorations(handle, &size);
      break;
    case 29:
      result = editor_register_text_style(
          handle,
          static_cast<uint32_t>(a),
          b,
          c,
          d,
          &size);
      break;
    case 30:
      result = editor_set_max_gutter_icons(handle, static_cast<uint32_t>(a), &size);
      break;
    default:
      return nullptr;
  }
  return adopt_binary(env, result, size);
}

jbyteArray get_link_target_at_jni(JNIEnv* env, jclass, jlong editor, jint line, jint column) {
  if (editor == 0) {
    return env->NewByteArray(0);
  }
  ActiveEditor active(static_cast<intptr_t>(editor));
  const char* text = editor_get_link_target_at(
      static_cast<intptr_t>(editor),
      static_cast<size_t>(line),
      static_cast<size_t>(column));
  return utf8_to_bytes(env, const_cast<char*>(text));
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
    {"editorSetKeyMap", "(J[B)[B", (void*)set_keymap_jni},
    {"editorUpdatePointerModifiers", "(JI)[B", (void*)update_pointer_modifiers_jni},
    {"editorTickAnimations", "(J)[B", (void*)tick_jni},
    {"editorInsertText", "(J[B)[B", (void*)insert_text_jni},
    {"editorBackspace", "(J)[B", (void*)backspace_jni},
    {"editorMoveLineUp", "(J)[B", (void*)move_line_up_jni},
    {"editorMoveLineDown", "(J)[B", (void*)move_line_down_jni},
    {"editorCopyLineUp", "(J)[B", (void*)copy_line_up_jni},
    {"editorCopyLineDown", "(J)[B", (void*)copy_line_down_jni},
    {"editorDeleteLine", "(J)[B", (void*)delete_line_jni},
    {"editorInsertLineAbove", "(J)[B", (void*)insert_line_above_jni},
    {"editorInsertLineBelow", "(J)[B", (void*)insert_line_below_jni},
    {"editorUndo", "(J)[B", (void*)undo_jni},
    {"editorRedo", "(J)[B", (void*)redo_jni},
    {"editorCanUndo", "(J)Z", (void*)can_undo_jni},
    {"editorCanRedo", "(J)Z", (void*)can_redo_jni},
    {"editorSetGutterSticky", "(JZ)[B", (void*)set_gutter_sticky_jni},
    {"editorSetGutterVisible", "(JZ)[B", (void*)set_gutter_visible_jni},
    {"editorSetWrapMode", "(JI)[B", (void*)set_wrap_mode_jni},
    {"editorSetTabSize", "(JI)[B", (void*)set_tab_size_jni},
    {"editorSetInsertSpaces", "(JZ)[B", (void*)set_insert_spaces_jni},
    {"editorSetAutoIndentMode", "(JI)[B", (void*)set_auto_indent_mode_jni},
    {"editorSetBackspaceUnindent", "(JZ)[B", (void*)set_backspace_unindent_jni},
    {"editorSetScale", "(JF)[B", (void*)set_scale_jni},
    {"editorSetLineSpacing", "(JFF)[B", (void*)set_line_spacing_jni},
    {"editorSetReadOnly", "(JZ)[B", (void*)set_read_only_jni},
    {"editorSetCurrentLineRenderMode", "(JI)[B", (void*)set_current_line_render_mode_jni},
    {"editorSetEditorRenderColors", "(J[B)[B", (void*)set_editor_render_colors_jni},
    {"editorSetEditorRangeEffectStyles", "(J[B)[B", (void*)set_editor_range_effect_styles_jni},
    {"editorImeBeginSession", "(JI)[B", (void*)ime_begin_session_jni},
    {"editorImeEndSession", "(JJ)[B", (void*)ime_end_session_jni},
    {"editorImeApplyCommands", "(J[B)[B", (void*)ime_apply_commands_jni},
    {"editorImeGetState", "(JJ)[B", (void*)ime_get_state_jni},
    {"editorImeGetContext", "(JJIJJ)[B", (void*)ime_get_context_jni},
    {"editorGetCursorRect", "(J)[F", (void*)get_cursor_rect_jni},
    {"editorGetPositionRect", "(JII)[F", (void*)get_position_rect_jni},
    {"editorGetVisibleLineRange", "(J)[I", (void*)get_visible_line_range_jni},
    {"editorGetScrollMetrics", "(J)[B", (void*)get_scroll_metrics_jni},
    {"editorGetSelectedText", "(J)[B", (void*)get_selected_text_jni},
    {"editorDecorationOp", "(JI[BIIII)[B", (void*)decoration_op_jni},
    {"editorGetLinkTargetAt", "(JII)[B", (void*)get_link_target_at_jni},
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
