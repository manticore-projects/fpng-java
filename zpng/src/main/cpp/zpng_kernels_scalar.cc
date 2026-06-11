// zpng_kernels_scalar.cc — portable scalar kernels.
//
// Compile with BASELINE flags only (no -mavx2 / -msse4.1). This table is
// the fallback for CPUs without SSE4.1 (VirtualBox guests with masked
// CPUID, ancient x86, exotic targets) and the correctness reference for
// the SIMD variants.

#include "zpng_kernels.h"

namespace zpng {
namespace {

void swapChannelsABGRtoRGBA_scalar(unsigned char *pImage, int numPixels) {
  for (int i = 0; i < numPixels; ++i) {
    unsigned char a = pImage[i * 4 + 0], b = pImage[i * 4 + 1];
    unsigned char g = pImage[i * 4 + 2], r = pImage[i * 4 + 3];
    pImage[i * 4 + 0] = r;
    pImage[i * 4 + 1] = g;
    pImage[i * 4 + 2] = b;
    pImage[i * 4 + 3] = a;
  }
}

void swapChannelsBGRtoRGB_scalar(unsigned char *pImage, int numPixels) {
  for (int i = 0; i < numPixels; ++i) {
    unsigned char *p = pImage + i * 3;
    unsigned char t = p[0];
    p[0] = p[2];
    p[2] = t;
  }
}

void intArgbToRgba_scalar(const unsigned char *src, unsigned char *dst,
                          int numPixels) {
  for (int i = 0; i < numPixels; ++i) {
    unsigned char b = src[i * 4 + 0], g = src[i * 4 + 1];
    unsigned char r = src[i * 4 + 2], a = src[i * 4 + 3];
    dst[i * 4 + 0] = r;
    dst[i * 4 + 1] = g;
    dst[i * 4 + 2] = b;
    dst[i * 4 + 3] = a;
  }
}

void intRgbToRgba_scalar(const unsigned char *src, unsigned char *dst,
                         int numPixels) {
  for (int i = 0; i < numPixels; ++i) {
    unsigned char b = src[i * 4 + 0], g = src[i * 4 + 1], r = src[i * 4 + 2];
    dst[i * 4 + 0] = r;
    dst[i * 4 + 1] = g;
    dst[i * 4 + 2] = b;
    dst[i * 4 + 3] = 0xFF;
  }
}

void intRgbToRgb_scalar(const unsigned char *src, unsigned char *dst,
                        int numPixels) {
  for (int i = 0; i < numPixels; ++i) {
    unsigned char b = src[i * 4 + 0], g = src[i * 4 + 1], r = src[i * 4 + 2];
    dst[i * 3 + 0] = r;
    dst[i * 3 + 1] = g;
    dst[i * 3 + 2] = b;
  }
}

void intBgrToRgb_scalar(const unsigned char *src, unsigned char *dst,
                        int numPixels) {
  for (int i = 0; i < numPixels; ++i) {
    unsigned char r = src[i * 4 + 0], g = src[i * 4 + 1], b = src[i * 4 + 2];
    dst[i * 3 + 0] = r;
    dst[i * 3 + 1] = g;
    dst[i * 3 + 2] = b;
  }
}

} // namespace

const Kernels kScalar = {
    swapChannelsABGRtoRGBA_scalar, swapChannelsBGRtoRGB_scalar,
    intArgbToRgba_scalar,          intRgbToRgba_scalar,
    intRgbToRgb_scalar,            intBgrToRgb_scalar,
};

} // namespace zpng
