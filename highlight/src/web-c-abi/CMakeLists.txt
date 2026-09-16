cmake_minimum_required(VERSION 3.18)
project(sweetline_web_c_abi LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT EMSCRIPTEN)
  message(FATAL_ERROR "Configure this directory with emcmake (Emscripten).")
endif()

set(SWEETLINE_HOME "" CACHE PATH "SweetLine source root")
if(NOT SWEETLINE_HOME)
  message(FATAL_ERROR "SWEETLINE_HOME is required")
endif()

set(SWEETLINE_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(SWEETLINE_BUILD_SHARED OFF CACHE BOOL "" FORCE)
set(SWEETLINE_BUILD_STATIC ON CACHE BOOL "" FORCE)
set(SWEETLINE_BUILD_WASM_EMBIND OFF CACHE BOOL "" FORCE)
set(SWEETLINE_BUILD_ANDROID_JNI OFF CACHE BOOL "" FORCE)
set(SWEETLINE_BUILD_APPLE_FRAMEWORK OFF CACHE BOOL "" FORCE)

add_subdirectory("${SWEETLINE_HOME}" sweetline_src)

# Emscripten.cmake sets should_build_c_api_objects=OFF; compile C ABI here.
set(SWEETLINE_C_ABI_EXPORTS
  _malloc
  _free
  _sl_create_engine
  _sl_free_engine
  _sl_engine_register_style_name
  _sl_engine_compile_json
  _sl_engine_compile_file
  _sl_create_document
  _sl_free_document
  _sl_engine_load_document
  _sl_engine_remove_document
  _sl_free_document_analyzer
  _sl_document_analyze_line_range
  _sl_document_analyze_incremental_in_line_range
  _sl_document_get_highlight_slice
  _sl_document_analyze_indent_guides_in_line_range
  _sl_document_analyze_bracket_pairs_in_line_range
  _sl_free_buffer
)
string(JOIN "','" SWEETLINE_C_ABI_EXPORTS_JOINED ${SWEETLINE_C_ABI_EXPORTS})

add_executable(sweetline_c_abi "${SWEETLINE_HOME}/src/c_sweetline.cpp")
set_target_properties(sweetline_c_abi PROPERTIES
  OUTPUT_NAME "sweetline_c_abi"
  SUFFIX ".js"
)
target_include_directories(sweetline_c_abi PRIVATE
  "${SWEETLINE_HOME}/include"
  "${SWEETLINE_HOME}/src"
  "${SWEETLINE_HOME}/3dparty/nlohmann_json/include"
  "${SWEETLINE_HOME}/3dparty/utfcpp/include"
  "${SWEETLINE_HOME}/3dparty/oniguruma/include"
)
target_compile_definitions(sweetline_c_abi PRIVATE
  SWEETLINE_EXPORT=1
  SWEETLINE_DEBUG=1
  ONIG_STATIC=ON
  WASM
)
target_compile_options(sweetline_c_abi PRIVATE -fexceptions)
target_link_libraries(sweetline_c_abi PRIVATE
  SweetLine::sweetline_static
  "--no-entry"
  "-fexceptions"
  "-sWASM=1"
  "-sMODULARIZE=1"
  "-sEXPORT_ES6=1"
  "-sALLOW_MEMORY_GROWTH=1"
  "-sNO_EXIT_RUNTIME=1"
  "-sENVIRONMENT=web,worker,node"
  "-sEXPORT_NAME=SweetLineCAbi"
  "-sEXPORTED_FUNCTIONS=['${SWEETLINE_C_ABI_EXPORTS_JOINED}']"
  "-sEXPORTED_RUNTIME_METHODS=['ccall','cwrap','UTF8ToString','stringToUTF8','lengthBytesUTF8','getValue','setValue','wasmMemory','HEAP32','HEAPU8']"
)
