# fpng-java
Java Wrapper for the fast, native [FPNG Encoder](https://github.com/richgel999/fpng)

[![Java CI with Gradle](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle.yml/badge.svg)](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle.yml) [![Gradle Package](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle-publish.yml/badge.svg)](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle-publish.yml)

# How to use it

There are 3 projects included:
- `fpng` is the C++ source from [FPNG](https://github.com/richgel999/fpng) with an additional C wrapper
- `fpng-java` is the Java Wrapper, depending on `fpng` and `JNA`
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
gradle jmh
```

```text

Benchmark                                           (imageName)  Mode  Cnt     Score     Error  Units
FPNGEncoderBenchmark.encode                         example.png  avgt    3     6.965 ±   0.241  ms/op
FPNGEncoderBenchmark.encode             looklet-look-scale6.png  avgt    3   435.029 ±  25.064  ms/op
ImageIOEncoderBenchmark.encode                      example.png  avgt    3    54.884 ±   1.281  ms/op
ImageIOEncoderBenchmark.encode          looklet-look-scale6.png  avgt    3  1301.629 ± 100.656  ms/op
ObjectPlanetPNGEncoderBenchmark.encode              example.png  avgt    3    26.028 ±   1.994  ms/op
ObjectPlanetPNGEncoderBenchmark.encode  looklet-look-scale6.png  avgt    3   648.007 ±  48.756  ms/op
PNGEncoderBenchmark.encode                          example.png  avgt    3    29.644 ±   2.647  ms/op
PNGEncoderBenchmark.encode              looklet-look-scale6.png  avgt    3   578.604 ±   6.321  ms/op
PNGEncoderBenchmark.encodeFastest                   example.png  avgt    3    17.881 ±   2.468  ms/op
PNGEncoderBenchmark.encodeFastest       looklet-look-scale6.png  avgt    3   366.691 ±  21.033  ms/op
```

# To Do

- [ ] Right now we compare only the speed without paying attention to the size of the encoded image. We will need to calibrate the benchmarks to compare only modes producing similar sizes. Also, 24bit vs 32bit modes need to be honored.
- [ ] Benchmark the translation of the `BufferedImage` into the `RGBA` byte array, which is right now Pixel based and likely slow.
- [ ] Further we should add more test images for the "screen capturing" use case, which may yield different outcomes. Right now only photo-realistic images are tested. 
- [ ] Publish Artifact to Maven/Sonatype.
- [ ] Fat/Ueber JAR with support for the 4 major Operating Systems.
- [ ] Drop slow JNA and replace with a JNI implementation.




