cmake_minimum_required(VERSION 3.18)
project(sweeteditor_compose_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT SWEETEDITOR_INCLUDE_DIR)
  message(FATAL_ERROR "SWEETEDITOR_INCLUDE_DIR is required")
endif()

add_library(sweeteditor_compose SHARED sweeteditor_jni.cpp)
target_include_directories(sweeteditor_compose PRIVATE ${SWEETEDITOR_INCLUDE_DIR})
if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
  target_compile_options(sweeteditor_compose PRIVATE -Wno-writable-strings)
endif()

if(ANDROID)
  if(NOT SWEETEDITOR_ANDROID_LIB_DIR)
    message(FATAL_ERROR "SWEETEDITOR_ANDROID_LIB_DIR is required on Android")
  endif()
  if(NOT ANDROID_ABI)
    message(FATAL_ERROR "ANDROID_ABI is required on Android")
  endif()
  set(_core_lib "${SWEETEDITOR_ANDROID_LIB_DIR}/${ANDROID_ABI}/libsweeteditor.so")
  if(NOT EXISTS "${_core_lib}")
    message(FATAL_ERROR "Missing SweetEditor core for ${ANDROID_ABI}: ${_core_lib}")
  endif()
  add_library(sweeteditor SHARED IMPORTED)
  set_target_properties(sweeteditor PROPERTIES IMPORTED_LOCATION "${_core_lib}")
  target_link_libraries(sweeteditor_compose PRIVATE sweeteditor log)
else()
  if(NOT SWEETEDITOR_CORE_LIB)
    message(FATAL_ERROR "SWEETEDITOR_CORE_LIB is required")
  endif()
  find_package(JNI REQUIRED)
  target_include_directories(sweeteditor_compose PRIVATE ${JNI_INCLUDE_DIRS})
  target_link_libraries(sweeteditor_compose PRIVATE ${SWEETEDITOR_CORE_LIB})
  if(APPLE)
    set_target_properties(sweeteditor_compose PROPERTIES
      BUILD_RPATH "@loader_path"
      INSTALL_RPATH "@loader_path"
    )
  elseif(UNIX)
    set_target_properties(sweeteditor_compose PROPERTIES
      BUILD_RPATH "\$ORIGIN"
      INSTALL_RPATH "\$ORIGIN"
    )
  endif()
endif()
