// zpng_kernels_neon.cc — NEON kernels for ARM64.
//
// NEON (ASIMD) is part of the armv8-a baseline, so no special flags are
// needed on aarch64. Compiles to an empty object on non-ARM targets.

#include "zpng_kernels.h"

#if defined(__aarch64__) || defined(_M_ARM64) || defined(__ARM_NEON)

#include <arm_neon.h>
#include <stdint.h>
#include <string.h>

namespace zpng {
namespace {

void swapChannelsABGRtoRGBA_neon(unsigned char *pImage, int numPixels) {
  // vqtbl1q_u8 takes a table-index vector in natural (low..high) order.
  // out byte i = in byte mask[i]: reverse all 4 bytes of each pixel.
  static const uint8_t neon_mask[16] = {3, 2,  1,  0, 7,  6,  5,  4,
                                        11, 10, 9, 8, 15, 14, 13, 12};
  uint8x16_t idx = vld1q_u8(neon_mask);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    uint8x16_t v = vld1q_u8(pImage + i * 4);
    vst1q_u8(pImage + i * 4, vqtbl1q_u8(v, idx));
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

void swapChannelsBGRtoRGB_neon(unsigned char *pImage, int numPixels) {
  // BGR -> RGB: swap byte 0 and 2 within each 3-byte pixel.
  // 4 pixels = 12 bytes; load 16, shuffle, store 12 (8 + 4 split-store).
  static const uint8_t neon_mask[16] = {2, 1, 0, 5,  4,  3, 8, 7,
                                        6, 11, 10, 9, 0,  0, 0, 0};
  uint8x16_t idx = vld1q_u8(neon_mask);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    unsigned char *p = pImage + i * 3;
    uint8x16_t v = vld1q_u8(p);
    uint8x16_t s = vqtbl1q_u8(v, idx);
    vst1_u8(p, vget_low_u8(s));
    uint32_t hi = vgetq_lane_u32(vreinterpretq_u32_u8(s), 2);
    memcpy(p + 8, &hi, 4);
  }
  for (; i < numPixels; ++i) {
    unsigned char *p = pImage + i * 3;
    unsigned char t = p[0];
    p[0] = p[2];
    p[2] = t;
  }
}

void intArgbToRgba_neon(const unsigned char *src, unsigned char *dst,
                        int numPixels) {
  // Per pixel, in [B,G,R,A] -> out [R,G,B,A].
  static const uint8_t neon_mask[16] = {2,  1, 0, 3, 6,  5,  4,  7,
                                        10, 9, 8, 11, 14, 13, 12, 15};
  uint8x16_t idx = vld1q_u8(neon_mask);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    uint8x16_t v = vld1q_u8(src + i * 4);
    vst1q_u8(dst + i * 4, vqtbl1q_u8(v, idx));
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

void intRgbToRgba_neon(const unsigned char *src, unsigned char *dst,
                       int numPixels) {
  // Shuffle as for intArgbToRgba then OR alpha to 0xFF.
  static const uint8_t neon_mask[16] = {2,  1, 0, 3, 6,  5,  4,  7,
                                        10, 9, 8, 11, 14, 13, 12, 15};
  static const uint8_t neon_alpha[16] = {0, 0, 0, 0xFF, 0, 0, 0, 0xFF,
                                         0, 0, 0, 0xFF, 0, 0, 0, 0xFF};
  uint8x16_t idx = vld1q_u8(neon_mask);
  uint8x16_t af = vld1q_u8(neon_alpha);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    uint8x16_t v = vld1q_u8(src + i * 4);
    vst1q_u8(dst + i * 4, vorrq_u8(vqtbl1q_u8(v, idx), af));
  }
  for (; i < numPixels; ++i) {
    unsigned char b = src[i * 4 + 0], g = src[i * 4 + 1], r = src[i * 4 + 2];
    dst[i * 4 + 0] = r;
    dst[i * 4 + 1] = g;
    dst[i * 4 + 2] = b;
    dst[i * 4 + 3] = 0xFF;
  }
}

void intRgbToRgb_neon(const unsigned char *src, unsigned char *dst,
                      int numPixels) {
  // 4-byte src [B,G,R,_] -> 3-byte dst [R,G,B].
  static const uint8_t neon_mask[16] = {2, 1, 0, 6,  5,  4,  10, 9,
                                        8, 14, 13, 12, 0,  0,  0,  0};
  uint8x16_t idx = vld1q_u8(neon_mask);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    uint8x16_t v = vld1q_u8(src + i * 4);
    uint8x16_t s = vqtbl1q_u8(v, idx);
    vst1_u8(dst + i * 3, vget_low_u8(s));
    uint32_t hi = vgetq_lane_u32(vreinterpretq_u32_u8(s), 2);
    memcpy(dst + i * 3 + 8, &hi, 4);
  }
  for (; i < numPixels; ++i) {
    unsigned char b = src[i * 4 + 0], g = src[i * 4 + 1], r = src[i * 4 + 2];
    dst[i * 3 + 0] = r;
    dst[i * 3 + 1] = g;
    dst[i * 3 + 2] = b;
  }
}

void intBgrToRgb_neon(const unsigned char *src, unsigned char *dst,
                      int numPixels) {
  // 4-byte src [R,G,B,_] -> 3-byte dst [R,G,B] (just drop pad).
  static const uint8_t neon_mask[16] = {0, 1, 2, 4,  5,  6,  8, 9,
                                        10, 12, 13, 14, 0,  0, 0, 0};
  uint8x16_t idx = vld1q_u8(neon_mask);
  int i = 0;
  for (; i + 4 <= numPixels; i += 4) {
    uint8x16_t v = vld1q_u8(src + i * 4);
    uint8x16_t s = vqtbl1q_u8(v, idx);
    vst1_u8(dst + i * 3, vget_low_u8(s));
    uint32_t hi = vgetq_lane_u32(vreinterpretq_u32_u8(s), 2);
    memcpy(dst + i * 3 + 8, &hi, 4);
  }
  for (; i < numPixels; ++i) {
    unsigned char r = src[i * 4 + 0], g = src[i * 4 + 1], b = src[i * 4 + 2];
    dst[i * 3 + 0] = r;
    dst[i * 3 + 1] = g;
    dst[i * 3 + 2] = b;
  }
}

} // namespace

const Kernels kNeon = {
    swapChannelsABGRtoRGBA_neon, swapChannelsBGRtoRGB_neon,
    intArgbToRgba_neon,          intRgbToRgba_neon,
    intRgbToRgb_neon,            intBgrToRgb_neon,
};

} // namespace zpng

#endif // ARM
