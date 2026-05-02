.. meta::
   :description: Java Wrapper around SSE/AVX optimized PNG encoders
   :keywords: java, png, encoder, fast, optimized, native, sse, avx

######################################
FPNG Java
######################################

.. toctree::
   :maxdepth: 1
   :hidden:

   usage
   FPNG Java API <javadoc_fpng.rst>
   FPNGE Java API <javadoc_fpnge.rst>
   ZPNG Java API <javadoc_zpng.rst>
   Changelog <changelog.md>


**FPNG-Java** provides Java wrappers around three native, SIMD-optimised PNG encoders — **FPNG** (SSE2), **FPNGE** (AVX2), and **ZPNG** (zlib-ng). All produce fully compliant PNG files at significantly higher throughput than ``javax.imageio``.

The three encoders solve different problems:

* **FPNG** and **FPNGE** are tuned for photographic content and raw throughput. They use minimal-effort deflate via lookup tables, producing large files very fast.
* **ZPNG** uses zlib-ng's full deflate implementation and is tuned for **screen content** (UI screenshots, dashboards, remote-desktop tiles, anything with sharp edges and large flat-colour regions). It produces files about half the size of FPNG/FPNGE on this kind of input, at roughly 2-3× the encoding cost. For bandwidth-constrained delivery this is the right trade.

The JDK22+ version of the encoders uses Foreign Function & Memory API and is the fastest. Otherwise JNI is used via JNA, which comes with some overhead.

**Latest stable release:** |FPNG_ENCODER_STABLE_VERSION_LINK|

**GitHub Repository:** https://github.com/manticore-projects/fpng-java

*******************************
Features
*******************************

* **Three encoders**, each tuned for a different workload
    * **FPNG** — SSE2/SSE4.1/PCLMUL, works on any x86-64 CPU; raw throughput
    * **FPNGE** — AVX2, used automatically on capable CPUs; faster than FPNG
    * **ZPNG** — zlib-ng backed, NEON/AVX2 accelerated; best file size for screen content
* Runtime ``hasAVX2()`` CPUID probe — no AVX2 library is loaded on unsupported hardware
* FFM for JDK22+, JNA for older JDKs
* Encodes ``BufferedImage`` directly — 3-channel (RGB) and 4-channel (RGBA)
* Handles Java's native ``TYPE_3BYTE_BGR`` / ``TYPE_4BYTE_ABGR`` channel order in C via SIMD byte shuffles (NEON on ARM64, SSE/AVX2 on x86)
* Interleaved Adler32 checksum — no redundant full-image pass (FPNG/FPNGE)
* PCLMUL-accelerated CRC32
* Supports ``Java 8``, ``Java 11``, ``Java 17``, ``Java 21``, and ``Java 23``
* **Platforms:** Linux x86-64 / ARM64, macOS x86-64 / ARM64 (Apple Silicon), Windows x86 / x86-64
* All binaries built and tested natively on each target platform via GitHub Runners

*******************************
Benchmarks
*******************************

Run the JMH benchmarks with:

.. code-block:: shell

    ./gradlew jmh

Results are written to ``benchmark/build/results/jmh/results.json``.

Speed
**********

.. raw:: html
    :file: _static/benchmark-report.html



Efficiency
**********

.. raw:: html
   :file: _static/benchmark-pareto.html