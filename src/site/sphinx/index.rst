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
   Changelog <changelog.md>


**FPNG-Java** provides Java wrappers around two native, SIMD-optimised PNG encoders — **FPNG** (SSE2) and **FPNGE** (AVX2). Both produce fully compliant PNG files at significantly higher throughput than ``javax.imageio``.

The JDK23+ version of the encoders uses Foreign Function & Memory API and is the fastest. Otherwise JNI is used via JNA, which comes with some overhead.

**Latest stable release:** |FPNG_ENCODER_STABLE_VERSION_LINK|

**GitHub Repository:** https://github.com/manticore-projects/fpng-java

*******************************
Features
*******************************

* **Two encoders** with automatic runtime selection
    * **FPNG** — SSE2/SSE4.1/PCLMUL, works on any x86-64 CPU
    * **FPNGE** — AVX2, used automatically when the CPU supports it
* FFM for JDK22+, JNI/JNA for older JDKs
* Runtime ``hasAVX2()`` CPUID probe — no AVX2 library is loaded on unsupported hardware
* Encodes ``BufferedImage`` directly — 3-channel (RGB) and 4-channel (RGBA)
* Handles Java's native ``TYPE_3BYTE_BGR`` / ``TYPE_4BYTE_ABGR`` channel order in C via SIMD byte shuffles
* Interleaved Adler32 checksum — no redundant full-image pass
* PCLMUL-accelerated CRC32
* Supports ``Java 8``, ``Java 11``, ``Java 17`` and ``Java 21``
* Platforms: Linux x86-64, Windows x86/x86-64, macOS x86-64

*******************************
Benchmarks
*******************************

Run the JMH benchmarks with:

.. code-block:: shell

    ./gradlew jmh

Results are written to ``benchmark/build/results/jmh/results.json``.

.. raw:: html
    :file: _static/benchmark-report.html
