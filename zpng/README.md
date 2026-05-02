# zpng — drop-in zlib-ng replacement for FPNGE/FPNG, tuned for screen content

A new PNG encoder source file (`zpng.cc`) that replaces both `fpnge.cc` and
`fpng.cpp` in your project. Same C ABI — your Java FFM bindings continue to
work without changes.

## What's in this drop

| File | Purpose |
|---|---|
| `zpng.cc` | The encoder. Single source file, compiles for both AVX2 and SSE4.1 targets. |
| `fpnge.h` | Public header. Same surface as the existing one in your project. |
| `build.gradle.kts` | Gradle native-build snippet — produces `libzpng_avx2.so` and `libzpng_sse.so`. |

The Java side does **not** need to change. `FPNGEEncode1`, `FPNGEEncode2`,
`FPNGEFillOptions`, `FPNGEOutputAllocSize`, plus all the channel-swap and
pixel-conversion helpers (`swapChannelsABGRtoRGBA`, `intArgbToRgba`, etc.) are
exported with identical signatures.

To switch your Java code to the new encoder, change only the library load:

```java
// Before:
System.loadLibrary("fpnge_avx2");

// After:
System.loadLibrary("zpng_avx2");
```

## Build prerequisites

```sh
# Arch:
pacman -S zlib-ng

# Verify:
ls /usr/include/zlib-ng.h    # header
ls /usr/lib/libz-ng.so       # library
```

`zpng.cc` uses zlib-ng's namespaced API (`zng_*`) so it cannot collide with
the libz the JVM links at startup. **Do not** link against the zlib-ng compat
shim (`-DZLIB_COMPAT=ON`) — symbol clashes with the JVM's libz are possible
in that mode.

## Build commands

Direct g++ if you want to verify outside Gradle:

```sh
# AVX2:
g++ -O3 -mavx2 -mpclmul -DNDEBUG -fPIC -shared -fvisibility=hidden \
    -std=c++17 zpng.cc -o libzpng_avx2.so -lz-ng

# SSE4.1:
g++ -O3 -msse4.1 -mpclmul -DNDEBUG -fPIC -shared -fvisibility=hidden \
    -std=c++17 zpng.cc -o libzpng_sse.so -lz-ng
```

Or via Gradle: drop `build.gradle.kts` (or merge its contents into your
existing build), then:

```sh
./gradlew buildZpngAll
# or, if you want them packaged into resources for jar shipping:
./gradlew packageZpng
```

## Compression results on `text_test.png` (992 × 2805 RGB)

| Encoder | Size | Ratio vs ObjectPlanet |
|---|---|---|
| FPNGE A0 patched, level 5 | 540 KB | 3.9× larger |
| FPNG (Up filter), level 5 | 394 KB | 2.8× larger |
| FPNG (None filter), level 5 | 347 KB | 2.5× larger |
| **zpng level 5** | **128 KB** | **0.92× — smaller** |
| ObjectPlanet | 139 KB | (baseline) |
| ImageIO | 156 KB | 1.12× larger |
| libpng (PIL adaptive) | 181 KB | 1.30× larger |

## Throughput on AVX2, single thread

Measured on `text_test.png` via Python ctypes (small wrapper overhead included):

| Level | Size | Time | Throughput |
|---|---|---|---|
| 1 | 339 KB | 14 ms | 200 MP/s |
| 2 | 168 KB | 22 ms | 125 MP/s |
| 3 | 144 KB | 30 ms | 92 MP/s |
| 4 | 136 KB | ~80 ms | ~35 MP/s |
| 5 | 128 KB | 168 ms | 17 MP/s |

**Recommended default: level 3.** It hits 92 MP/s at 144 KB, which is
comparable size to ObjectPlanet but several times faster. Level 5 only buys
12% more compression for 5× the encode time — usually not worth it.

The level mapping `1..5 → zlib-ng 1, 3, 5, 7, 9` is in `map_level()`. Adjust
if you want different points on the size/speed curve.

## Why filter NONE (and not Up)?

This file targets screen content. On your `text_test.png`:

| filter | strategy | size at level 9 |
|---|---|---|
| **None** | **DEFAULT** | **128 KB** |
| None | FILTERED | 128 KB |
| None | RLE | 272 KB |
| Up | DEFAULT | 187 KB |
| Up | FILTERED | 191 KB |
| Heuristic (None\|Up) | DEFAULT | 174 KB |

The Up filter is the standard PNG choice for natural images: it reduces
residual magnitude so most filtered bytes cluster near 0. But it does this by
making each row's encoding *depend on the row above*, which destroys the
verbatim-glyph repetition that LZ77 needs to compress text. zlib-ng's hash
chain finds "the" appearing 50 times on a page when filter is None;
under Up, the same word turns into 50 different residual sequences.

The heuristic that picks per-row between None and Up makes things *worse*,
not better — it picks Up for rows where Up locally minimises residual
magnitude (early character rows), and that's exactly where the row-above
dependency is strongest, breaking the most LZ77 matches.

For natural images you will want to flip both knobs. Search for `FILTER_NONE`
in `zpng.cc` to find the two lines to change. A reasonable extension would
be a runtime flag in `FPNGEOptions`.

## Trade-off documented honestly

`zpng` is **30% worse than libpng** on natural photographic images
(synthetic noisy gradient: zpng 2.3 MB vs libpng 1.8 MB). This is the price
of being tuned for screen content. If your workload changes, either:

1. Re-enable Up filtering (line marked `FILTER_NONE` in `zpng.cc`), or
2. Keep both encoders and dispatch based on a quick classifier — sample a
   few rows, count distinct byte values in the histogram, route low-entropy
   inputs to the current zpng and high-entropy inputs to an Up-filter
   version.

Today the workload is screen content, so option (1) and (2) are deferred.

## What was removed compared to FPNGE/FPNG

These exported symbols are gone because zpng uses zlib-ng's match-finder and
Huffman code generation:

- The FPNGE Huffman table machinery (`HuffmanTable`, `WriteHuffmanCode`,
  symbol-counting passes, predictor selection)
- The FPNG `pixel_deflate_dyn_3_rle` family
- The `Crc32` PCLMUL class — `zng_crc32` does the same thing internally with
  runtime CPU dispatch

These are kept (verbatim from the existing fpnge.cc):

- `swapChannelsABGRtoRGBA`, `swapChannelsBGRtoRGB`
- `intArgbToRgba`, `intRgbToRgba`, `intRgbToRgb`, `intBgrToRgb`

So your Java conversion paths (`BufferedImage.TYPE_4BYTE_ABGR`, `TYPE_INT_ARGB`,
etc.) work without modification.

## What I'd ask you to verify before shipping

1. **Run your JMH suite against `libzpng_*.so`** — confirm the level/size
   numbers I'm seeing match yours, and that no level falls into a degenerate
   path (the `8351205` level-2/3 results you saw with FPNG should not appear
   here; level 1 is "fastest deflate", levels 2–5 all use proper LZ77).

2. **Round-trip a few real pages with lodepng** — I tested with PIL but your
   actual decode path is whatever your customers use. The PNG framing in
   `write_png_header` and `write_png_iend` is straightforward but worth one
   sanity round-trip.

3. **Confirm rpath / library resolution on your deployment targets.** The
   build script uses `-Wl,-rpath,$ORIGIN` so `libzpng_*.so` will look for
   `libz-ng.so` in the same directory. If you're packaging the .so in a jar
   and extracting at runtime, also extract `libz-ng.so` next to it; or, if
   your customers' systems are Arch with system zlib-ng, leave the rpath off
   and let the linker resolve from `/usr/lib`.

4. **JVM symbol-resolution sanity.** With the `zng_*` namespace there should
   be no clash with the JVM's libz. To double-check on your deployment box:

   ```sh
   ldd libzpng_avx2.so | grep -i z
   # should show libz-ng.so.2, NOT libz.so.1
   ```

   If you see `libz.so.1`, something pulled in the compat shim and that's a
   bug — clean rebuild with `-DZLIB_COMPAT=OFF` if you're building zlib-ng
   from source.
