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

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;

@State(Scope.Benchmark)
public abstract class EncoderBenchmark {

    @Param({"example.png", "looklet-look-scale6.png"}) // Add more PNG file names as needed
    String imageName;

    @Param({"3", "4"})
    int channels;

    BufferedImage image;
    Blackhole blackhole;

    long size;

    public static void main(String[] args) throws Exception {}

    @Setup
    public void setup(Blackhole blackhole) throws IOException {
        this.blackhole = blackhole;
        image = loadImage(imageName);
    }

    @TearDown
    public void tearDown() throws IOException {
        DateFormat dtf = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String fileName =
                System.getProperty("java.io.tmpdir") + File.separator + "EncoderBenchmark.csv";
        try (FileWriter writer = new FileWriter(fileName, Charset.defaultCharset(), true);) {
            writer.append(dtf.format(new Date())).append(";")
                    .append(this.getClass().getSimpleName()).append(";")
                    .append(imageName).append(";")
                    .append(String.valueOf(channels)).append(";")
                    .append(String.valueOf(size))
                    .append("\n");
            writer.flush();
        }
    }

    private BufferedImage loadImage(String imageName) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(imageName)) {
            assert stream != null;
            BufferedImage image = ImageIO.read(stream);

            // Re-Encode according to the channels
            BufferedImage convertedImage = new BufferedImage(
                    image.getWidth(), image.getHeight(), channels == 4
                            ? BufferedImage.TYPE_INT_ARGB
                            : BufferedImage.TYPE_INT_RGB);

            // Draw the original image onto the new image
            convertedImage.getGraphics().drawImage(image, 0, 0, null);

            return convertedImage;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Error loading image: " + imageName, e);
        }
    }

    abstract void encode() throws IOException;
}
