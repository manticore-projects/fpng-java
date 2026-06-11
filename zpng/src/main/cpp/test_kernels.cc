// test_kernels.cc — validates every available kernel table against the
// scalar reference, across pixel counts that exercise the AVX2 main loop,
// the SSE mid loop, and the scalar tail (0..67 pixels plus larger sizes).
//
// Compile with BASELINE flags (this TU calls through the tables only).

#include "zpng_kernels.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <vector>

static unsigned rng_state = 0x12345678u;
static unsigned char rnd() {
  rng_state = rng_state * 1664525u + 1013904223u;
  return (unsigned char)(rng_state >> 24);
}

static int failures = 0;

static void check(bool ok, const char *table, const char *fn, int n) {
  if (!ok) {
    fprintf(stderr, "FAIL %s.%s numPixels=%d\n", table, fn, n);
    ++failures;
  }
}

static void test_table(const zpng::Kernels &k, const char *name) {
  const zpng::Kernels &ref = zpng::kScalar;
  std::vector<int> sizes;
  for (int n = 0; n <= 67; ++n) sizes.push_back(n);
  sizes.push_back(1024);
  sizes.push_back(1023);
  sizes.push_back(4096 + 3);

  for (int n : sizes) {
    // Source buffers with slack so 16-byte loads past the logical end
    // (the 3-byte-pixel kernels read 16 bytes per 12) stay in bounds,
    // mirroring how the encoder allocates.
    std::vector<unsigned char> src4(n * 4 + 32), src3(n * 3 + 32);
    for (auto &b : src4) b = rnd();
    for (auto &b : src3) b = rnd();

    // In-place 4ch swap.
    {
      std::vector<unsigned char> a = src4, b = src4;
      k.swapChannelsABGRtoRGBA(a.data(), n);
      ref.swapChannelsABGRtoRGBA(b.data(), n);
      check(memcmp(a.data(), b.data(), n * 4) == 0, name,
            "swapChannelsABGRtoRGBA", n);
    }
    // In-place 3ch swap.
    {
      std::vector<unsigned char> a = src3, b = src3;
      k.swapChannelsBGRtoRGB(a.data(), n);
      ref.swapChannelsBGRtoRGB(b.data(), n);
      check(memcmp(a.data(), b.data(), n * 3) == 0, name,
            "swapChannelsBGRtoRGB", n);
    }
    // Out-of-place 4->4.
    {
      std::vector<unsigned char> a(n * 4 + 32, 0xAA), b(n * 4 + 32, 0xAA);
      k.intArgbToRgba(src4.data(), a.data(), n);
      ref.intArgbToRgba(src4.data(), b.data(), n);
      check(memcmp(a.data(), b.data(), n * 4) == 0, name, "intArgbToRgba", n);

      k.intRgbToRgba(src4.data(), a.data(), n);
      ref.intRgbToRgba(src4.data(), b.data(), n);
      check(memcmp(a.data(), b.data(), n * 4) == 0, name, "intRgbToRgba", n);
    }
    // Out-of-place 4->3.
    {
      std::vector<unsigned char> a(n * 3 + 32, 0xAA), b(n * 3 + 32, 0xAA);
      k.intRgbToRgb(src4.data(), a.data(), n);
      ref.intRgbToRgb(src4.data(), b.data(), n);
      check(memcmp(a.data(), b.data(), n * 3) == 0, name, "intRgbToRgb", n);

      k.intBgrToRgb(src4.data(), a.data(), n);
      ref.intBgrToRgb(src4.data(), b.data(), n);
      check(memcmp(a.data(), b.data(), n * 3) == 0, name, "intBgrToRgb", n);
    }
  }
  printf("table %-8s : %s\n", name, failures == 0 ? "OK" : "FAILED");
}

int main() {
  test_table(zpng::kScalar, "scalar");
#if defined(__x86_64__) || defined(__i386__)
  if (__builtin_cpu_supports("sse4.1")) test_table(zpng::kSse41, "sse4.1");
  else printf("table sse4.1   : skipped (CPU lacks SSE4.1)\n");
  if (__builtin_cpu_supports("avx2")) test_table(zpng::kAvx2, "avx2");
  else printf("table avx2     : skipped (CPU lacks AVX2)\n");
#endif
#if defined(__aarch64__)
  test_table(zpng::kNeon, "neon");
#endif

  const zpng::Kernels &sel = zpng::select_kernels();
  const char *picked = "scalar";
  if (&sel == &zpng::kScalar) picked = "scalar";
#if defined(__x86_64__) || defined(__i386__)
  else if (&sel == &zpng::kSse41) picked = "sse4.1";
  else if (&sel == &zpng::kAvx2) picked = "avx2";
#endif
#if defined(__aarch64__)
  else if (&sel == &zpng::kNeon) picked = "neon";
#endif
  printf("select_kernels() picked: %s\n", picked);

  return failures ? 1 : 0;
}
