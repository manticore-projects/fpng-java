package com.manticore;

import com.manticore.tools.FPNGEncoder;
import org.openjdk.jmh.annotations.Benchmark;

public class FPNGEncoderBenchmark extends EncoderBenchmark {
    @Benchmark
    public void encode() {
        byte[] result = FPNGEncoder.encode(image, 4, 0);
        blackhole.consume(result);
    }
}
