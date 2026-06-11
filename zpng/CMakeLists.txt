# zpng — zlib-ng-backed PNG encoder for screen content.
#
# Builds libzpng.{so,dylib,dll} from src/main/cpp/, statically linking
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
#
# SIMD is dispatched at RUNTIME (CPUID + XGETBV in zpng_dispatch.cc), so the
# source is split into per-ISA translation units. The kernel TUs are
# self-guarded by architecture macros and compile to empty objects on the
# wrong target, so the source list is the same for every arch — only the
# per-source compile options below differ.
# ---------------------------------------------------------------------------
set(ZPNG_BASELINE_SOURCES
    src/main/cpp/zpng.cc                  # framing, deflate glue, public ABI
    src/main/cpp/zpng_dispatch.cc         # CPUID/XGETBV kernel selection
    src/main/cpp/zpng_kernels_scalar.cc   # portable fallback / reference
    src/main/cpp/zpng_kernels_neon.cc)    # ARM64 (armv8-a baseline; empty on x86)

set(ZPNG_SSE41_SOURCES
    src/main/cpp/zpng_kernels_sse41.cc)   # x86 only; empty object on ARM

set(ZPNG_AVX2_SOURCES
    src/main/cpp/zpng_kernels_avx2.cc)    # x86 only; empty object on ARM

add_library(zpng SHARED
    ${ZPNG_BASELINE_SOURCES}
    ${ZPNG_SSE41_SOURCES}
    ${ZPNG_AVX2_SOURCES})

target_include_directories(zpng
    PRIVATE
        src/main/headers
        src/main/cpp                       # zpng_kernels.h (internal)
        "${ZLIBNG_INSTALL_DIR}/include")

# ---------------------------------------------------------------------------
# Per-arch / per-TU SIMD flags.
#
# THE ONE RULE: ISA flags (-msse4.1 / -mavx2 / /arch:AVX2) go on their
# kernel TU ONLY, via set_source_files_properties. Putting -mavx2 on the
# whole target lets the compiler auto-vectorize ordinary loops in the
# baseline TUs with VEX/ymm instructions, which SIGILLs on CPUs that mask
# AVX from CPUID (VirtualBox guests, notably under the Hyper-V
# paravirtualization fallback on Windows hosts). That is the exact bug
# the runtime-dispatch split exists to fix; the leak check at the bottom
# of this file enforces the rule at build time.
#
# On x86_64 the target-wide flags are the plain x86-64 baseline. The old
# global -msse4.2 -mpclmul are gone: the encoder no longer contains PCLMUL
# code (zng_crc32 dispatches inside zlib-ng, which is built separately
# with its own flags), and SSE4 now lives only in the sse41 kernel TU.
#
# On aarch64: NEON is part of the mandatory ARMv8 baseline, so no extra
# flag is needed; the x86 kernel TUs compile to empty objects and must
# NOT receive x86 -m flags (GCC errors on unknown options).
#
# On x86 (32-bit, Windows/MSVC only): MSVC compiles SSE4.1 and AVX2
# intrinsics without any /arch gate, so the same per-TU scheme works;
# /arch:AVX2 on the avx2 TU is valid for Win32 too. Runtime dispatch
# decides what actually executes.
# ---------------------------------------------------------------------------
if(ZPNG_TARGET_ARCH STREQUAL "x86_64" OR ZPNG_TARGET_ARCH STREQUAL "x86")
    if(MSVC)
        # SSE4.1 TU needs no /arch flag: MSVC exposes those intrinsics
        # unconditionally and emits legacy (non-VEX) encodings.
        set_source_files_properties(${ZPNG_AVX2_SOURCES}
            PROPERTIES COMPILE_OPTIONS "/arch:AVX2")
    else()
        if(ZPNG_TARGET_ARCH STREQUAL "x86_64")
            target_compile_options(zpng PRIVATE
                -march=x86-64 -mtune=generic)
        endif()
        set_source_files_properties(${ZPNG_SSE41_SOURCES}
            PROPERTIES COMPILE_OPTIONS "-msse4.1")
        set_source_files_properties(${ZPNG_AVX2_SOURCES}
            PROPERTIES COMPILE_OPTIONS "-mavx2")
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
# (on GNU ld) --exclude-libs,ALL. The internal kernel tables and
# select_kernels() are plain hidden-visibility symbols resolved at link
# time within the .so; nothing new appears in the dynamic symbol table.
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
# SIMD leak check (Linux x86_64 builds only).
#
# Post-build guard for the per-TU flag rule above: disassembles every
# object EXCEPT zpng_kernels_avx2.o and fails the build if any VEX/AVX
# instruction is found. This catches the regression class that caused the
# original VirtualBox SIGILL — e.g. someone adding -march=native or
# -mavx2 to the target-wide flags, or a new source file slipping into the
# wrong flag group.
#
# Linux-only: macOS x86_64 ships no GNU objdump by default and macOS
# hosts are being phased toward arm64 anyway; the Linux CI runner gives
# the coverage we need since the flag groups are identical per toolchain.
# ---------------------------------------------------------------------------
option(ZPNG_SIMD_LEAK_CHECK
    "Fail the build if AVX/VEX instructions appear outside the avx2 kernel TU" ON)

if(ZPNG_SIMD_LEAK_CHECK
        AND CMAKE_SYSTEM_NAME STREQUAL "Linux"
        AND ZPNG_TARGET_ARCH STREQUAL "x86_64")
    # Honor a toolchain-provided objdump first (cross builds), else PATH.
    if(CMAKE_OBJDUMP)
        set(ZPNG_OBJDUMP "${CMAKE_OBJDUMP}")
    else()
        find_program(ZPNG_OBJDUMP objdump)
    endif()
    if(ZPNG_OBJDUMP)
        add_custom_command(TARGET zpng POST_BUILD
            COMMAND sh "${CMAKE_CURRENT_SOURCE_DIR}/cmake/check-simd-leak.sh"
                    "${ZPNG_OBJDUMP}" "$<TARGET_OBJECTS:zpng>"
            COMMAND_EXPAND_LISTS
            VERBATIM
            COMMENT "zpng: verifying no AVX/VEX instructions outside zpng_kernels_avx2.o")
    else()
        message(WARNING "zpng: objdump not found; SIMD leak check skipped")
    endif()
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
