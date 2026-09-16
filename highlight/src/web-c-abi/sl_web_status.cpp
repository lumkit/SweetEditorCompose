#include <sweetline/c_sweetline.h>

#include <stdint.h>

extern "C" {

int32_t sl_web_compile_json(sl_engine_handle_t engine, const char* json, const char** message_out) {
  sl_syntax_error_t error = sl_engine_compile_json(engine, json);
  if (message_out != nullptr) {
    *message_out = error.err_msg;
  }
  return static_cast<int32_t>(error.err_code);
}

int32_t sl_web_compile_file(sl_engine_handle_t engine, const char* path, const char** message_out) {
  sl_syntax_error_t error = sl_engine_compile_file(engine, path);
  if (message_out != nullptr) {
    *message_out = error.err_msg;
  }
  return static_cast<int32_t>(error.err_code);
}

}
