// zpng_kernels_avx2.cc — AVX2 kernels.
//
// Compile with: GCC/Clang -mavx2; MSVC /arch:AVX2. x86-only: compiles to
// an empty object elsewhere.
//
// This TU is the ONLY one that may contain AVX/VEX instructions. The
// 128-bit mid loops and scalar tails below get VEX-encoded by the
// compiler — that is fine here, because this table is only selected
// after a runtime CPUID + XGETBV check confirms AVX2 support.
//
// The original monolithic file declared the 128-bit `mask` inside an
// `#ifdef __SSE4_1__` block and reused it from the AVX2 loop; here each
// function simply declares both masks locally.

#include "zpng_kernels.h"

#if defined(__x86_64__) || defined(__i386__) || defined(_M_X64) || \
    defined(_M_IX86)

#include <immintrin.h>
#include <stdint.h>

namespace zpng {
namespace {

void swapChannelsABGRtoRGBA_avx2(unsigned char *pImage, int numPixels) {
  const __m256i mask256 = _mm256_set_epi8(
      12, 13, 14, 15, 8, 9, 10, 11, 4, 5, 6, 7, 0, 1, 2, 3,
      12, 13, 14, 15, 8, 9, 10, 11, 4, 5, 6, 7, 0, 1, 2, 3);
  const __m128i mask = _mm_set_epi8(12, 13, 14, 15, 8, 9, 10, 11,
                                    4, 5, 6, 7, 0, 1, 2, 3);
  int i = 0;
  for (; i + 8 <= numPixels; i += 8) {
    __m256i v = _mm256_loadu_si256((__m256i *)(pImage + i * 4));
    _mm256_storeu_si256((__m256i *)(pImage + i * 4),
                        _mm256_shuffle_epi8(v, mask256));
  }
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

void swapChannelsBGRtoRGB_avx2(unsigned char *pImage, int numPixels) {
  const __m128i mask =
      _mm_set_epi8(-1, -1, -1, -1, 9, 10, 11, 6, 7, 8, 3, 4, 5, 0, 1, 2);
  int i = 0;
  // 8 pixels = 24 bytes per iteration, via two 128-bit shuffles.
  for (; i + 8 <= numPixels; i += 8) {
    unsigned char *p = pImage + i * 3;
    __m128i lo = _mm_loadu_si128((__m128i *)p);
    __m128i ls = _mm_shuffle_epi8(lo, mask);
    _mm_storel_epi64((__m128i *)p, ls);
    *(uint32_t *)(p + 8) = (uint32_t)_mm_extract_epi32(ls, 2);
    __m128i hi = _mm_loadu_si128((__m128i *)(p + 12));
    __m128i hs = _mm_shuffle_epi8(hi, mask);
    _mm_storel_epi64((__m128i *)(p + 12), hs);
    *(uint32_t *)(p + 20) = (uint32_t)_mm_extract_epi32(hs, 2);
  }
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

void intArgbToRgba_avx2(const unsigned char *src, unsigned char *dst,
                        int numPixels) {
  const __m128i mask = _mm_set_epi8(15, 12, 13, 14, 11, 8, 9, 10,
                                    7, 4, 5, 6, 3, 0, 1, 2);
  const __m256i mask256 = _mm256_broadcastsi128_si256(mask);
  int i = 0;
  for (; i + 8 <= numPixels; i += 8) {
    __m256i v = _mm256_loadu_si256((const __m256i *)(src + i * 4));
    _mm256_storeu_si256((__m256i *)(dst + i * 4),
                        _mm256_shuffle_epi8(v, mask256));
  }
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

void intRgbToRgba_avx2(const unsigned char *src, unsigned char *dst,
                       int numPixels) {
  const __m128i mask = _mm_set_epi8(15, 12, 13, 14, 11, 8, 9, 10,
                                    7, 4, 5, 6, 3, 0, 1, 2);
  const __m128i alpha = _mm_set1_epi32((int)0xFF000000);
  const __m256i mask256 = _mm256_broadcastsi128_si256(mask);
  const __m256i alpha256 = _mm256_set1_epi32((int)0xFF000000);
  int i = 0;
  for (; i + 8 <= numPixels; i += 8) {
    __m256i v = _mm256_loadu_si256((const __m256i *)(src + i * 4));
    v = _mm256_or_si256(_mm256_shuffle_epi8(v, mask256), alpha256);
    _mm256_storeu_si256((__m256i *)(dst + i * 4), v);
  }
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

void intRgbToRgb_avx2(const unsigned char *src, unsigned char *dst,
                      int numPixels) {
  const __m128i mask =
      _mm_set_epi8((char)0x80, (char)0x80, (char)0x80, (char)0x80,
                   12, 13, 14, 8, 9, 10, 4, 5, 6, 0, 1, 2);
  int i = 0;
  for (; i + 8 <= numPixels; i += 8) {
    const unsigned char *sp = src + i * 4;
    unsigned char *dp = dst + i * 3;
    __m128i lo = _mm_loadu_si128((const __m128i *)sp);
    __m128i ls = _mm_shuffle_epi8(lo, mask);
    _mm_storel_epi64((__m128i *)dp, ls);
    *(uint32_t *)(dp + 8) = (uint32_t)_mm_extract_epi32(ls, 2);
    __m128i hi = _mm_loadu_si128((const __m128i *)(sp + 16));
    __m128i hs = _mm_shuffle_epi8(hi, mask);
    _mm_storel_epi64((__m128i *)(dp + 12), hs);
    *(uint32_t *)(dp + 20) = (uint32_t)_mm_extract_epi32(hs, 2);
  }
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

void intBgrToRgb_avx2(const unsigned char *src, unsigned char *dst,
                      int numPixels) {
  const __m128i mask =
      _mm_set_epi8((char)0x80, (char)0x80, (char)0x80, (char)0x80,
                   14, 13, 12, 10, 9, 8, 6, 5, 4, 2, 1, 0);
  int i = 0;
  for (; i + 8 <= numPixels; i += 8) {
    const unsigned char *sp = src + i * 4;
    unsigned char *dp = dst + i * 3;
    __m128i lo = _mm_loadu_si128((const __m128i *)sp);
    __m128i ls = _mm_shuffle_epi8(lo, mask);
    _mm_storel_epi64((__m128i *)dp, ls);
    *(uint32_t *)(dp + 8) = (uint32_t)_mm_extract_epi32(ls, 2);
    __m128i hi = _mm_loadu_si128((const __m128i *)(sp + 16));
    __m128i hs = _mm_shuffle_epi8(hi, mask);
    _mm_storel_epi64((__m128i *)(dp + 12), hs);
    *(uint32_t *)(dp + 20) = (uint32_t)_mm_extract_epi32(hs, 2);
  }
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

const Kernels kAvx2 = {
    swapChannelsABGRtoRGBA_avx2, swapChannelsBGRtoRGB_avx2,
    intArgbToRgba_avx2,          intRgbToRgba_avx2,
    intRgbToRgb_avx2,            intBgrToRgb_avx2,
};

} // namespace zpng

#endif // x86
