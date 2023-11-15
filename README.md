# fpng-java
Java Wrapper for the fast, native [FPNG Encoder](https://github.com/richgel999/fpng) and the AVX optimized native [FPNGE Encoder](https://github.com/veluca93/fpnge).

[![Java CI with Gradle](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle.yml/badge.svg)](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle.yml) [![Gradle Package](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle-publish.yml/badge.svg)](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle-publish.yml)

# How to use it

There are 5 projects included:
- `fpng` is the C++ source from [FPNG](https://github.com/richgel999/fpng) with an additional C wrapper
- `fpng-java` is the Java Wrapper, depending on `fpng` and `JNA`
- `fpnge` is the AVX optimized C++ source from [FPNGE](https://github.com/veluca93/fpnge) with an additional C wrapper
- `fpnge-java` is the Java Wrapper, depending on `fpnge` and `JNA`
- `benchmark` are optional JMH based performance tests

The following Gradle task will compile FPNG with `-O3 -march-native` and wrap it into a JAR via JNA.

```bash
git clone --depth 1 https://github.com/manticore-projects/fpng-java.git
cd fpng-java
gradle clean assemble
```
The artifact will be written to: `.fpng-java/build/libs/fpng-java-1.0-SNAPSHOT.jar`

# Benchmarks

There is a JMH based benchmark suite comparing other Java PNG Encoders, using one small and one very large PNG:

```bash
gradle clean assemble jmh
```

Interestingly the Benchmarks heavily depend on the JDK, with GraalVM 11 or 21 being much faster for the Native Libs than OpenJDK 11/21 or JetBrains JDK 11/21. This affects only the Native Libs and we will have to investigate the reason.

```text
GRAAL VM 11

Benchmark                                           (imageName)  Mode  Cnt     Score    Error  Units
FPNGEBenchmark.encode                               example.png  avgt    3     5.895 ±  0.369  ms/op
FPNGEBenchmark.encode                   looklet-look-scale6.png  avgt    3   242.326 ±  3.215  ms/op
FPNGEncoderBenchmark.encode                         example.png  avgt    3     7.267 ±  4.725  ms/op
FPNGEncoderBenchmark.encode             looklet-look-scale6.png  avgt    3   353.969 ± 14.833  ms/op
ImageIOEncoderBenchmark.encode                      example.png  avgt    3    54.096 ±  0.742  ms/op
ImageIOEncoderBenchmark.encode          looklet-look-scale6.png  avgt    3  1285.791 ± 14.237  ms/op
ObjectPlanetPNGEncoderBenchmark.encode              example.png  avgt    3    25.882 ±  0.670  ms/op
ObjectPlanetPNGEncoderBenchmark.encode  looklet-look-scale6.png  avgt    3   639.232 ± 23.186  ms/op
PNGEncoderBenchmark.encode                          example.png  avgt    3    29.218 ±  0.965  ms/op
PNGEncoderBenchmark.encode              looklet-look-scale6.png  avgt    3   573.997 ± 16.234  ms/op
PNGEncoderBenchmark.encodeFastest                   example.png  avgt    3    17.628 ±  0.511  ms/op
PNGEncoderBenchmark.encodeFastest       looklet-look-scale6.png  avgt    3   362.208 ±  4.346  ms/op
```

```text
JetBrains JDK 21

Benchmark                                           (imageName)  Mode  Cnt     Score    Error  Units
FPNGEBenchmark.encode                               example.png  avgt    3    11.688 ±  0.285  ms/op
FPNGEBenchmark.encode                   looklet-look-scale6.png  avgt    3   530.782 ± 39.565  ms/op
FPNGEncoderBenchmark.encode                         example.png  avgt    3    13.006 ±  0.230  ms/op
FPNGEncoderBenchmark.encode             looklet-look-scale6.png  avgt    3   650.211 ± 38.461  ms/op
ImageIOEncoderBenchmark.encode                      example.png  avgt    3    56.187 ±  1.557  ms/op
ImageIOEncoderBenchmark.encode          looklet-look-scale6.png  avgt    3  1359.781 ± 54.542  ms/op
ObjectPlanetPNGEncoderBenchmark.encode              example.png  avgt    3    32.995 ±  1.255  ms/op
ObjectPlanetPNGEncoderBenchmark.encode  looklet-look-scale6.png  avgt    3   872.825 ± 25.337  ms/op
PNGEncoderBenchmark.encode                          example.png  avgt    3    29.636 ±  2.403  ms/op
PNGEncoderBenchmark.encode              looklet-look-scale6.png  avgt    3   573.655 ± 50.611  ms/op
PNGEncoderBenchmark.encodeFastest                   example.png  avgt    3    17.548 ±  0.858  ms/op
PNGEncoderBenchmark.encodeFastest       looklet-look-scale6.png  avgt    3   358.136 ± 22.376  ms/op
```

# To Do

- [ ] Right now we compare only the speed without paying attention to the size of the encoded image. We will need to calibrate the benchmarks to compare only modes producing similar sizes. Also, 24bit vs 32bit modes need to be honored.
- [ ] Benchmark the translation of the `BufferedImage` into the `RGBA` byte array, which is right now Pixel based and likely slow.
- [ ] Further we should add more test images for the "screen capturing" use case, which may yield different outcomes. Right now only photo-realistic images are tested.
- [ ] Publish Artifact to Maven/Sonatype.
- [ ] Fat/Ueber JAR with support for the 4 major Operating Systems.
- [ ] Drop slow JNA and replace with a JNI implementation.
- [ ] Investigate the difference in performance on Graal JDK vs OpenJDK or JetBrains JDK.
