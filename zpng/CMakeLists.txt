# zpng — zlib-ng-backed PNG encoder for screen content.
#
# Builds libzpng.{so,dylib,dll} from src/main/cpp/zpng.cc, statically linking
# the zlib-ng .a/.lib that was already built by the Gradle `buildZlibNgStatic`
# task. This CMakeLists is invoked from Gradle as a standalone build per
# (host_os, target_arch) combination — see zpng/build.gradle for the
# task plumbing.
#
# Required input variables (passed via -D):
#   ZLIBNG_INSTALL_DIR  — path containing include/zlib-ng.h and lib/libz-ng.a
#   ZPNG_TARGET_ARCH    — one of: x86_64, aarch64, x86 (for windows)
#   CMAKE_TOOLCHAIN_FILE — for cross-compilation (optional; native build if unset)

cmake_minimum_required(VERSION 3.20)
project(zpng CXX)

# We build PIC shared objects on every platform. C++17 is required for the
# encoder source.
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)

# ---------------------------------------------------------------------------
# Required inputs
# ---------------------------------------------------------------------------
if(NOT DEFINED ZLIBNG_INSTALL_DIR)
    message(FATAL_ERROR "ZLIBNG_INSTALL_DIR must be set (path with include/ and lib/)")
endif()
if(NOT DEFINED ZPNG_TARGET_ARCH)
    message(FATAL_ERROR "ZPNG_TARGET_ARCH must be set (x86_64, aarch64, or x86)")
endif()

message(STATUS "zpng: target arch = ${ZPNG_TARGET_ARCH}")
message(STATUS "zpng: zlib-ng prefix = ${ZLIBNG_INSTALL_DIR}")

# ---------------------------------------------------------------------------
# Locate the zlib-ng static library that the Gradle buildZlibNgStatic task
# produced. Naming differs between MSVC (zlibstatic-ng.lib) and Unix toolchains
# (libz-ng.a).
# ---------------------------------------------------------------------------
if(MSVC)
    set(ZLIBNG_STATIC_LIB "${ZLIBNG_INSTALL_DIR}/lib/zlibstatic-ng.lib")
else()
    set(ZLIBNG_STATIC_LIB "${ZLIBNG_INSTALL_DIR}/lib/libz-ng.a")
endif()

if(NOT EXISTS "${ZLIBNG_STATIC_LIB}")
    message(FATAL_ERROR
        "zlib-ng static lib not found at ${ZLIBNG_STATIC_LIB}. "
        "Run the buildZlibNgStatic Gradle task first.")
endif()

# ---------------------------------------------------------------------------
# Library target
# ---------------------------------------------------------------------------
add_library(zpng SHARED src/main/cpp/zpng.cc)

target_include_directories(zpng
    PRIVATE
        src/main/headers
        "${ZLIBNG_INSTALL_DIR}/include")

# ---------------------------------------------------------------------------
# Per-arch SIMD flags.
#
# On x86_64: enable AVX2 + PCLMUL. The encoder's #ifdef __AVX2__ blocks
# only kick in if the compiler defines __AVX2__, which requires either
# -mavx2 (GCC/Clang) or /arch:AVX2 (MSVC).
#
# On aarch64: NEON is part of the mandatory ARMv8 baseline, so no extra
# flag is needed. The encoder's #if defined(__ARM_NEON) || defined(__aarch64__)
# guard activates automatically.
#
# On x86 (32-bit): skip SIMD flags. The original FPNG build supported it
# only to keep cpp-library's path layout sane; we don't actually need
# AVX2 there.
# ---------------------------------------------------------------------------
if(ZPNG_TARGET_ARCH STREQUAL "x86_64")
    if(MSVC)
        target_compile_options(zpng PRIVATE /arch:AVX2)
    else()
        target_compile_options(zpng PRIVATE
            -march=x86-64 -mtune=generic
            -msse4.2 -mpclmul -mavx2)
    endif()
elseif(ZPNG_TARGET_ARCH STREQUAL "aarch64")
    if(NOT MSVC)
        target_compile_options(zpng PRIVATE
            -march=armv8-a -mtune=generic)
    endif()
    # MSVC ARM64: no extra flags; ARMv8 baseline is implicit.
endif()

# ---------------------------------------------------------------------------
# Common compile flags
# ---------------------------------------------------------------------------
if(MSVC)
    target_compile_options(zpng PRIVATE
        /O2 /DNDEBUG /W3
        /wd4244 /wd4267 /wd4146)  # int truncation, unsigned/signed mismatches
else()
    target_compile_options(zpng PRIVATE
        -O3 -DNDEBUG
        -Wall -Wextra -Wno-unused-parameter -Wno-unused-variable)
endif()

# ---------------------------------------------------------------------------
# Linking
#
# We link zlib-ng statically (full path, positional argument). The shared
# object then exports only the 11 zpng-namespace symbols — the zng_*
# symbols from the static archive are scoped via -fvisibility=hidden and
# (on GNU ld) --exclude-libs,ALL.
#
# Apple's ld64 hard-errors on --exclude-libs (GNU-only flag), so we omit
# it for AppleClang. Symbol scoping there relies on -fvisibility=hidden
# alone, which is sufficient given the explicit ZPNG_EXPORT attributes on
# the public API.
# ---------------------------------------------------------------------------
target_link_libraries(zpng PRIVATE "${ZLIBNG_STATIC_LIB}")

if(NOT MSVC AND NOT APPLE)
    # GNU ld on Linux. The flag tells the linker to hide all symbols
    # transitively pulled from any static archive, so the libzpng.so's
    # dynamic symbol table contains only the ZPNG_EXPORT-marked functions.
    target_link_options(zpng PRIVATE -Wl,--exclude-libs,ALL)
endif()

if(MSVC)
    # MSVC: no equivalent of --exclude-libs. The header's __declspec(dllexport)
    # already restricts what gets exported via the .def-less mechanism, so
    # zng_* symbols don't leak into the import library.
    set_target_properties(zpng PROPERTIES
        WINDOWS_EXPORT_ALL_SYMBOLS OFF)
endif()

# ---------------------------------------------------------------------------
# Output naming
#
# We want the resulting file to be exactly `libzpng.so` / `libzpng.dylib` /
# `zpng.dll`, matching what the cpp-library plugin used to produce and what
# the workflow's verification step expects to find at
# build/native-jar-staging/<os>/<arch>/[stripped/]<file>.
# ---------------------------------------------------------------------------
set_target_properties(zpng PROPERTIES
    PREFIX "lib"     # libzpng on Unix
    OUTPUT_NAME "zpng")
if(WIN32)
    # Windows naming: zpng.dll (no "lib" prefix), zpng.lib (import library).
    set_target_properties(zpng PROPERTIES PREFIX "")
endif()

# ---------------------------------------------------------------------------
# Install
# ---------------------------------------------------------------------------
include(GNUInstallDirs)
install(TARGETS zpng
    RUNTIME DESTINATION "${CMAKE_INSTALL_BINDIR}"   # .dll on Windows
    LIBRARY DESTINATION "${CMAKE_INSTALL_LIBDIR}"   # .so / .dylib on Unix
    ARCHIVE DESTINATION "${CMAKE_INSTALL_LIBDIR}")  # .lib import lib on Windows