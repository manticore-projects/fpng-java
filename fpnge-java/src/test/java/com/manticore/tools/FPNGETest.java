package com.manticore.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class FPNGETest {

    @Test
    void encodeTest() {
        // Load the PNG file into a BufferedImage
        try (
                InputStream inputStream = ClassLoader.getSystemResourceAsStream("example.png")
        ) {
            assert inputStream!=null;
            BufferedImage image = ImageIO.read(inputStream);
            byte[] data = FPNGE.encode(image, 4, 0);

            Assertions.assertNotNull(data);
            Assertions.assertTrue(data.length > 0);

            File file = File.createTempFile("fpngeEncoder", ".png");

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}