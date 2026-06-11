// zpng_kernels.h — internal header for the channel-swap SIMD kernels.
//
// Each ISA variant lives in its own translation unit, compiled with the
// matching instruction-set flags:
//
//   TU                       GCC/Clang flags     MSVC flags
//   ----------------------   -----------------   -----------------
//   zpng.cc                  (baseline, none)    (none)
//   zpng_dispatch.cc         (baseline, none)    (none)
//   zpng_kernels_scalar.cc   (baseline, none)    (none)
//   zpng_kernels_sse41.cc    -msse4.1            (none; SSE4.1
//                                                intrinsics need no
//                                                /arch flag on x64)
//   zpng_kernels_avx2.cc     -mavx2              /arch:AVX2
//   zpng_kernels_neon.cc     (aarch64 baseline)  (none)
//
// CRITICAL: only the *_sse41 / *_avx2 TUs may be compiled with SIMD
// flags. If -mavx2 leaks onto any other TU, the compiler auto-vectorizes
// plain loops with VEX/ymm instructions and the library SIGILLs on
// non-AVX CPUs (e.g. VirtualBox guests under Hyper-V fallback) again.
//
// Each TU is self-guarded by architecture macros, so it compiles to an
// empty object on the wrong target — but the build must still avoid
// passing x86 flags to ARM compilers and vice versa.

#ifndef ZPNG_KERNELS_H
#define ZPNG_KERNELS_H

namespace zpng {

// Function-pointer table for one ISA level. Every kernel processes the
// FULL range [0, numPixels), including the scalar tail — tails compiled
// inside an AVX2 TU are safe because that table is only ever selected
// when the CPU supports AVX2.
struct Kernels {
  void (*swapChannelsABGRtoRGBA)(unsigned char *pImage, int numPixels);
  void (*swapChannelsBGRtoRGB)(unsigned char *pImage, int numPixels);
  void (*intArgbToRgba)(const unsigned char *src, unsigned char *dst,
                        int numPixels);
  void (*intRgbToRgba)(const unsigned char *src, unsigned char *dst,
                       int numPixels);
  void (*intRgbToRgb)(const unsigned char *src, unsigned char *dst,
                      int numPixels);
  void (*intBgrToRgb)(const unsigned char *src, unsigned char *dst,
                      int numPixels);
};

extern const Kernels kScalar;

#if defined(__x86_64__) || defined(__i386__) || defined(_M_X64) || \
    defined(_M_IX86)
extern const Kernels kSse41;
extern const Kernels kAvx2;
#endif

#if defined(__aarch64__) || defined(_M_ARM64) || defined(__ARM_NEON)
extern const Kernels kNeon;
#endif

// Returns the best kernel table supported by both the CPU and the OS
// (XSAVE/YMM state is checked for AVX2). Detection runs once, lazily,
// behind a thread-safe magic static.
const Kernels &select_kernels();

} // namespace zpng

#endif // ZPNG_KERNELS_H
