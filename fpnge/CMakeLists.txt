# fpnge — Google's PNG encoder (https://github.com/veluca93/fpnge).
#
# Builds libfpnge.{so,dylib,dll} from src/main/cpp/fpnge.cc. No external
# library dependencies. Replaces the cpp-library plugin that used to drive
# this build, sidestepping its broken Windows ARM64 support
# (gradle/gradle#21703 — the WindowsRegistry native service isn't built for
# Windows ARM64, which crashes any cpp-library project at configuration time
# on that platform).
#
# Invoked from Gradle as a standalone build per host. See fpnge/build.gradle
# for the task plumbing.
#
# Optional input variables (passed via -D):
#   FPNGE_PGO_STAGE    — 'generate' or 'use' for profile-guided optimization
#   FPNGE_PGO_DIR      — directory for .gcda files when PGO is in play

cmake_minimum_required(VERSION 3.20)
project(fpnge CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# NOTE: We deliberately do NOT set CMAKE_CXX_VISIBILITY_PRESET=hidden here.
# Same reason as fpng: fpnge.h's EXPORT macro doesn't include the GCC/Clang
# visibility attribute on Linux/macOS, so visibility=hidden would silently
# strip all exported C symbols (FPNGEEncode, FPNGEFillOptions, etc.) from
# the built .so. The original cpp-library plugin used default visibility
# and that's what the source was written for.

# ---------------------------------------------------------------------------
# Library target
#
# Single source file. The src/main/cpp/ directory in this repo also contains:
#   - fpnge_main.cc   (upstream Google CLI tool with main(); not part of the
#                      shared library)
#   - lodepng.cpp     (LodePNG, used only by fpnge_main.cc to decode the
#                      input PNG; not part of the shared library)
# These exist for manual benchmarking via the upstream CLI but are NOT
# linked into libfpnge. Listing fpnge.cc explicitly (rather than globbing
# src/main/cpp/*) ensures they stay out — globbing would silently pull
# them in and break the shared library by adding a duplicate main() and
# bloating it with the LodePNG decoder.
# ---------------------------------------------------------------------------
add_library(fpnge SHARED src/main/cpp/fpnge.cc)

target_include_directories(fpnge PRIVATE src/main/headers)

# ---------------------------------------------------------------------------
# Compiler flags
#
# These mirror what fpnge/build.gradle's cpp-library configuration was passing.
# x86_64-only for now: fpnge uses SSE/AVX intrinsics with no NEON port (per
# the comment that was in the original build.gradle's targetMachines block).
# ---------------------------------------------------------------------------
if(MSVC)
    target_compile_options(fpnge PRIVATE
        /EHsc /Ox /std:c++17 /W4 /arch:AVX2
        # MSVC equivalents of -fno-strict-aliasing and gcc warnings: not needed,
        # MSVC's optimizer doesn't have the same aliasing assumptions.
    )
else()
    target_compile_options(fpnge PRIVATE
        -fPIC -O3 -march=x86-64 -mtune=generic
        -funroll-loops -std=c++17 -Wall
        -msse4.2 -mpclmul -msse2 -mavx2
        -fno-strict-aliasing
    )
endif()

# ---------------------------------------------------------------------------
# Profile-Guided Optimization (PGO) — GCC/Clang only
#
# Three-stage workflow toggled via -DFPNGE_PGO_STAGE=<generate|use>:
#
#   * (unset / 'none')   Normal build, no PGO flags.
#   * generate           Compile/link with -fprofile-generate. The resulting
#                        binary writes .gcda profile data to FPNGE_PGO_DIR
#                        when exercised.
#   * use                Compile/link with -fprofile-use, consuming the
#                        .gcda data from the previous stage. Adds
#                        -fprofile-correction to tolerate small source drift
#                        (e.g. comment-only edits shifting line numbers).
#
# Profile data lives outside the build dir (fpnge/build.gradle sets
# FPNGE_PGO_DIR to <repo-root>/.pgo/fpnge/) so `clean` between stages doesn't
# wipe it.
# ---------------------------------------------------------------------------
if(NOT MSVC AND DEFINED FPNGE_PGO_STAGE AND NOT FPNGE_PGO_STAGE STREQUAL "none")
    if(NOT DEFINED FPNGE_PGO_DIR)
        message(FATAL_ERROR "FPNGE_PGO_STAGE=${FPNGE_PGO_STAGE} requires FPNGE_PGO_DIR")
    endif()
    if(FPNGE_PGO_STAGE STREQUAL "generate")
        target_compile_options(fpnge PRIVATE "-fprofile-generate=${FPNGE_PGO_DIR}")
        target_link_options(fpnge PRIVATE "-fprofile-generate=${FPNGE_PGO_DIR}")
    elseif(FPNGE_PGO_STAGE STREQUAL "use")
        target_compile_options(fpnge PRIVATE
            "-fprofile-use=${FPNGE_PGO_DIR}"
            -fprofile-correction)
        target_link_options(fpnge PRIVATE "-fprofile-use=${FPNGE_PGO_DIR}")
    else()
        message(FATAL_ERROR "FPNGE_PGO_STAGE must be 'generate', 'use', or 'none'")
    endif()
    message(STATUS "fpnge PGO: stage=${FPNGE_PGO_STAGE}, dir=${FPNGE_PGO_DIR}")
endif()

# ---------------------------------------------------------------------------
# Linking
#
# fpnge has no external dependencies. The original Gradle build linked
# libgcov unconditionally on GCC, which broke macOS Clang. We follow the
# zpng pattern: only emit -lgcov when actually doing PGO, only on GCC.
# ---------------------------------------------------------------------------
if(NOT MSVC AND NOT APPLE AND DEFINED FPNGE_PGO_STAGE AND
   NOT FPNGE_PGO_STAGE STREQUAL "none")
    target_link_libraries(fpnge PRIVATE gcov)
endif()

# ---------------------------------------------------------------------------
# Output naming
#
# Match cpp-library's output: libfpnge.so / libfpnge.dylib / fpnge.dll
# ---------------------------------------------------------------------------
set_target_properties(fpnge PROPERTIES
    PREFIX "lib"
    OUTPUT_NAME "fpnge")
if(WIN32)
    set_target_properties(fpnge PROPERTIES PREFIX "")
endif()

# ---------------------------------------------------------------------------
# Install
# ---------------------------------------------------------------------------
include(GNUInstallDirs)
install(TARGETS fpnge
    RUNTIME DESTINATION "${CMAKE_INSTALL_BINDIR}"
    LIBRARY DESTINATION "${CMAKE_INSTALL_LIBDIR}"
    ARCHIVE DESTINATION "${CMAKE_INSTALL_LIBDIR}")