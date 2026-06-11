// zpng_dispatch.cc — runtime CPU detection and kernel selection.
//
// Compile with BASELINE flags only. This TU must never contain AVX/SSE4
// instructions: it is the code that runs BEFORE we know what the CPU
// supports.
//
// Detection notes:
//   - GCC/Clang: __builtin_cpu_supports("avx2") checks the CPUID feature
//     bit AND the OS XSAVE state (XCR0 ymm bits), so it returns false on
//     CPUs/hypervisors that mask AVX (VirtualBox under Hyper-V fallback)
//     and on kernels that don't enable YMM state.
//   - MSVC: manual CPUID leaf 1 / leaf 7 + _xgetbv(0) check, equivalent
//     semantics.
//   - ARM64: NEON is baseline; no detection needed.

#include "zpng_kernels.h"

#if defined(_MSC_VER) && (defined(_M_X64) || defined(_M_IX86))
#include <intrin.h>
#endif

namespace zpng {

namespace {

#if defined(__x86_64__) || defined(__i386__) || defined(_M_X64) || \
    defined(_M_IX86)

#if defined(_MSC_VER)

bool cpu_has_sse41() {
  int r[4];
  __cpuid(r, 1);
  return (r[2] >> 19) & 1; // ECX.SSE4_1
}

bool cpu_has_avx2() {
  int r1[4];
  __cpuid(r1, 1);
  const bool osxsave = (r1[2] >> 27) & 1; // ECX.OSXSAVE
  const bool avx     = (r1[2] >> 28) & 1; // ECX.AVX
  if (!osxsave || !avx) return false;
  // XCR0 bits 1 (SSE) and 2 (YMM) must both be enabled by the OS.
  if ((_xgetbv(0) & 0x6) != 0x6) return false;
  int r7[4];
  __cpuidex(r7, 7, 0);
  return (r7[1] >> 5) & 1; // EBX.AVX2
}

#else // GCC / Clang

bool cpu_has_sse41() { return __builtin_cpu_supports("sse4.1") != 0; }
bool cpu_has_avx2()  { return __builtin_cpu_supports("avx2") != 0; }

#endif

const Kernels &pick() {
  // __builtin_cpu_init() is implied by __builtin_cpu_supports on modern
  // GCC/Clang; calling through a magic static also guarantees we are past
  // libgcc's own constructors.
  if (cpu_has_avx2())  return kAvx2;
  if (cpu_has_sse41()) return kSse41;
  return kScalar;
}

#elif defined(__aarch64__) || defined(_M_ARM64) || defined(__ARM_NEON)

const Kernels &pick() { return kNeon; }

#else

const Kernels &pick() { return kScalar; }

#endif

} // namespace

const Kernels &select_kernels() {
  // Thread-safe, lazy, evaluated exactly once (C++11 magic static).
  static const Kernels &k = pick();
  return k;
}

} // namespace zpng
