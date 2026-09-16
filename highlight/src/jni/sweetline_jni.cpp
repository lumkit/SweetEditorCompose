#include "sweetline_jni.h"

#include <jni.h>

#include <cstdint>
#include <cstring>

#include <sweetline/c_sweetline.h>

namespace {

jstring empty_if_null(JNIEnv* env, const char* text) {
  return env->NewStringUTF(text != nullptr ? text : "");
}

void throw_highlight(JNIEnv* env, int32_t code, const char* message) {
  jclass ex_class = env->FindClass("io/github/lumkit/sweeteditor/highlight/HighlightException");
  if (ex_class == nullptr) {
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), message != nullptr ? message : "SweetLine error");
    return;
  }
  jmethodID ctor = env->GetMethodID(ex_class, "<init>", "(ILjava/lang/String;Ljava/lang/Throwable;)V");
  if (ctor == nullptr) {
    env->DeleteLocalRef(ex_class);
    env->ThrowNew(env->FindClass("java/lang/RuntimeException"), message != nullptr ? message : "SweetLine error");
    return;
  }
  jstring jmessage = empty_if_null(env, message);
  jobject throwable = env->NewObject(ex_class, ctor, static_cast<jint>(code), jmessage, nullptr);
  env->DeleteLocalRef(jmessage);
  if (throwable != nullptr) {
    env->Throw(static_cast<jthrowable>(throwable));
    env->DeleteLocalRef(throwable);
  }
  env->DeleteLocalRef(ex_class);
}

void throw_syntax_error(JNIEnv* env, const sl_syntax_error_t& error) {
  throw_highlight(env, static_cast<int32_t>(error.err_code), error.err_msg);
}

class Utf8Chars {
 public:
  Utf8Chars(JNIEnv* env, jstring value) : env_(env), value_(value), chars_(nullptr) {
    if (env_ != nullptr && value_ != nullptr) {
      chars_ = env_->GetStringUTFChars(value_, nullptr);
    }
  }

  ~Utf8Chars() {
    if (env_ != nullptr && value_ != nullptr && chars_ != nullptr) {
      env_->ReleaseStringUTFChars(value_, chars_);
    }
  }

  const char* c_str() const { return chars_ != nullptr ? chars_ : ""; }

  Utf8Chars(const Utf8Chars&) = delete;
  Utf8Chars& operator=(const Utf8Chars&) = delete;

 private:
  JNIEnv* env_;
  jstring value_;
  const char* chars_;
};

jintArray adopt_int_buffer(JNIEnv* env, int32_t* buffer, jsize length) {
  if (env == nullptr || buffer == nullptr || length < 0) {
    if (buffer != nullptr) {
      sl_free_buffer(buffer);
    }
    return nullptr;
  }
  jintArray array = env->NewIntArray(length);
  if (array == nullptr) {
    sl_free_buffer(buffer);
    return nullptr;
  }
  env->SetIntArrayRegion(array, 0, length, reinterpret_cast<const jint*>(buffer));
  sl_free_buffer(buffer);
  return array;
}

jsize highlight_slice_length(const int32_t* buffer) {
  if (buffer == nullptr) {
    return 0;
  }
  const int32_t stride = buffer[1];
  const int32_t line_count = buffer[4];
  if (stride <= 0 || line_count < 0) {
    return 0;
  }
  jsize index = 5;
  for (int32_t line = 0; line < line_count; ++line) {
    const int32_t span_count = buffer[index++];
    if (span_count < 0) {
      return 0;
    }
    index += span_count * stride;
  }
  return index;
}

jsize indent_buffer_length(const int32_t* buffer) {
  if (buffer == nullptr) {
    return 0;
  }
  const int32_t line_state_count = buffer[1];
  const int32_t guide_count = buffer[2];
  if (line_state_count < 0 || guide_count < 0) {
    return 0;
  }
  jsize index = 3;
  for (int32_t i = 0; i < guide_count; ++i) {
    const int32_t branch_count = buffer[index + 4];
    if (branch_count < 0) {
      return 0;
    }
    index += 5 + branch_count * 2;
  }
  index += line_state_count * 4;
  return index;
}

jsize bracket_slice_length(const int32_t* buffer) {
  return highlight_slice_length(buffer);
}

jlong create_engine_jni(JNIEnv*, jclass, jint tab_size) {
  sl_engine_handle_t engine = sl_create_engine(false, false, static_cast<int32_t>(tab_size));
  return reinterpret_cast<jlong>(engine);
}

void free_engine_jni(JNIEnv*, jclass, jlong engine) {
  sl_free_engine(reinterpret_cast<sl_engine_handle_t>(engine));
}

void register_style_name_jni(JNIEnv* env, jclass, jlong engine, jstring name, jint style_id) {
  Utf8Chars utf8(env, name);
  sl_engine_register_style_name(
      reinterpret_cast<sl_engine_handle_t>(engine),
      utf8.c_str(),
      static_cast<int32_t>(style_id));
}

void compile_json_jni(JNIEnv* env, jclass, jlong engine, jstring json) {
  Utf8Chars utf8(env, json);
  sl_syntax_error_t error =
      sl_engine_compile_json(reinterpret_cast<sl_engine_handle_t>(engine), utf8.c_str());
  if (error.err_code != SL_OK) {
    throw_syntax_error(env, error);
  }
}

void compile_file_jni(JNIEnv* env, jclass, jlong engine, jstring path) {
  Utf8Chars utf8(env, path);
  sl_syntax_error_t error =
      sl_engine_compile_file(reinterpret_cast<sl_engine_handle_t>(engine), utf8.c_str());
  if (error.err_code != SL_OK) {
    throw_syntax_error(env, error);
  }
}

jlong create_document_jni(JNIEnv* env, jclass, jstring uri, jstring text) {
  Utf8Chars uri_utf8(env, uri);
  Utf8Chars text_utf8(env, text);
  sl_document_handle_t document = sl_create_document(uri_utf8.c_str(), text_utf8.c_str());
  return reinterpret_cast<jlong>(document);
}

void free_document_jni(JNIEnv*, jclass, jlong document) {
  sl_free_document(reinterpret_cast<sl_document_handle_t>(document));
}

jlong load_document_jni(JNIEnv*, jclass, jlong engine, jlong document) {
  sl_analyzer_handle_t analyzer = sl_engine_load_document(
      reinterpret_cast<sl_engine_handle_t>(engine),
      reinterpret_cast<sl_document_handle_t>(document));
  return reinterpret_cast<jlong>(analyzer);
}

void remove_document_jni(JNIEnv* env, jclass, jlong engine, jstring uri) {
  Utf8Chars utf8(env, uri);
  sl_engine_remove_document(reinterpret_cast<sl_engine_handle_t>(engine), utf8.c_str());
}

void free_document_analyzer_jni(JNIEnv*, jclass, jlong analyzer) {
  sl_free_document_analyzer(reinterpret_cast<sl_analyzer_handle_t>(analyzer));
}

jintArray analyze_line_range_jni(JNIEnv* env, jclass, jlong analyzer, jint start_line, jint line_count) {
  int32_t visible_range[2] = {static_cast<int32_t>(start_line), static_cast<int32_t>(line_count)};
  int32_t* buffer = sl_document_analyze_line_range(
      reinterpret_cast<sl_analyzer_handle_t>(analyzer),
      visible_range);
  return adopt_int_buffer(env, buffer, highlight_slice_length(buffer));
}

jintArray analyze_incremental_in_line_range_jni(
    JNIEnv* env,
    jclass,
    jlong analyzer,
    jint start_line,
    jint start_column,
    jint end_line,
    jint end_column,
    jstring new_text,
    jint visible_start_line,
    jint visible_line_count) {
  Utf8Chars utf8(env, new_text);
  int32_t changes_range[4] = {
      static_cast<int32_t>(start_line),
      static_cast<int32_t>(start_column),
      static_cast<int32_t>(end_line),
      static_cast<int32_t>(end_column),
  };
  int32_t visible_range[2] = {
      static_cast<int32_t>(visible_start_line),
      static_cast<int32_t>(visible_line_count),
  };
  int32_t* buffer = sl_document_analyze_incremental_in_line_range(
      reinterpret_cast<sl_analyzer_handle_t>(analyzer),
      changes_range,
      utf8.c_str(),
      visible_range);
  return adopt_int_buffer(env, buffer, highlight_slice_length(buffer));
}

jintArray get_highlight_slice_jni(JNIEnv* env, jclass, jlong analyzer, jint start_line, jint line_count) {
  int32_t visible_range[2] = {static_cast<int32_t>(start_line), static_cast<int32_t>(line_count)};
  int32_t* buffer = sl_document_get_highlight_slice(
      reinterpret_cast<sl_analyzer_handle_t>(analyzer),
      visible_range);
  return adopt_int_buffer(env, buffer, highlight_slice_length(buffer));
}

jintArray analyze_indent_guides_in_line_range_jni(
    JNIEnv* env,
    jclass,
    jlong analyzer,
    jint start_line,
    jint line_count) {
  int32_t visible_range[2] = {static_cast<int32_t>(start_line), static_cast<int32_t>(line_count)};
  int32_t* buffer = sl_document_analyze_indent_guides_in_line_range(
      reinterpret_cast<sl_analyzer_handle_t>(analyzer),
      visible_range);
  return adopt_int_buffer(env, buffer, indent_buffer_length(buffer));
}

jintArray analyze_bracket_pairs_in_line_range_jni(
    JNIEnv* env,
    jclass,
    jlong analyzer,
    jint start_line,
    jint line_count) {
  int32_t visible_range[2] = {static_cast<int32_t>(start_line), static_cast<int32_t>(line_count)};
  int32_t* buffer = sl_document_analyze_bracket_pairs_in_line_range(
      reinterpret_cast<sl_analyzer_handle_t>(analyzer),
      visible_range);
  return adopt_int_buffer(env, buffer, bracket_slice_length(buffer));
}

const JNINativeMethod kMethods[] = {
    {"createEngine", "(I)J", (void*)create_engine_jni},
    {"freeEngine", "(J)V", (void*)free_engine_jni},
    {"registerStyleName", "(JLjava/lang/String;I)V", (void*)register_style_name_jni},
    {"compileJson", "(JLjava/lang/String;)V", (void*)compile_json_jni},
    {"compileFile", "(JLjava/lang/String;)V", (void*)compile_file_jni},
    {"createDocument", "(Ljava/lang/String;Ljava/lang/String;)J", (void*)create_document_jni},
    {"freeDocument", "(J)V", (void*)free_document_jni},
    {"loadDocument", "(JJ)J", (void*)load_document_jni},
    {"removeDocument", "(JLjava/lang/String;)V", (void*)remove_document_jni},
    {"freeDocumentAnalyzer", "(J)V", (void*)free_document_analyzer_jni},
    {"analyzeLineRange", "(JII)[I", (void*)analyze_line_range_jni},
    {
        "analyzeIncrementalInLineRange",
        "(JIIIILjava/lang/String;II)[I",
        (void*)analyze_incremental_in_line_range_jni,
    },
    {"getHighlightSlice", "(JII)[I", (void*)get_highlight_slice_jni},
    {"analyzeIndentGuidesInLineRange", "(JII)[I", (void*)analyze_indent_guides_in_line_range_jni},
    {"analyzeBracketPairsInLineRange", "(JII)[I", (void*)analyze_bracket_pairs_in_line_range_jni},
};

}  // namespace

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  JNIEnv* env = nullptr;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }
  jclass jni_class = env->FindClass("io/github/lumkit/sweeteditor/highlight/jni/SweetLineJni");
  if (jni_class == nullptr) {
    return JNI_ERR;
  }
  if (env->RegisterNatives(jni_class, kMethods, sizeof(kMethods) / sizeof(kMethods[0])) != 0) {
    return JNI_ERR;
  }
  return JNI_VERSION_1_6;
}
