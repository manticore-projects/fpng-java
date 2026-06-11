// zpng.cc — PNG encoder backed by zlib-ng. Tuned for screen content
// (text, UI mockups, rasterised documents).
//
// Strategy:
//   - Filter: NONE on every row.
//   - Compression: zlib-ng deflate at the supplied 0..9 level,
//     strategy Z_DEFAULT_STRATEGY.
//   - SIMD: the channel-swap helpers are dispatched at RUNTIME via
//     CPUID (see zpng_dispatch.cc / zpng_kernels_*.cc). One binary runs
//     correctly on any x86-64 (AVX2 -> SSE4.1 -> scalar) and on ARM64
//     (NEON). Deflate itself is dispatched by zlib-ng at runtime.
//
//     THIS TU MUST BE COMPILED WITH BASELINE FLAGS ONLY. Compiling it
//     with -mavx2 lets the compiler auto-vectorize ordinary loops with
//     VEX/ymm instructions, reintroducing SIGILL on non-AVX CPUs (e.g.
//     VirtualBox guests with AVX masked out of CPUID). Per-TU flags:
//     see the table in zpng_kernels.h.
//
// Why NONE / DEFAULT?  Screen content has bimodal byte distributions
// (heavy mass at 0x00 and 0xFF), small set of distinct pixel values,
// and high cross-row redundancy (the same glyph repeats many times
// down a page). Empirically on a 992x2805 RGB text page:
//
//      filter   strategy   level   size
//      None     DEFAULT     9      127.9 KB   <-- this configuration
//      Up       FILTERED    9      190.8 KB   (the natural-image pick)
//      Up       DEFAULT     9      187.0 KB
//      None     FILTERED    9      128.2 KB
//      None     RLE         9      271.5 KB
//
// Up filter helps natural images by reducing residual magnitude, but on
// screen content it destroys the verbatim-glyph repetition that LZ77
// needs. If you ever need this encoder to handle photographs, expose a
// runtime switch — search for FILTER_NONE in this file.
//
// Build is driven by the project's build.gradle. The plugin compiles the
// per-ISA kernel TUs with their specific SIMD flags (zpng_kernels.h has
// the full table) and statically links zlib-ng.

#include "zpng.h"
#include "zpng_kernels.h"

#include <zlib-ng.h>

#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <vector>

// glibc 2.38 strtol redirect compat.
//
// On x86_64 glibc 2.38+ targets, GCC 14+ macros redirect strtol to
// __isoc23_strtol, creating a hard dependency on GLIBC_2.38 which breaks
// older deployment targets. We provide our own __isoc23_strtol that
// forwards to strtol@GLIBC_2.2.5 via a versioned symbol alias.
//
// The .symver pragma uses x86 GLIBC versioning conventions; ARM64 has its
// own version baseline (2.17+) and does not need this workaround. macOS
// uses libSystem, not glibc, so the whole block is skipped there.
#if defined(__linux__) && defined(__GLIBC__) && __GLIBC__ >= 2 \
    && (defined(__x86_64__) || defined(__i386__))
extern "C" {
  __asm__(".symver __compat_strtol, strtol@GLIBC_2.2.5");
  extern long int __compat_strtol(const char *, char **, int);
  long int __isoc23_strtol(const char *p, char **e, int b) {
    return __compat_strtol(p, e, b);
  }
}
#endif

// EXPORT is now defined in zpng.h as ZPNG_EXPORT. Keep a local alias so we
// don't have to rename every callsite, and so the implementation matches
// the declaration's linkage attribute (matters for MSVC).
#define EXPORT ZPNG_EXPORT

// Same struct your existing FFM code already understands.
struct CharArray {
  unsigned char *data;
  size_t size;
};

namespace {

// -----------------------------------------------------------------------------
// PNG primitives
// -----------------------------------------------------------------------------

static inline void put_be32(unsigned char *p, uint32_t v) {
  p[0] = (v >> 24) & 0xff;
  p[1] = (v >> 16) & 0xff;
  p[2] = (v >>  8) & 0xff;
  p[3] =  v        & 0xff;
}

// CRC32 (IEEE/PNG polynomial). zlib-ng exposes zng_crc32 which dispatches to
// PCLMUL/CRC32C-instruction implementations at runtime — no need to maintain
// a separate CRC implementation here.
static inline uint32_t png_crc32(const unsigned char *data, size_t len) {
  return (uint32_t)zng_crc32(0, data, (uint32_t)len);
}

static const unsigned char kPngSignature[8] = {
  137, 80, 78, 71, 13, 10, 26, 10
};

// Writes IHDR (and optionally cICP). Returns number of bytes written.
static size_t write_png_header(unsigned char *out,
                               uint32_t width, uint32_t height,
                               size_t bytes_per_channel, size_t num_channels,
                               char cicp_colorspace) {
  unsigned char *p = out;
  memcpy(p, kPngSignature, 8); p += 8;

  // IHDR: length=13, type=IHDR, width, height, bitdepth, colortype, compression=0, filter=0, interlace=0
  put_be32(p, 13); p += 4;
  unsigned char *ihdr_start = p;
  memcpy(p, "IHDR", 4); p += 4;
  put_be32(p, width);  p += 4;
  put_be32(p, height); p += 4;
  *p++ = (unsigned char)(bytes_per_channel * 8);
  static const unsigned char numc_to_colortype[5] = {0, 0, 4, 2, 6};
  *p++ = numc_to_colortype[num_channels];
  *p++ = 0; // compression
  *p++ = 0; // filter
  *p++ = 0; // interlace
  uint32_t crc = png_crc32(ihdr_start, (size_t)(p - ihdr_start));
  put_be32(p, crc); p += 4;

  if (cicp_colorspace == PNG_CICP_PQ) {
    // 4 byte payload: 9 (BT.2020 primaries), 16 (PQ TF), 0 (full range), 1 (matrix=identity for RGB).
    put_be32(p, 4); p += 4;
    unsigned char *cicp_start = p;
    memcpy(p, "cICP", 4); p += 4;
    *p++ = 9;   // primaries
    *p++ = 16;  // transfer (PQ)
    *p++ = 0;   // matrix coeffs (0 = RGB)
    *p++ = 1;   // full range
    crc = png_crc32(cicp_start, (size_t)(p - cicp_start));
    put_be32(p, crc); p += 4;
  }

  return (size_t)(p - out);
}

// IEND: 0-length data, type "IEND", precomputed CRC = 0xae426082.
static size_t write_png_iend(unsigned char *out) {
  put_be32(out, 0);
  memcpy(out + 4, "IEND", 4);
  put_be32(out + 8, 0xae426082);
  return 12;
}

// -----------------------------------------------------------------------------
// Filter pass: NONE on every row.
//
// PNG filter NONE means: 1 filter byte (=0) followed by raw row bytes verbatim.
// We assemble (filter byte | row) into a contiguous buffer to feed deflate.
// This matters for zlib-ng more than it might appear: a single deflate input
// stream lets the hash chain match across row boundaries. Calling deflate
// per-row would prevent cross-row matches.
//
// FILTER_NONE: change the loop body in encode_png_with_level() if you need
// Up filter for natural images. The framing here is filter-agnostic.
// -----------------------------------------------------------------------------

static inline void filter_row_none(const unsigned char *src,
                                   unsigned char *dst,
                                   size_t bytes_per_line) {
  dst[0] = 0; // filter byte: None
  memcpy(dst + 1, src, bytes_per_line);
}

// -----------------------------------------------------------------------------
// Compression-level handling
//
// User passes 0..9 directly (zlib-ng convention).
//   0 = stored (no compression, just framing)
//   1 = fastest deflate (deflate_quick / deflate_fast)
//   ...
//   9 = best compression (deflate_slow with longest hash chains)
// We pass the value straight through after clamping. The legacy 1..5 callers
// can keep using pngFillOptions, which clamps to 0..9.
// -----------------------------------------------------------------------------

static int clamp_level(int comp_level) {
  if (comp_level < 0) return 0;
  if (comp_level > 9) return 9;
  return comp_level;
}

// -----------------------------------------------------------------------------
// Core encoder: takes raw RGB/RGBA pixels, returns full PNG byte stream length.
// comp_level is the zlib-ng deflate level 0..9 (passed through, clamped).
// -----------------------------------------------------------------------------

static size_t encode_png_with_level(size_t bytes_per_channel,
                                    size_t num_channels,
                                    const unsigned char *image,
                                    size_t width,
                                    size_t row_stride,
                                    size_t height,
                                    unsigned char *output,
                                    size_t output_capacity,
                                    int comp_level,
                                    char cicp_colorspace) {
  assert(bytes_per_channel == 1 || bytes_per_channel == 2);
  assert(num_channels >= 1 && num_channels <= 4);

  const size_t bytes_per_pixel = bytes_per_channel * num_channels;
  const size_t bytes_per_line  = bytes_per_pixel * width;
  const size_t filtered_row_size = 1 + bytes_per_line;
  const size_t filtered_total    = filtered_row_size * height;

  // PNG header.
  size_t out_ofs = write_png_header(output, (uint32_t)width, (uint32_t)height,
                                    bytes_per_channel, num_channels,
                                    cicp_colorspace);

  // Filter all rows into a contiguous buffer.
  // We use filter NONE for every row — see the file header comment for why.
  std::vector<unsigned char> filtered(filtered_total);
  for (size_t y = 0; y < height; y++) {
    const unsigned char *src = image + y * row_stride;
    unsigned char *dst = filtered.data() + y * filtered_row_size;
    filter_row_none(src, dst, bytes_per_line);
  }

  // IDAT framing.
  if (out_ofs + 12 + 12 > output_capacity) return 0;
  size_t idat_length_pos = out_ofs; out_ofs += 4;
  size_t idat_type_pos   = out_ofs; memcpy(output + out_ofs, "IDAT", 4); out_ofs += 4;
  size_t idat_data_pos   = out_ofs;
  size_t idat_data_avail = output_capacity - idat_data_pos - 12;
  if (idat_data_avail > UINT32_MAX) idat_data_avail = UINT32_MAX;

  // zlib-ng deflate. Strategy DEFAULT — see file header comment for why we
  // explicitly do NOT use Z_FILTERED here.
  zng_stream strm;
  memset(&strm, 0, sizeof(strm));
  int level = clamp_level(comp_level);
  if (zng_deflateInit2(&strm, level, Z_DEFLATED, 15, 8, Z_DEFAULT_STRATEGY) != Z_OK) {
    return 0;
  }
  strm.next_in   = filtered.data();
  strm.avail_in  = (uint32_t)filtered_total;
  strm.next_out  = output + idat_data_pos;
  strm.avail_out = (uint32_t)idat_data_avail;
  int rc = zng_deflate(&strm, Z_FINISH);
  size_t idat_data_len = (size_t)strm.total_out;
  zng_deflateEnd(&strm);
  if (rc != Z_STREAM_END) return 0;

  out_ofs += idat_data_len;

  // Patch IDAT length, write its CRC.
  put_be32(output + idat_length_pos, (uint32_t)idat_data_len);
  uint32_t idat_crc = png_crc32(output + idat_type_pos,
                                4 + idat_data_len);
  put_be32(output + out_ofs, idat_crc);
  out_ofs += 4;

  // IEND.
  out_ofs += write_png_iend(output + out_ofs);
  return out_ofs;
}

} // namespace

// =============================================================================
// Public C ABI. Java FFM does not need to know any of the internals.
// =============================================================================

extern "C" {

EXPORT void pngFillOptions(PNGOptions *options, int level, int cicp) {
  // Compression level: 0..9 (zlib-ng range). 0 = stored, 9 = max compression.
  // We piggy-back the level into the `predictor` field of the PNGOptions
  // struct so the struct ABI stays unchanged. predictor's signed-char range
  // (-128..127) easily holds 0..9.
  level = clamp_level(level);
  options->predictor       = (char)level;
  options->huffman_sample   = 23;
  options->cicp_colorspace = (char)cicp;
}

EXPORT size_t pngOutputAllocSize(size_t bytes_per_channel,
                                   size_t num_channels,
                                   size_t width, size_t height) {
  // PNG signature(8) + IHDR(25) + cICP(16, optional) + IDAT framing(12) + IEND(12)
  // + worst-case zlib stream over (1+W*N)*H input.
  size_t bpl = bytes_per_channel * num_channels * width;
  size_t filtered = (1 + bpl) * height;
  // zlib stored-block worst case: 5 bytes header per 65535 bytes + zlib wrapper(6).
  size_t max_zlib = filtered + (filtered / 65535 + 1) * 5 + 6;
  return 8 + 25 + 16 + 12 + max_zlib + 12 + 64; // 64 byte slack
}

EXPORT size_t pngEncode(size_t bytes_per_channel, size_t num_channels,
                          const void *data, size_t width, size_t row_stride,
                          size_t height, void *output,
                          const PNGOptions *options) {
  int level = 6;  // sensible mid-range default if no options supplied
  char cicp = PNG_CICP_NONE;
  if (options) {
    // pngFillOptions stored level in predictor field (see above).
    int p = options->predictor;
    level = clamp_level(p);
    cicp = options->cicp_colorspace;
  }
  size_t cap = pngOutputAllocSize(bytes_per_channel, num_channels, width, height);
  return encode_png_with_level(bytes_per_channel, num_channels,
                               (const unsigned char *)data,
                               width, row_stride, height,
                               (unsigned char *)output, cap,
                               level, cicp);
}

// -----------------------------------------------------------------------------
// Channel-swap and pixel-format helpers exposed to Java FFM.
//
// The exported names and signatures are unchanged; the bodies now forward
// through the runtime-selected kernel table (zpng_dispatch.cc). The magic
// static behind select_kernels() costs one acquire load per call —
// negligible against the per-pixel work, and it keeps initialization
// thread-safe and lazy regardless of how the .so is loaded (dlopen, Java
// FFM SymbolLookup, static linking into a test harness).
// -----------------------------------------------------------------------------

EXPORT void swapChannelsABGRtoRGBA(unsigned char *pImage, int numPixels) {
  zpng::select_kernels().swapChannelsABGRtoRGBA(pImage, numPixels);
}

EXPORT void swapChannelsBGRtoRGB(unsigned char *pImage, int numPixels) {
  zpng::select_kernels().swapChannelsBGRtoRGB(pImage, numPixels);
}

EXPORT void intArgbToRgba(const unsigned char *src,
                          unsigned char *dst, int numPixels) {
  zpng::select_kernels().intArgbToRgba(src, dst, numPixels);
}

EXPORT void intRgbToRgba(const unsigned char *src,
                         unsigned char *dst, int numPixels) {
  zpng::select_kernels().intRgbToRgba(src, dst, numPixels);
}

EXPORT void intRgbToRgb(const unsigned char *src,
                        unsigned char *dst, int numPixels) {
  zpng::select_kernels().intRgbToRgb(src, dst, numPixels);
}

EXPORT void intBgrToRgb(const unsigned char *src,
                        unsigned char *dst, int numPixels) {
  zpng::select_kernels().intBgrToRgb(src, dst, numPixels);
}

// -----------------------------------------------------------------------------
// Java entry points.
//
// encode1: caller passes a buffer in BufferedImage byte order
//   (TYPE_4BYTE_ABGR for 4ch, TYPE_3BYTE_BGR for 3ch). We swap in place,
//   encode, then swap back to leave the caller's buffer untouched.
//
// encode2: caller passes a buffer already in RGBA/RGB order
//   (e.g. produced by intArgbToRgba). No swap needed.
// -----------------------------------------------------------------------------

EXPORT CharArray *encode1(size_t bytes_per_channel, size_t num_channels,
                          unsigned char *pImage,
                          size_t width, size_t height, int comp_level) {
  PNGOptions options;
  pngFillOptions(&options, comp_level, PNG_CICP_NONE);

  size_t row_stride = width * num_channels * bytes_per_channel;
  size_t numPixels  = width * height;

  if (num_channels == 4) swapChannelsABGRtoRGBA(pImage, (int)numPixels);
  else if (num_channels == 3) swapChannelsBGRtoRGB(pImage, (int)numPixels);

  CharArray *out = (CharArray *)malloc(sizeof(CharArray));
  if (!out) goto restore;
  out->size = pngOutputAllocSize(bytes_per_channel, num_channels, width, height);
  out->data = (unsigned char *)malloc(out->size);
  if (!out->data) { free(out); out = nullptr; goto restore; }

  out->size = pngEncode(bytes_per_channel, num_channels, pImage, width,
                          row_stride, height, out->data, &options);

restore:
  if (num_channels == 4) swapChannelsABGRtoRGBA(pImage, (int)numPixels);
  else if (num_channels == 3) swapChannelsBGRtoRGB(pImage, (int)numPixels);
  return out;
}

EXPORT CharArray *encode2(size_t bytes_per_channel, size_t num_channels,
                          unsigned char *pImage,
                          size_t width, size_t height, int comp_level) {
  PNGOptions options;
  pngFillOptions(&options, comp_level, PNG_CICP_NONE);

  size_t row_stride = width * num_channels * bytes_per_channel;

  CharArray *out = (CharArray *)malloc(sizeof(CharArray));
  if (!out) return nullptr;
  out->size = pngOutputAllocSize(bytes_per_channel, num_channels, width, height);
  out->data = (unsigned char *)malloc(out->size);
  if (!out->data) { free(out); return nullptr; }

  out->size = pngEncode(bytes_per_channel, num_channels, pImage, width,
                          row_stride, height, out->data, &options);
  return out;
}

} // extern "C"
