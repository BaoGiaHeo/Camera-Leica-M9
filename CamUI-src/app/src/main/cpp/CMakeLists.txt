cmake_minimum_required(VERSION 3.22.1)
project(fujisr LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(NCNN_INCLUDE_DIR "${CMAKE_CURRENT_LIST_DIR}/third_party/ncnn/include")
set(NCNN_LIBRARY "${CMAKE_CURRENT_LIST_DIR}/../jniLibs/${ANDROID_ABI}/libncnn.so")

if(NOT EXISTS "${NCNN_INCLUDE_DIR}/net.h")
    message(FATAL_ERROR "NCNN headers missing. Run the prepare-ncnn workflow step first.")
endif()
if(NOT EXISTS "${NCNN_LIBRARY}")
    message(FATAL_ERROR "NCNN Android Vulkan shared library missing at ${NCNN_LIBRARY}")
endif()

add_library(ncnn SHARED IMPORTED)
set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION "${NCNN_LIBRARY}")

add_library(fujisr SHARED ncnn_jni.cpp)
target_include_directories(fujisr PRIVATE "${NCNN_INCLUDE_DIR}")

find_library(android-lib android REQUIRED)
find_library(log-lib log REQUIRED)
find_library(jnigraphics-lib jnigraphics REQUIRED)
find_library(vulkan-lib vulkan)

# These feature flags must match the shipped NCNN Android Vulkan build.
# NCNN's public headers conditionally expose the GPU API, named blobs and pixel converters.
target_compile_definitions(fujisr PRIVATE
    NCNN_VULKAN=1
    NCNN_STRING=1
    NCNN_PIXEL=1
)

target_compile_options(fujisr PRIVATE
    -O3
    -fexceptions
    -frtti
    -fvisibility=hidden
    -Wall
    -Wextra
    -Wno-unused-parameter
)

target_link_libraries(fujisr PRIVATE
    ncnn
    ${android-lib}
    ${log-lib}
    ${jnigraphics-lib}
)

if(vulkan-lib)
    target_link_libraries(fujisr PRIVATE ${vulkan-lib})
endif()
