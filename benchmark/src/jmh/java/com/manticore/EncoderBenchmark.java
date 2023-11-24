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
import java.text.DateFormat;
import java.util.Date;

@State(Scope.Benchmark)
public abstract class EncoderBenchmark {

    @Param({"example.png", "looklet-look-scale6.png"})  // Add more PNG file names as needed
    String imageName;

    @Param({"3", "4"})
    int channels;

    BufferedImage image;
    Blackhole blackhole;

    long size;

    public static void main(String[] args) throws Exception {
    }

    @Setup
    public void setup(Blackhole blackhole) throws IOException {
        this.blackhole = blackhole;
        image = loadImage(imageName);
    }

    @TearDown
    public void tearDown() throws IOException {
        DateFormat dtf = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String fileName = System.getProperty("java.io.tmpdir") + File.separator + "EncoderBenchmark.csv";
        try (FileWriter writer = new FileWriter(fileName, true);) {
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
            assert stream!=null;
            BufferedImage image = ImageIO.read(stream);

            // Re-Encode according to the channels
            BufferedImage convertedImage = new BufferedImage(
                    image.getWidth()
                    , image.getHeight()
                    , channels==4
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB
            );

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
