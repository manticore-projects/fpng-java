// zpng_kernels_sse41.cc — SSE4.1 kernels.
//
// Compile with: GCC/Clang -msse4.1; MSVC needs no /arch flag (SSE4.1
// intrinsics are always available on x64 and emit legacy-encoded SSE,
// not VEX). x86-only: compiles to an empty object elsewhere.
//
// The bodies are the SSE blocks from the original monolithic zpng.cc,
// each followed by its scalar tail. The tail code in this TU may be
// auto-vectorized with SSE4.1 — fine, this table is only selected when
// SSE4.1 is present.

#include "zpng_kernels.h"

#if defined(__x86_64__) || defined(__i386__) || defined(_M_X64) || \
    defined(_M_IX86)

#include <smmintrin.h>
#include <stdint.h>

namespace zpng {
namespace {

void swapChannelsABGRtoRGBA_sse41(unsigned char *pImage, int numPixels) {
  const __m128i mask = _mm_set_epi8(12, 13, 14, 15, 8, 9, 10, 11,
                                    4, 5, 6, 7, 0, 1, 2, 3);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    __m128i v = _mm_loadu_si128((__m128i *)(pImage + i * 4));
    _mm_storeu_si128((__m128i *)(pImage + i * 4), _mm_shuffle_epi8(v, mask));
  }
  for (; i < numPixels; ++i) {
    unsigned char a = pImage[i * 4 + 0], b = pImage[i * 4 + 1];
    unsigned char g = pImage[i * 4 + 2], r = pImage[i * 4 + 3];
    pImage[i * 4 + 0] = r;
    pImage[i * 4 + 1] = g;
    pImage[i * 4 + 2] = b;
    pImage[i * 4 + 3] = a;
  }
}

void swapChannelsBGRtoRGB_sse41(unsigned char *pImage, int numPixels) {
  const __m128i mask =
      _mm_set_epi8(-1, -1, -1, -1, 9, 10, 11, 6, 7, 8, 3, 4, 5, 0, 1, 2);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    unsigned char *p = pImage + i * 3;
    __m128i v = _mm_loadu_si128((__m128i *)p);
    __m128i s = _mm_shuffle_epi8(v, mask);
    _mm_storel_epi64((__m128i *)p, s);
    *(uint32_t *)(p + 8) = (uint32_t)_mm_extract_epi32(s, 2);
  }
  for (; i < numPixels; ++i) {
    unsigned char *p = pImage + i * 3;
    unsigned char t = p[0];
    p[0] = p[2];
    p[2] = t;
  }
}

void intArgbToRgba_sse41(const unsigned char *src, unsigned char *dst,
                         int numPixels) {
  const __m128i mask = _mm_set_epi8(15, 12, 13, 14, 11, 8, 9, 10,
                                    7, 4, 5, 6, 3, 0, 1, 2);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    __m128i v = _mm_loadu_si128((const __m128i *)(src + i * 4));
    _mm_storeu_si128((__m128i *)(dst + i * 4), _mm_shuffle_epi8(v, mask));
  }
  for (; i < numPixels; ++i) {
    unsigned char b = src[i * 4 + 0], g = src[i * 4 + 1];
    unsigned char r = src[i * 4 + 2], a = src[i * 4 + 3];
    dst[i * 4 + 0] = r;
    dst[i * 4 + 1] = g;
    dst[i * 4 + 2] = b;
    dst[i * 4 + 3] = a;
  }
}

void intRgbToRgba_sse41(const unsigned char *src, unsigned char *dst,
                        int numPixels) {
  const __m128i mask = _mm_set_epi8(15, 12, 13, 14, 11, 8, 9, 10,
                                    7, 4, 5, 6, 3, 0, 1, 2);
  const __m128i alpha = _mm_set1_epi32((int)0xFF000000);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    __m128i v = _mm_loadu_si128((const __m128i *)(src + i * 4));
    v = _mm_or_si128(_mm_shuffle_epi8(v, mask), alpha);
    _mm_storeu_si128((__m128i *)(dst + i * 4), v);
  }
  for (; i < numPixels; ++i) {
    unsigned char b = src[i * 4 + 0], g = src[i * 4 + 1], r = src[i * 4 + 2];
    dst[i * 4 + 0] = r;
    dst[i * 4 + 1] = g;
    dst[i * 4 + 2] = b;
    dst[i * 4 + 3] = 0xFF;
  }
}

void intRgbToRgb_sse41(const unsigned char *src, unsigned char *dst,
                       int numPixels) {
  const __m128i mask =
      _mm_set_epi8((char)0x80, (char)0x80, (char)0x80, (char)0x80,
                   12, 13, 14, 8, 9, 10, 4, 5, 6, 0, 1, 2);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    __m128i v = _mm_loadu_si128((const __m128i *)(src + i * 4));
    __m128i s = _mm_shuffle_epi8(v, mask);
    _mm_storel_epi64((__m128i *)(dst + i * 3), s);
    *(uint32_t *)(dst + i * 3 + 8) = (uint32_t)_mm_extract_epi32(s, 2);
  }
  for (; i < numPixels; ++i) {
    unsigned char b = src[i * 4 + 0], g = src[i * 4 + 1], r = src[i * 4 + 2];
    dst[i * 3 + 0] = r;
    dst[i * 3 + 1] = g;
    dst[i * 3 + 2] = b;
  }
}

void intBgrToRgb_sse41(const unsigned char *src, unsigned char *dst,
                       int numPixels) {
  const __m128i mask =
      _mm_set_epi8((char)0x80, (char)0x80, (char)0x80, (char)0x80,
                   14, 13, 12, 10, 9, 8, 6, 5, 4, 2, 1, 0);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    __m128i v = _mm_loadu_si128((const __m128i *)(src + i * 4));
    __m128i s = _mm_shuffle_epi8(v, mask);
    _mm_storel_epi64((__m128i *)(dst + i * 3), s);
    *(uint32_t *)(dst + i * 3 + 8) = (uint32_t)_mm_extract_epi32(s, 2);
  }
  for (; i < numPixels; ++i) {
    unsigned char r = src[i * 4 + 0], g = src[i * 4 + 1], b = src[i * 4 + 2];
    dst[i * 3 + 0] = r;
    dst[i * 3 + 1] = g;
    dst[i * 3 + 2] = b;
  }
}

} // namespace

const Kernels kSse41 = {
    swapChannelsABGRtoRGBA_sse41, swapChannelsBGRtoRGB_sse41,
    intArgbToRgba_sse41,          intRgbToRgba_sse41,
    intRgbToRgb_sse41,            intBgrToRgb_sse41,
};

} // namespace zpng

#endif // x86
