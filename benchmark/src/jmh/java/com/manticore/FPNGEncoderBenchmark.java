package com.manticore;

import com.manticore.tools.FPNGEncoder;
import org.openjdk.jmh.annotations.Benchmark;

public class FPNGEncoderBenchmark extends EncoderBenchmark {

    /*
     * Enables computing custom Huffman tables for each file, instead of using the custom global
     * tables. Results in roughly 6% smaller files on average, but compression is around 40% slower.
     * FPNG_ENCODE_SLOWER = 1,
     * 
     * Only use raw Deflate blocks (no compression at all). Intended for testing.
     * FPNG_FORCE_UNCOMPRESSED = 2,
     */

    // @Param({"0", "1", "2"})
    // int compressionLevel;

    @Benchmark
    public void encode() {
        byte[] result = FPNGEncoder.encode(image, channels, 1);
        size = result.length;
        blackhole.consume(result);
    }
}
