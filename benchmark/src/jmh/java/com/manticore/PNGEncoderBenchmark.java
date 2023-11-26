/**
 * FPNG-Java is a Java Wrapper around the fast SSE/AVX optimised FPNG encoders.
 * Copyright (C) 2023 Andreas Reichel <andreas@manticore-projects.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/agpl-3.0.html#license-text/>.
 */
package com.manticore;

import com.pngencoder.PngEncoder;
import org.openjdk.jmh.annotations.Benchmark;

public class PNGEncoderBenchmark extends EncoderBenchmark {
    @Benchmark
    public void encode() {
        byte[] result = new PngEncoder()
                .withBufferedImage(image)
                .withMultiThreadedCompressionEnabled(false)
                .withPredictorEncoding(true)
                .withCompressionLevel(1)
                .toBytes();
        size = result.length;
        blackhole.consume(result);
    }

    @Benchmark
    public void encodeFastest() {
        byte[] result = new PngEncoder()
                .withBufferedImage(image)
                .withMultiThreadedCompressionEnabled(false)
                .withPredictorEncoding(false)
                .withCompressionLevel(1)
                .toBytes();
        size = result.length;
        blackhole.consume(result);
    }
}
