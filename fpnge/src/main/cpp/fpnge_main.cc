// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <chrono>
#include <memory>
#include <thread>
#include <vector>

// For realpath() / PATH_MAX
#ifdef _WIN32
#  include <stdlib.h>   // _fullpath
#else
#  include <limits.h>
#  include <stdlib.h>
#endif

#include "fpnge.h"
#include "lodepng.h"

// ---------------------------------------------------------------------------
// Path traversal defence
// ---------------------------------------------------------------------------

// Returns a heap-allocated canonical absolute path, or nullptr on failure.
// The caller must free() the returned pointer.
// Rejects paths containing embedded NUL bytes (possible on some C runtimes).
static char *safe_realpath(const char *path) {
  if (!path || path[0] == '\0') {
    fprintf(stderr, "error: empty file path\n");
    return nullptr;
  }
  // Reject embedded NUL bytes: strlen stops at the first NUL, so a path
  // like "ok.png\0../../etc/shadow" would appear shorter than it is.
  // The argv string itself is NUL-terminated by the OS, so measuring via
  // strnlen with a generous bound catches any injection attempt.
  if (strnlen(path, PATH_MAX + 1) > PATH_MAX) {
    fprintf(stderr, "error: path too long\n");
    return nullptr;
  }
#ifdef _WIN32
  char *resolved = static_cast<char *>(malloc(PATH_MAX + 1));
  if (!resolved) return nullptr;
  if (!_fullpath(resolved, path, PATH_MAX + 1)) {
    free(resolved);
    fprintf(stderr, "error: cannot resolve path '%s'\n", path);
    return nullptr;
  }
#else
  // realpath() resolves all symlinks and ".." components.
  // For the output path the file may not yet exist; resolve its parent dir.
  char *resolved = realpath(path, nullptr);
  if (!resolved) {
    // File doesn't exist yet (common for the output). Resolve the parent
    // directory and append just the filename so we still catch ".." escapes.
    const char *slash = strrchr(path, '/');
    const char *filename = slash ? slash + 1 : path;
    char parent_buf[PATH_MAX + 1];
    if (slash) {
      size_t dir_len = static_cast<size_t>(slash - path);
      if (dir_len >= sizeof(parent_buf)) {
        fprintf(stderr, "error: path too long\n");
        return nullptr;
      }
      memcpy(parent_buf, path, dir_len);
      parent_buf[dir_len] = '\0';
    } else {
      // Relative filename only — use current directory as parent.
      if (!realpath(".", parent_buf)) {
        fprintf(stderr, "error: cannot resolve current directory\n");
        return nullptr;
      }
    }
    char *parent_real = realpath(parent_buf, nullptr);
    if (!parent_real) {
      fprintf(stderr, "error: cannot resolve directory for '%s': %s\n",
              path, strerror(errno));
      return nullptr;
    }
    // Reconstruct: parent_real + '/' + filename
    size_t total = strlen(parent_real) + 1 + strlen(filename) + 1;
    resolved = static_cast<char *>(malloc(total));
    if (!resolved) { free(parent_real); return nullptr; }
    snprintf(resolved, total, "%s/%s", parent_real, filename);
    free(parent_real);
    // Reject if the filename itself still contains a traversal component.
    if (strstr(filename, "..") != nullptr) {
      fprintf(stderr, "error: path traversal detected in '%s'\n", path);
      free(resolved);
      return nullptr;
    }
  }
#endif
  return resolved;
}

static int print_usage(const char *app) {
  fprintf(stderr, "Usage: %s [options] in.png out.png\n", app);
  fprintf(stderr, "  -1..%d  compression level (default %d)\n",
          FPNGE_COMPRESS_LEVEL_BEST, FPNGE_COMPRESS_LEVEL_DEFAULT);
  fprintf(stderr, "  -r<n>  run <n> repetitions and report\n");
  fprintf(stderr, "  -pq    reinterpret input pixels as PQ and add a cICP "
                  "chunk; can be used to make HDR PNGs\n");
  return 1;
}

int main(int argc, char **argv) {
  if (argc < 3) {
    return print_usage(argv[0]);
  }

  int comp_level = FPNGE_COMPRESS_LEVEL_DEFAULT;
  size_t num_reps = 0;
  int cicp_colorspace = FPNGE_CICP_NONE;

  int arg_p = 1;
  for (; arg_p < argc; arg_p++) {
    if (argv[arg_p][0] != '-')
      break;
    char opt = argv[arg_p][1];
    if (opt >= '1' && opt <= '0' + FPNGE_COMPRESS_LEVEL_BEST) {
      comp_level = opt - '0';
    } else if (opt == 'r') {
      // FIX (integer overflow): atoi() returns int; a negative value like
      // "-r-1" would silently wrap to SIZE_MAX when stored in size_t,
      // producing a near-infinite benchmark loop.  Use strtol() so we can
      // validate the range before the assignment.
      char *endptr = nullptr;
      long reps_l = strtol(argv[arg_p] + 2, &endptr, 10);
      if (*endptr != '\0' || reps_l <= 0) {
        fprintf(stderr, "error: -r requires a positive integer\n");
        return print_usage(argv[0]);
      }
      num_reps = static_cast<size_t>(reps_l);
    } else if (opt == 'p' && argv[arg_p][2] == 'q') {
      cicp_colorspace = FPNGE_CICP_PQ;
    } else {
      return print_usage(argv[0]);
    }
  }

  if (arg_p + 2 != argc) {
    return print_usage(argv[0]);
  }

  // FIX (path traversal): resolve both paths to their canonical absolute form
  // before use.  This collapses all ".." segments and symlinks so that a
  // caller supplying "../../etc/shadow" cannot reach outside the intended
  // working tree.  safe_realpath() also rejects embedded NUL bytes.
  char *in_resolved = safe_realpath(argv[arg_p]);
  if (!in_resolved) return 1;

  char *out_resolved = safe_realpath(argv[arg_p + 1]);
  if (!out_resolved) { free(in_resolved); return 1; }

  // RAII guards so both strings are freed on every return path, including
  // early returns introduced by the other fixes in this file.
  auto path_deleter = [](char *p) { free(p); };
  std::unique_ptr<char, decltype(path_deleter)> in_guard(in_resolved, path_deleter);
  std::unique_ptr<char, decltype(path_deleter)> out_guard(out_resolved, path_deleter);

  const char *in  = in_resolved;
  const char *out = out_resolved;
  struct FPNGEOptions options;
  FPNGEFillOptions(&options, comp_level, cicp_colorspace);

  FILE *infile = fopen(in, "rb");
  if (!infile) {
    fprintf(stderr, "error opening %s: %s\n", in, strerror(errno));
    return 1;
  }

  // FIX (integer overflow): fseek/ftell can fail and return -1.  Passing
  // -1L to the vector constructor casts to SIZE_MAX, triggering an enormous
  // allocation or std::bad_alloc.  Check both calls explicitly.
  if (fseek(infile, 0, SEEK_END) != 0) {
    fprintf(stderr, "error seeking in %s: %s\n", in, strerror(errno));
    fclose(infile);
    return 1;
  }
  long file_size = ftell(infile);
  if (file_size < 0) {
    fprintf(stderr, "error getting size of %s: %s\n", in, strerror(errno));
    fclose(infile);
    return 1;
  }
  if (fseek(infile, 0, SEEK_SET) != 0) {
    fprintf(stderr, "error seeking in %s: %s\n", in, strerror(errno));
    fclose(infile);
    return 1;
  }
  std::vector<unsigned char> in_data(static_cast<size_t>(file_size));
  if (fread(in_data.data(), 1, in_data.size(), infile) != in_data.size()) {
    fprintf(stderr, "error reading from %s: %s\n", in, strerror(errno));
    // FIX (resource leak): original used exit(1) leaving infile open.
    fclose(infile);
    return 1;
  }
  // FIX (resource leak): infile was never closed in the original code.
  fclose(infile);

  LodePNGState state;
  lodepng_state_init(&state);

  bool has_alpha = false;
  bool is_hbd = false;

  unsigned width, height;
  unsigned error =
      lodepng_inspect(&width, &height, &state, in_data.data(), in_data.size());
  if (error) {
    fprintf(stderr, "lodepng error %u: %s\n", error, lodepng_error_text(error));
    // FIX (resource leak): lodepng_state_cleanup was never called.
    lodepng_state_cleanup(&state);
    return 1;
  }

  if (state.info_png.color.colortype == LCT_RGBA ||
      state.info_png.color.colortype == LCT_GREY_ALPHA) {
    has_alpha = true;
  }

  if (state.info_png.color.bitdepth > 8) {
    is_hbd = true;
  }

  if (lodepng_chunk_find_const(in_data.data() + 8,
                               in_data.data() + in_data.size(), "tRNS")) {
    has_alpha = true;
  }

  // FIX (resource leak): state is no longer needed after inspect; clean it up
  // here so subsequent early returns don't need to remember to do so.
  lodepng_state_cleanup(&state);

  size_t num_c = has_alpha ? 4 : 3;

  unsigned char *png;

  // RGB(A) only for now.
  error = lodepng_decode_memory(
      &png, &width, &height, in_data.data(), in_data.size(),
      has_alpha ? LodePNGColorType::LCT_RGBA : LodePNGColorType::LCT_RGB,
      is_hbd ? 16 : 8);

  if (error) {
    fprintf(stderr, "lodepng error %u: %s\n", error, lodepng_error_text(error));
    // NOTE: lodepng_decode_memory frees its output on error, so png is
    // already NULL here — no free needed, but we return cleanly.
    return 1;
  }

  size_t encoded_size = 0;
  size_t bytes_per_channel = is_hbd ? 2 : 1;

  // FIX (integer overflow): compute the stride as size_t explicitly so the
  // multiplication never occurs in 32-bit unsigned arithmetic on platforms
  // where size_t is 32 bits.  On such platforms
  // (unsigned)width * num_c * bytes_per_channel would overflow for large images.
  size_t stride = static_cast<size_t>(width) * num_c * bytes_per_channel;

  void *encoded =
      malloc(FPNGEOutputAllocSize(bytes_per_channel, num_c, width, height));
  // FIX (resource leak): malloc return value was not checked.  A NULL encoded
  // pointer would be passed directly into FPNGEEncode, causing undefined
  // behaviour.  Free png before returning.
  if (!encoded) {
    fprintf(stderr, "out of memory allocating output buffer\n");
    free(png);
    return 1;
  }

  if (num_reps > 0) {
    auto start = std::chrono::high_resolution_clock::now();
    for (size_t _ = 0; _ < num_reps; _++) {
      encoded_size = FPNGEEncode(bytes_per_channel, num_c, png, width,
                                 stride, height, encoded, &options);
    }
    auto stop = std::chrono::high_resolution_clock::now();
    float us =
        std::chrono::duration_cast<std::chrono::microseconds>(stop - start)
            .count();
    size_t pixels = size_t{width} * size_t{height} * num_reps;
    float mps = pixels / us;
    fprintf(stderr, "%10.3f MP/s\n", mps);
    fprintf(stderr, "%10.3f bits/pixel\n",
            encoded_size * 8.0 / float(width) / float(height));
  } else {
    encoded_size = FPNGEEncode(bytes_per_channel, num_c, png, width,
                               stride, height, encoded, &options);
  }

  FILE *o = fopen(out, "wb");
  if (!o) {
    fprintf(stderr, "error opening %s: %s\n", out, strerror(errno));
    // FIX (resource leak): png and encoded were leaked here in the original.
    free(png);
    free(encoded);
    return 1;
  }
  if (fwrite(encoded, 1, encoded_size, o) != encoded_size) {
    fprintf(stderr, "error writing to %s: %s\n", out, strerror(errno));
    // FIX (resource leak + silent failure): original fell through without
    // returning an error code, so the caller saw success despite a partial
    // write.  Clean up and return failure.
    fclose(o);
    free(png);
    free(encoded);
    return 1;
  }
  fclose(o);
  free(png);
  free(encoded);

  return 0;
}