######################################
FPNG Java
######################################

.. toctree::
   :maxdepth: 2
   :hidden:

   usage
   FPNG Java API <javadoc_fpng.rst>
   FPNGE Java API <javadoc_fpnge.rst>
   Changelog <changelog.md>


**FPNG-Java** provides Java wrappers around two native, SIMD-optimised PNG encoders — **FPNG** (SSE2) and **FPNGE** (AVX2). Both are accessed through JNA and produce fully compliant PNG files at significantly higher throughput than ``javax.imageio``.

Latest stable release: |FPNG_ENCODER_STABLE_VERSION_LINK|

*******************************
Features
*******************************

    * **Two encoders** with automatic runtime selection

        * **FPNG** — SSE2/SSE4.1/PCLMUL, works on any x86-64 CPU
        * **FPNGE** — AVX2, used automatically when the CPU supports it

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