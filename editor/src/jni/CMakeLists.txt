cmake_minimum_required(VERSION 3.18)
project(sweeteditor_compose_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT SWEETEDITOR_INCLUDE_DIR)
  message(FATAL_ERROR "SWEETEDITOR_INCLUDE_DIR is required")
endif()
if(NOT SWEETEDITOR_CORE_LIB)
  message(FATAL_ERROR "SWEETEDITOR_CORE_LIB is required")
endif()

find_package(JNI REQUIRED)

add_library(sweeteditor_compose SHARED sweeteditor_jni.cpp)
target_include_directories(sweeteditor_compose PRIVATE ${SWEETEDITOR_INCLUDE_DIR} ${JNI_INCLUDE_DIRS})
if(CMAKE_CXX_COMPILER_ID MATCHES "Clang")
  target_compile_options(sweeteditor_compose PRIVATE -Wno-writable-strings)
endif()
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
