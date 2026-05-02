# [fpng-java](https://manticore-projects.com/FPNG-Java/index.html#) [![Gradle Package](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle-publish.yml/badge.svg)](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle-publish.yml)  [![Maven Central](https://img.shields.io/maven-central/v/com.manticore-projects.tools/fpng-java)](https://central.sonatype.com/artifact/com.manticore-projects.tools/fpng-java) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](http://makeapullrequest.com)

Java Wrappers for three native, SIMD-optimised PNG encoders:

- **FPNG** (SSE2) — fastest for typical photographic content; widest CPU compatibility
- **FPNGe** (AVX2) — Google's AVX2-accelerated variant of FPNG
- **ZPNG** (zlib-ng) — best compression ratio for **screen content** (UI screenshots, dashboards, remote-desktop frames); uses zlib-ng's full deflate implementation with NEON/AVX2 acceleration

Contains **64-bit binaries for Linux, macOS, and Windows on x86_64, plus Linux, Windows and macOS on ARM64**, built and tested natively on each platform via GitHub Runners.

The appropriate FPNG/FPNGe encoder is selected **automatically at runtime** via a `hasAVX2()` CPUID probe — the AVX2 library is never loaded on unsupported hardware. ZPNG is loaded explicitly when its compression characteristics are needed. Channel conversion from Java's native ABGR/BGR format to RGBA/RGB is handled in C via SIMD byte shuffles (NEON on ARM64, SSE/AVX2 on x86).

**License:** [GNU Affero General Public License](https://www.gnu.org/licenses/agpl-3.0.html#license-text), Version 3 or later.

![C++](https://img.shields.io/badge/c++-%2300599C.svg?style=for-the-badge&logo=c%2B%2B&logoColor=white) ![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white) ![Linux](https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black) ![macOS](https://img.shields.io/badge/mac%20os-000000?style=for-the-badge&logo=macos&logoColor=F0F0F0) ![Windows](https://img.shields.io/badge/Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white) ![Git](https://img.shields.io/badge/git-%23F05033.svg?style=for-the-badge&logo=git&logoColor=white)

# How to use it

[Maven](#maven-artifacts) and [Gradle](#gradle-artifacts) artifacts are available, please see [below](#maven-artifacts).

```java
import com.manticore.tools.FPNGEncoder;     // SSE2 encoder (always safe to load)
import com.manticore.tools.FPNGE;           // AVX2 encoder (only load when supported)
import com.manticore.tools.ZPNG;            // zlib-ng encoder (best for screen content)

// Automatic runtime selection between FPNG and FPNGe
FPNGEncoder.ENCODER.fpng_init();
boolean useAVX2 = FPNGEncoder.ENCODER.hasAVX2() != 0;

byte[] png;
if (useAVX2) {
    png = FPNGE.encode(bufferedImage, 4, 5);        // AVX2, 4 channels, best compression
} else {
    png = FPNGEncoder.encode(bufferedImage, 4, 0);   // SSE2, 4 channels, fastest compression
}

// ZPNG for screen content — pick a level by your bandwidth budget:
//   level 1: fastest, 30-40% larger files than level 5
//   level 3: well-balanced default for most screen content
//   level 5: typical sweet spot for slow links / mobile
byte[] screenshotPng = ZPNG.encode(bufferedImage, 4, 5);
```

There are 11 projects included:

- `encoder-java` is an abstract base class for loading the native libraries, byte arrays and tests
- `fpng` is the C++ source from [FPNG](https://github.com/richgel999/fpng) with an additional C wrapper (SSE2)
- `fpng-java` provides the Java `FPNGEncoder`, depending on `fpng` and `JNA`
- `fpnge` is the AVX2 optimised C++ source from [FPNGe](https://github.com/veluca93/fpnge) with an additional C wrapper
- `fpnge-java` provides the Java `FPNGE` encoder, depending on `fpnge` and `JNA`
- `zpng` is a [zlib-ng](https://github.com/zlib-ng/zlib-ng) backed encoder optimised for screen content; statically links zlib-ng and supports compression levels 0..9
- `zpng-java` provides the Java `ZPNG` encoder via JNA
- `zpng-java23` provides FFM-based wrappers (JDK 22+) for `ZPNG`
- `benchmark` are optional JMH based performance tests
- `maven-test` is a minimal Java project stub for testing the Maven dependencies and the native libs on various OS after publishing
- `fpng-java23` used Java23 FFM contains both `FPNG` and `FPNGE` (without need for JNA)

# When to use which encoder

The three encoders solve different problems. Pick based on your input:

- **Photographic / camera content** → FPNG or FPNGe. These produce larger files than zlib-based encoders but are 5-10× faster, which dominates total transfer-time when bandwidth is generous.
- **Screen content** (UI screenshots, charts, dashboards, remote-desktop tiles, anything with sharp edges and large flat-colour regions) → **ZPNG**. zlib-ng's deflate exploits the redundancy in this kind of content far better than FPNG's lookup tables, producing files that are often half the size for a small CPU-time cost.
- **Bandwidth-constrained delivery** (mobile, slow links, high latency) → **ZPNG** at level 3-5. The smaller file dominates total time-on-wire even though encoding is slower.
- **Need maximum throughput regardless of file size** (LAN-only delivery, throwaway frames) → FPNG/FPNGe at compression level 0-1.
- **Targeting JDK 22+** → use `zpng-java23` (FFM-based, lower per-call overhead than JNA).

ZPNG's compression levels follow zlib-ng's 0..9 convention: 0 = stored, 1 = fastest, 9 = best compression. Levels 1-5 are the practical range for screen content; levels 6-9 add CPU cost for diminishing size returns.

The following Gradle task will compile the native libraries with `-O3 -march=x86-64 -mtune=generic` and wrap them into JARs via JNA.

```bash
git clone --depth 1 https://github.com/manticore-projects/fpng-java.git
cd fpng-java
gradle clean assemble
```

The artifacts will be written to `fpng-java/build/libs/fpng-java-2.0.2.jar` and `fpnge-java/build/libs/fpnge-java-2.0.2.jar`.


# Benchmarks

There is a JMH based benchmark suite comparing other Java PNG Encoders, using one small and one very large PNG:

```bash
gradle clean assemble jmh
```

```text
# JMH version: 1.37, Blackhole mode: compiler
# VM version: JDK 21.0.1, OpenJDK 64-Bit Server VM, 21.0.1+12-jvmci-23.1-b19 -XX:+UseSerialGC -Xms512M -Xmx2G -XX:+UseStringDeduplication

Benchmark                               (channels)              (imageName)  Mode  Cnt     Score    Error  Units
FPNGEBenchmark.encode                            3              example.png  avgt   10     1.993 ±  0.008  ms/op
FPNGEBenchmark.encode                            3  looklet-look-scale6.png  avgt   10    84.272 ±  0.860  ms/op
FPNGEBenchmark.encode                            4              example.png  avgt   10     2.976 ±  0.014  ms/op
FPNGEBenchmark.encode                            4  looklet-look-scale6.png  avgt   10   126.364 ± 11.213  ms/op
FPNGEncoderBenchmark.encode                      3              example.png  avgt   10     5.057 ±  0.046  ms/op
FPNGEncoderBenchmark.encode                      3  looklet-look-scale6.png  avgt   10   196.432 ±  0.836  ms/op
FPNGEncoderBenchmark.encode                      4              example.png  avgt   10     6.434 ±  0.054  ms/op
FPNGEncoderBenchmark.encode                      4  looklet-look-scale6.png  avgt   10   268.631 ± 12.623  ms/op
ImageIOEncoderBenchmark.encode                   3              example.png  avgt   10    54.189 ±  0.422  ms/op
ImageIOEncoderBenchmark.encode                   3  looklet-look-scale6.png  avgt   10  1099.045 ± 13.667  ms/op
ImageIOEncoderBenchmark.encode                   4              example.png  avgt   10    64.538 ±  0.605  ms/op
ImageIOEncoderBenchmark.encode                   4  looklet-look-scale6.png  avgt   10  1270.494 ±  6.797  ms/op
ObjectPlanetPNGEncoderBenchmark.encode           3              example.png  avgt   10    43.910 ±  0.248  ms/op
ObjectPlanetPNGEncoderBenchmark.encode           3  looklet-look-scale6.png  avgt   10  1491.651 ±  8.445  ms/op
ObjectPlanetPNGEncoderBenchmark.encode           4              example.png  avgt   10    53.449 ±  0.279  ms/op
ObjectPlanetPNGEncoderBenchmark.encode           4  looklet-look-scale6.png  avgt   10  1776.148 ±  7.182  ms/op
PNGEncoderBenchmark.encode                       3              example.png  avgt   10    39.813 ±  0.517  ms/op
PNGEncoderBenchmark.encode                       3  looklet-look-scale6.png  avgt   10   854.307 ±  2.627  ms/op
PNGEncoderBenchmark.encode                       4              example.png  avgt   10    44.704 ±  0.252  ms/op
PNGEncoderBenchmark.encode                       4  looklet-look-scale6.png  avgt   10  1120.410 ±  6.746  ms/op
PNGEncoderBenchmark.encodeFastest                3              example.png  avgt   10    23.990 ±  0.173  ms/op
PNGEncoderBenchmark.encodeFastest                3  looklet-look-scale6.png  avgt   10   311.706 ±  1.456  ms/op
PNGEncoderBenchmark.encodeFastest                4              example.png  avgt   10    28.218 ±  0.202  ms/op
PNGEncoderBenchmark.encodeFastest                4  looklet-look-scale6.png  avgt   10   428.018 ±  5.281  ms/op
```

![Small Image Benchmark Results](src/site/sphinx/_static/benchmark_small.svg "Small Image Benchmark Results")
![Large Image Benchmark Results](src/site/sphinx/_static/benchmark_large.svg "Large Image Benchmark Results")
**Remark:** Score in milliseconds per encoding, smaller is better.

The **compression rates** were set to `MEDIUM` for achieving comparable file-sizes. The Java Encoders are able to achieve better compression rates at an even higher performance penalty.
`FPNG SSE` and `FPNGe AVX` achieve very competitive file-sizes for smaller images but fall-off considerably for the very large image (1.5x the size of ImageIO). Please see details in the [Benchmark Spreadsheet](src/site/sphinx/_static/benchmark.ods).

# Maven Artifacts

```xml
<dependencies>
    <!-- SSE2 encoder (always safe, also provides hasAVX2() probe) -->
    <dependency>
        <groupId>com.manticore-projects.tools</groupId>
        <artifactId>fpng-java</artifactId>
        <version>[2.0.2,)</version>
    </dependency>
    <!-- AVX2 encoder (only load at runtime when hasAVX2() returns true) -->
    <dependency>
        <groupId>com.manticore-projects.tools</groupId>
        <artifactId>fpnge-java</artifactId>
        <version>[2.0.2,)</version>
    </dependency>
    <!-- zlib-ng-backed encoder, optimised for screen content -->
    <dependency>
        <groupId>com.manticore-projects.tools</groupId>
        <artifactId>zpng-java</artifactId>
        <version>[2.0.2,)</version>
    </dependency>
</dependencies>
```

# Gradle Artifacts

```groovy
repositories {
    mavenCentral()
}
dependencies {
    // SSE2 encoder (always safe, also provides hasAVX2() probe)
    implementation 'com.manticore-projects.tools:fpng-java:[2.0.2,)'
    // AVX2 encoder (only load at runtime when hasAVX2() returns true)
    implementation 'com.manticore-projects.tools:fpnge-java:[2.0.2,)'
    // zlib-ng-backed encoder, optimised for screen content
    implementation 'com.manticore-projects.tools:zpng-java:[2.0.2,)'
}
```

# PGO

```bash
# Step 1 — Baseline (no PGO). Saves the result file.
./gradlew :fpnge:clean :benchmark:jmh
cp benchmark/build/results/jmh/results.json baseline.json

# Step 2 — Build instrumented and run training. JMH numbers from this
# run are MEANINGLESS (instrumentation overhead makes everything ~30%
# slower). The run's purpose is just to populate .pgo/fpnge/*.gcda.
./gradlew :fpnge:clean :benchmark:jmh -PpgoStage=generate

# Step 3 — Sanity check: profile data was actually written.
find .pgo/fpnge -name '*.gcda' | head
#   If this is empty, the training didn't reach the instrumented lib.
#   Most likely cause: pgoStage flag wasn't propagated to the C++ build.
#   STOP HERE if empty -- step 4 will fail with the GradleException.

# Step 4 — Build PGO-optimized and run the actual measurement.
./gradlew :fpnge:clean :benchmark:jmh -PpgoStage=use
cp benchmark/build/results/jmh/results.json pgo.json

# Step 5 — Compare the two JSON files. Quick eyeball:
diff <(jq '.[].primaryMetric.score' baseline.json) \
     <(jq '.[].primaryMetric.score' pgo.json)
```
