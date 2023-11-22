package com.manticore;

import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@State(Scope.Benchmark)
public abstract class EncoderBenchmark {

    @Param({"example.png", "looklet-look-scale6.png"})  // Add more PNG file names as needed
    String imageName;
    BufferedImage image;
    Blackhole blackhole;

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    @Setup
    public void setup(Blackhole blackhole) {
        this.blackhole = blackhole;
        image = loadImage(imageName);
    }

    private BufferedImage loadImage(String imageName) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(imageName)) {
            assert stream!=null;
            return ImageIO.read(stream);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("Error loading image: " + imageName, e);
        }
    }

    abstract void encode() throws IOException;
}
