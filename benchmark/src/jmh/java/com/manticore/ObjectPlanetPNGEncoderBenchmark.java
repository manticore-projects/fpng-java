package com.manticore;

import com.objectplanet.image.PngEncoder;
import org.openjdk.jmh.annotations.Benchmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.objectplanet.image.PngEncoder.COLOR_TRUECOLOR;
import static com.objectplanet.image.PngEncoder.COLOR_TRUECOLOR_ALPHA;
import static com.objectplanet.image.PngEncoder.DEFAULT_COMPRESSION;

public class ObjectPlanetPNGEncoderBenchmark extends EncoderBenchmark {
    @Benchmark
    public void encode() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PngEncoder encoder = new PngEncoder(channels == 4 ? COLOR_TRUECOLOR_ALPHA : COLOR_TRUECOLOR,
                DEFAULT_COMPRESSION);
        encoder.encode(image, os);
        byte[] result = os.toByteArray();
        os.close();
        size = result.length;
        blackhole.consume(result);
    }
}
