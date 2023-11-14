package com.manticore;

import com.objectplanet.image.PngEncoder;
import org.openjdk.jmh.annotations.Benchmark;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.objectplanet.image.PngEncoder.BEST_SPEED;
import static com.objectplanet.image.PngEncoder.COLOR_TRUECOLOR_ALPHA;

public class ObjectPlanetPNGEncoderBenchmark extends EncoderBenchmark {
    @Benchmark
    public void encode() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PngEncoder encoder=new PngEncoder(COLOR_TRUECOLOR_ALPHA, BEST_SPEED);
        encoder.encode(image, os);
        byte[] result = os.toByteArray();
        os.close();

        blackhole.consume(result);

    }
}
