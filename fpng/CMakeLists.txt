# fpng — Rich Geldreich's fast PNG encoder.
#
# Builds libfpng.{so,dylib,dll} from src/main/cpp/fpng.cc. No external library
# dependencies. This CMakeLists replaces the cpp-library plugin that used to
# drive this build, sidestepping its broken Windows ARM64 support
# (gradle/gradle#21703 — the WindowsRegistry native service isn't built for
# Windows ARM64, which crashes any cpp-library project at configuration time
# on that platform).
#
# Invoked from Gradle as a standalone build per host. See fpng/build.gradle
# for the task plumbing.
#
# Optional input variables (passed via -D):
#   FPNG_PGO_STAGE    — 'generate' or 'use' for profile-guided optimization
#   FPNG_PGO_DIR      — directory for .gcda files when PGO is in play

cmake_minimum_required(VERSION 3.20)
project(fpng CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# NOTE: We deliberately do NOT set CMAKE_CXX_VISIBILITY_PRESET=hidden here.
#
# fpng's EXPORT macro (defined in fpng.h) is geared toward Windows
# __declspec(dllexport) and may not include __attribute__((visibility("default")))
# on Linux/macOS. With visibility=hidden, ALL symbols (including the
# extern "C" fpng_init, swapChannelsABGRtoRGBA, etc.) get hidden, and the
# resulting .so has no usable exports. JNA-side dlsym then fails with
# "undefined symbol: fpng_init".
#
# The original cpp-library plugin used the toolchain default (=visible on
# Linux) and that's what fpng.h was written to expect. Matching that here.
# The .so will export some internal C++ symbols too, but JNA only looks up
# the names it cares about, so the extra surface is harmless.

# ---------------------------------------------------------------------------
# Library target
#
# Single explicit source file. The upstream filename is fpng.cpp (with .cpp
# extension, not .cc). We list it explicitly rather than globbing src/main/cpp/*
# because:
#   - The directory may contain auxiliary files (test harnesses, alternative
#     encoders, third-party deps) that shouldn't be linked into the shared lib
#   - Explicit listing makes "what's in the .so" obvious from this file
# If you add JNI glue or other source files that DO belong in libfpng, list
# them here.
# ---------------------------------------------------------------------------
add_library(fpng SHARED src/main/cpp/fpng.cpp)

target_include_directories(fpng PRIVATE src/main/headers)

# ---------------------------------------------------------------------------
# Compiler flags
#
# These mirror what fpng/build.gradle's cpp-library configuration was passing.
# x86_64-only for now: fpng uses SSE/AVX intrinsics with no NEON port (per
# the comment that was in the original build.gradle's targetMachines block).
# ---------------------------------------------------------------------------
if(MSVC)
    target_compile_options(fpng PRIVATE
        /EHsc /Ox /std:c++17 /W4 /arch:AVX2
        # MSVC equivalents of -fno-strict-aliasing and gcc warnings: not needed,
        # MSVC's optimizer doesn't have the same aliasing assumptions.
    )
else()
    target_compile_options(fpng PRIVATE
        -fPIC -O3 -march=x86-64 -mtune=generic
        -funroll-loops -std=c++17 -Wall
        -msse4.2 -mpclmul -msse2 -mavx2
        -fno-strict-aliasing
    )
endif()

# ---------------------------------------------------------------------------
# Profile-Guided Optimization (PGO) — GCC/Clang only
#
# Three-stage workflow toggled via -DFPNG_PGO_STAGE=<generate|use>:
#
#   * (unset / 'none')   Normal build, no PGO flags.
#   * generate           Compile/link with -fprofile-generate. The resulting
#                        binary writes .gcda profile data to FPNG_PGO_DIR
#                        when exercised.
#   * use                Compile/link with -fprofile-use, consuming the
#                        .gcda data from the previous stage. Adds
#                        -fprofile-correction to tolerate small source drift
#                        (e.g. comment-only edits shifting line numbers).
#
# Profile data lives outside the build dir (fpng/build.gradle sets
# FPNG_PGO_DIR to <repo-root>/.pgo/fpng/) so `clean` between stages doesn't
# wipe it.
# ---------------------------------------------------------------------------
if(NOT MSVC AND DEFINED FPNG_PGO_STAGE AND NOT FPNG_PGO_STAGE STREQUAL "none")
    if(NOT DEFINED FPNG_PGO_DIR)
        message(FATAL_ERROR "FPNG_PGO_STAGE=${FPNG_PGO_STAGE} requires FPNG_PGO_DIR")
    endif()
    if(FPNG_PGO_STAGE STREQUAL "generate")
        target_compile_options(fpng PRIVATE "-fprofile-generate=${FPNG_PGO_DIR}")
        target_link_options(fpng PRIVATE "-fprofile-generate=${FPNG_PGO_DIR}")
    elseif(FPNG_PGO_STAGE STREQUAL "use")
        target_compile_options(fpng PRIVATE
            "-fprofile-use=${FPNG_PGO_DIR}"
            -fprofile-correction)
        target_link_options(fpng PRIVATE "-fprofile-use=${FPNG_PGO_DIR}")
    else()
        message(FATAL_ERROR "FPNG_PGO_STAGE must be 'generate', 'use', or 'none'")
    endif()
    message(STATUS "fpng PGO: stage=${FPNG_PGO_STAGE}, dir=${FPNG_PGO_DIR}")
endif()

# ---------------------------------------------------------------------------
# Linking
#
# fpng has no external dependencies. The original Gradle build linked
# libgcov unconditionally on GCC, which broke macOS Clang. We follow the
# zpng pattern: only emit -lgcov when actually doing PGO, only on GCC.
# ---------------------------------------------------------------------------
if(NOT MSVC AND NOT APPLE AND DEFINED FPNG_PGO_STAGE AND
   NOT FPNG_PGO_STAGE STREQUAL "none")
    target_link_libraries(fpng PRIVATE gcov)
endif()

# ---------------------------------------------------------------------------
# Output naming
#
# Match cpp-library's output: libfpng.so / libfpng.dylib / fpng.dll
# ---------------------------------------------------------------------------
set_target_properties(fpng PROPERTIES
    PREFIX "lib"
    OUTPUT_NAME "fpng")
if(WIN32)
    set_target_properties(fpng PROPERTIES PREFIX "")
endif()

# ---------------------------------------------------------------------------
# Install
# ---------------------------------------------------------------------------
include(GNUInstallDirs)
install(TARGETS fpng
    RUNTIME DESTINATION "${CMAKE_INSTALL_BINDIR}"
    LIBRARY DESTINATION "${CMAKE_INSTALL_LIBDIR}"
    ARCHIVE DESTINATION "${CMAKE_INSTALL_LIBDIR}")