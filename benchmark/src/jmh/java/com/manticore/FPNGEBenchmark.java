package com.manticore;

import com.manticore.tools.FPNGE;
import org.openjdk.jmh.annotations.Benchmark;

public class FPNGEBenchmark extends EncoderBenchmark {
    @Benchmark
    public void encode() {
        byte[] result = FPNGE.encode(image, 4, 0);
        blackhole.consume(result);
    }
}
