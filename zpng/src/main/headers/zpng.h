// zpng.h — public C ABI for the zpng encoder.
//
// Compression levels follow zlib-ng's 0..9 convention.
// Two Java entry points (encode1, encode2) are exported by the .so.
#ifndef ZPNG_H
#define ZPNG_H

#include <stddef.h>

// Symbol export attribute. Must match the macro used in zpng.cc, otherwise
// MSVC reports "redefinition; different linkage" because the declaration
// (visible to the .cc via #include) and the definition disagree on whether
// the symbol is dllexport. GCC/Clang are lenient about this but MSVC is not.
#if defined(_MSC_VER) && !defined(__clang__)
  #define ZPNG_EXPORT __declspec(dllexport)
#else
  #define ZPNG_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

// CICP color-space identifiers for the optional cICP chunk.
#define PNG_CICP_NONE 0
#define PNG_CICP_PQ   9

struct PNGOptions {
  char predictor;        // carries the compression level 0..9 (struct ABI legacy)
  char huffman_sample;   // unused, kept for ABI compat
  char cicp_colorspace;
};

// Configures `options` for compression `level` (0..9, clamped) and `cicp`.
ZPNG_EXPORT void   pngFillOptions(struct PNGOptions *options, int level, int cicp);

// Maximum output buffer size required for an image of these dimensions.
ZPNG_EXPORT size_t pngOutputAllocSize(size_t bytes_per_channel, size_t num_channels,
                                      size_t width, size_t height);

// Low-level encoder: caller-allocated output buffer; takes pre-arranged
// RGBA/RGB pixel data. Returns the encoded byte count (or 0 on failure).
ZPNG_EXPORT size_t pngEncode(size_t bytes_per_channel, size_t num_channels,
                             const void *data, size_t width, size_t row_stride,
                             size_t height, void *output,
                             const struct PNGOptions *options);

#ifdef __cplusplus
}
#endif

#endif // ZPNG_H