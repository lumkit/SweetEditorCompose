cmake_minimum_required(VERSION 3.18)
project(sweetline_compose_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT SWEETLINE_INCLUDE_DIR)
  message(FATAL_ERROR "SWEETLINE_INCLUDE_DIR is required")
endif()

add_library(sweetline_compose SHARED sweetline_jni.cpp)
target_include_directories(sweetline_compose PRIVATE ${SWEETLINE_INCLUDE_DIR})
if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
  target_compile_options(sweetline_compose PRIVATE -Wno-writable-strings)
endif()

if(ANDROID)
  if(NOT SWEETLINE_ANDROID_LIB_DIR)
    message(FATAL_ERROR "SWEETLINE_ANDROID_LIB_DIR is required on Android")
  endif()
  if(NOT ANDROID_ABI)
    message(FATAL_ERROR "ANDROID_ABI is required on Android")
  endif()
  set(_core_lib "${SWEETLINE_ANDROID_LIB_DIR}/${ANDROID_ABI}/libsweetline.so")
  if(NOT EXISTS "${_core_lib}")
    message(FATAL_ERROR "Missing SweetLine core for ${ANDROID_ABI}: ${_core_lib}")
  endif()
  add_library(sweetline SHARED IMPORTED)
  set_target_properties(sweetline PROPERTIES IMPORTED_LOCATION "${_core_lib}")
  target_link_libraries(sweetline_compose PRIVATE sweetline log)
else()
  if(NOT SWEETLINE_CORE_LIB)
    message(FATAL_ERROR "SWEETLINE_CORE_LIB is required")
  endif()
  find_package(JNI REQUIRED)
  target_include_directories(sweetline_compose PRIVATE ${JNI_INCLUDE_DIRS})
  target_link_libraries(sweetline_compose PRIVATE ${SWEETLINE_CORE_LIB})
  if(APPLE)
    set_target_properties(sweetline_compose PROPERTIES
      BUILD_RPATH "@loader_path"
      INSTALL_RPATH "@loader_path"
    )
  elseif(UNIX)
    set_target_properties(sweetline_compose PROPERTIES
      BUILD_RPATH "\$ORIGIN"
      INSTALL_RPATH "\$ORIGIN"
    )
  endif()
endif()
