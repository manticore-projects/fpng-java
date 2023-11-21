# fpng-java
Java Wrapper for the fast, native [FPNG Encoder](https://github.com/richgel999/fpng) and the AVX optimized native [FPNGE Encoder](https://github.com/veluca93/fpnge).
It contains an additional SSE translation from Java's ABGR 4 byte into the expected RGBA 4 byte arrays (AVX has been tested to be slower, likely due to need for a 32-bit alignment). The JAR contains the **binaries for Windows, Linux and MacOS** (all 64 bit).

[![Java CI with Gradle](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle.yml/badge.svg)](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle.yml) [![Gradle Package](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle-publish.yml/badge.svg)](https://github.com/manticore-projects/fpng-java/actions/workflows/gradle-publish.yml)

# How to use it

[Maven](#maven-artifacts) and [Gradle](#gradle-artifacts) artifacts are available, please see [below](#maven-artifacts).

There are 7 projects included:
- `encoder` is a basis class for loading the native libraries, byte arrays and tests
- `fpng` is the C++ source from [FPNG](https://github.com/richgel999/fpng) with an additional C wrapper
- `fpng-java` is the Java Wrapper, depending on `fpng` and `JNA`
- `fpnge` is the AVX optimized C++ source from [FPNGE](https://github.com/veluca93/fpnge) with an additional C wrapper
- `fpnge-java` is the Java Wrapper, depending on `fpnge` and `JNA`
- `benchmark` are optional JMH based performance tests
- `maven-test` as a most simple Java project stub for testing the Maven dependencies and the Native Libs on various OS after publishing

The following Gradle task will compile FPNG with `-O3 -march-native` and wrap it into a JAR via JNA.

```bash
git clone --depth 1 https://github.com/manticore-projects/fpng-java.git
cd fpng-java
gradle clean assemble
```
The artifact will be written to: `.fpng-java/build/libs/fpng-java-0.99.0-SNAPSHOT.jar`


# Benchmarks

There is a JMH based benchmark suite comparing other Java PNG Encoders, using one small and one very large PNG:

```bash
gradle clean assemble jmh
```

```text
GRAAL VM 21
Benchmark                                           (imageName)  Mode  Cnt     Score    Error  Units
FPNGEBenchmark.encode                               example.png  avgt    3     3.147 ±  0.062  ms/op
FPNGEBenchmark.encode                   looklet-look-scale6.png  avgt    3   188.284 ± 84.303  ms/op
FPNGEncoderBenchmark.encode                         example.png  avgt    3     6.310 ±  0.115  ms/op
FPNGEncoderBenchmark.encode             looklet-look-scale6.png  avgt    3   322.365 ± 91.968  ms/op
ImageIOEncoderBenchmark.encode                      example.png  avgt    3    48.085 ±  3.540  ms/op
ImageIOEncoderBenchmark.encode          looklet-look-scale6.png  avgt    3  1239.409 ± 15.437  ms/op
ObjectPlanetPNGEncoderBenchmark.encode              example.png  avgt    3    32.568 ±  1.147  ms/op
ObjectPlanetPNGEncoderBenchmark.encode  looklet-look-scale6.png  avgt    3   876.725 ± 52.697  ms/op
PNGEncoderBenchmark.encode                          example.png  avgt    3    29.178 ±  0.287  ms/op
PNGEncoderBenchmark.encode              looklet-look-scale6.png  avgt    3   572.452 ± 17.830  ms/op
PNGEncoderBenchmark.encodeFastest                   example.png  avgt    3    17.461 ±  2.498  ms/op
PNGEncoderBenchmark.encodeFastest       looklet-look-scale6.png  avgt    3   367.480 ±  6.410  ms/op
```

```text
GRAALVM 11
Benchmark                                           (imageName)  Mode  Cnt     Score     Error  Units
FPNGEBenchmark.encode                               example.png  avgt    3     2.731 ±   0.018  ms/op
FPNGEBenchmark.encode                   looklet-look-scale6.png  avgt    3   182.363 ± 159.723  ms/op
FPNGEncoderBenchmark.encode                         example.png  avgt    3     6.491 ±   0.526  ms/op
FPNGEncoderBenchmark.encode             looklet-look-scale6.png  avgt    3   313.017 ±  71.408  ms/op
ImageIOEncoderBenchmark.encode                      example.png  avgt    3    47.353 ±   3.971  ms/op
ImageIOEncoderBenchmark.encode          looklet-look-scale6.png  avgt    3  1199.796 ±  47.642  ms/op
ObjectPlanetPNGEncoderBenchmark.encode              example.png  avgt    3    28.079 ±   0.101  ms/op
ObjectPlanetPNGEncoderBenchmark.encode  looklet-look-scale6.png  avgt    3   660.480 ±  79.759  ms/op
PNGEncoderBenchmark.encode                          example.png  avgt    3    29.172 ±   0.288  ms/op
PNGEncoderBenchmark.encode              looklet-look-scale6.png  avgt    3   574.485 ±  16.903  ms/op
PNGEncoderBenchmark.encodeFastest                   example.png  avgt    3    17.516 ±   0.135  ms/op
PNGEncoderBenchmark.encodeFastest       looklet-look-scale6.png  avgt    3   360.417 ±   8.995  ms/op
```

# Maven Artifacts
```xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
        <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>
<dependencies>
    <dependency>
        <groupId>com.manticore-projects.tools</groupId>
        <artifactId>encoder-java</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>com.manticore-projects.tools</groupId>
        <artifactId>fpng-java</artifactId>
        <version>1.0.0</version>
    </dependency>
    <dependency>
        <groupId>com.manticore-projects.tools</groupId>
        <artifactId>fpnge-java</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```
# Gradle Artifacts
```groovy
repositories {
    mavenCentral()
    maven {
        url = uri('https://s01.oss.sonatype.org/content/repositories/releases/')
    }
    maven {
        url = uri('https://s01.oss.sonatype.org/content/repositories/snapshots/')
    }
}
dependencies {
    implementation 'com.manticore-projects.tools:encoder-java:+'
    implementation 'com.manticore-projects.tools:fpng-java:+'
    implementation 'com.manticore-projects.tools:fpnge-java:+'
}
```

# To Do

- [ ] Right now we compare only the speed without paying attention to the size of the encoded image. We will need to calibrate the benchmarks to compare only modes producing similar sizes. Also, 24bit vs 32bit modes need to be honored.
- [X] Benchmark the translation of the `BufferedImage` into the `RGBA` byte array, which is right now Pixel based and likely slow.
- [ ] Further we should add more test images for the "screen capturing" use case, which may yield different outcomes. Right now only photo-realistic images are tested.
- [X] Publish Artifact to Maven/Sonatype.
- [X] Fat/Ueber JAR with support for the 4 major Operating Systems.
- [ ] Drop slow JNA and replace with a JNI implementation.
- [X] Investigate the difference in performance on Graal JDK vs OpenJDK or JetBrains JDK.
- [ ] Try profiling with PGO.
