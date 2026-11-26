cmake_minimum_required(VERSION 3.22.1)
project(fuji_jpeg_tile LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

include(ExternalProject)

# Build the upstream libjpeg-compatible API for the active Android ABI.
# The native pipeline writes one JPEG incrementally with jpeg_write_scanlines;
# it never allocates a full 200MP output bitmap.
set(LIBJPEG_INSTALL_DIR "${CMAKE_CURRENT_BINARY_DIR}/libjpeg-turbo-install")
set(LIBJPEG_ARCHIVE "${LIBJPEG_INSTALL_DIR}/lib/libjpeg.a")

ExternalProject_Add(fuji_libjpeg_turbo
    GIT_REPOSITORY https://github.com/libjpeg-turbo/libjpeg-turbo.git
    GIT_TAG 3.1.3
    GIT_SHALLOW TRUE
    UPDATE_DISCONNECTED TRUE
    INSTALL_DIR "${LIBJPEG_INSTALL_DIR}"
    CMAKE_ARGS
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=android-29
        -DCMAKE_BUILD_TYPE=Release
        -DCMAKE_INSTALL_PREFIX=<INSTALL_DIR>
        -DCMAKE_INSTALL_LIBDIR=lib
        -DCMAKE_POSITION_INDEPENDENT_CODE=ON
        -DENABLE_SHARED=OFF
        -DENABLE_STATIC=ON
        -DWITH_SIMD=OFF
        -DWITH_TURBOJPEG=OFF
        -DWITH_TOOLS=OFF
        -DWITH_TESTS=OFF
        -DWITH_JAVA=OFF
    BUILD_BYPRODUCTS "${LIBJPEG_ARCHIVE}"
)

# CMake validates imported include directories at configure time, before ExternalProject
# has downloaded and installed libjpeg-turbo. Create the directories now; the external
# project populates the real headers/archive before the dependent native target compiles.
file(MAKE_DIRECTORY "${LIBJPEG_INSTALL_DIR}/include")
file(MAKE_DIRECTORY "${LIBJPEG_INSTALL_DIR}/lib")

add_library(fuji_libjpeg_static STATIC IMPORTED GLOBAL)
set_target_properties(fuji_libjpeg_static PROPERTIES
    IMPORTED_LOCATION "${LIBJPEG_ARCHIVE}"
    INTERFACE_INCLUDE_DIRECTORIES "${LIBJPEG_INSTALL_DIR}/include"
)

add_library(fuji_jpeg_tile SHARED JpegTileEncoder.cpp)
add_dependencies(fuji_jpeg_tile fuji_libjpeg_turbo)
target_include_directories(fuji_jpeg_tile PRIVATE "${LIBJPEG_INSTALL_DIR}/include")
target_compile_options(fuji_jpeg_tile PRIVATE -O3 -fno-exceptions)
target_link_libraries(fuji_jpeg_tile PRIVATE fuji_libjpeg_static android log m)
