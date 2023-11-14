package com.manticore;

import com.pngencoder.PngEncoder;
import org.openjdk.jmh.annotations.Benchmark;

public class PNGEncoderBenchmark extends EncoderBenchmark {
    @Benchmark
    public void encode() {
        byte[] result = new PngEncoder()
                .withBufferedImage(image)
                .withMultiThreadedCompressionEnabled(false)
                .withCompressionLevel(2)
                .toBytes();
        blackhole.consume(result);
    }

    @Benchmark
    public void encodeFastest() {
        byte[] result = new PngEncoder()
                .withBufferedImage(image)
                .withMultiThreadedCompressionEnabled(false)
                .withCompressionLevel(1)
                .toBytes();
        blackhole.consume(result);
    }
}
