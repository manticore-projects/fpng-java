package com.manticore;

import com.manticore.tools.FPNGE;
import org.openjdk.jmh.annotations.Benchmark;

public class FPNGEBenchmark extends EncoderBenchmark {
    // @Param({"1", "2", "3", "4", "5"})
    // int compressionLevel;

    @Benchmark
    public void encode() {
        byte[] result = FPNGE.encode(image, channels, 5);
        size = result.length;
        blackhole.consume(result);
    }

}
