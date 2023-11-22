package com.manticore;

import org.openjdk.jmh.annotations.Benchmark;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageIOEncoderBenchmark extends EncoderBenchmark {
    @Benchmark
    public void encode() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", os);
        byte[] result = os.toByteArray();

        blackhole.consume(result);
    }
}
