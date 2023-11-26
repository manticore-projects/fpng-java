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

import org.openjdk.jmh.annotations.Benchmark;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class ByteBufferBenchmark extends EncoderBenchmark {
    public static void swapIntBytes(byte[] bytes) {
        assert bytes.length % 4 == 0;
        for (int i = 0; i < bytes.length; i += 4) {
            // swap 0 and 3
            byte tmp = bytes[i];
            bytes[i] = bytes[i + 3];
            bytes[i + 3] = tmp;
            // swap 1 and 2
            byte tmp2 = bytes[i + 1];
            bytes[i + 1] = bytes[i + 2];
            bytes[i + 2] = tmp2;
        }
    }

    @Benchmark

    public void encode() {
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();

        // Get the pixel data as an integer array
        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);

        // Create a byte array to store RGBA values
        byte[] rgbaArray = new byte[width * height * 4];

        // Extract RGBA values from each pixel
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            rgbaArray[i * 4] = (byte) ((pixel >> 16) & 0xFF); // Red
            rgbaArray[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF); // Green
            rgbaArray[i * 4 + 2] = (byte) (pixel & 0xFF); // Blue
            rgbaArray[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF); // Alpha
        }

        blackhole.consume(rgbaArray);
    }

    @Benchmark
    public void encodeDataBufferABGRWithChannelSwap() {
        // Create a new BufferedImage with TYPE_INT_ARGB
        BufferedImage convertedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);

        // Draw the original image onto the new image
        convertedImage.getGraphics().drawImage(image, 0, 0, null);

        DataBufferByte dataBuffer = (DataBufferByte) convertedImage.getRaster().getDataBuffer();
        byte[] rgbaArray = dataBuffer.getData();

        // Create a new byte array for the converted image
        byte[] convertedData = new byte[rgbaArray.length];

        // Swap the positions of red and blue channels (ABGR to RGBA)
        for (int i = 0; i < rgbaArray.length; i += 4) {
            convertedData[i] = rgbaArray[i + 3]; // Red
            convertedData[i + 1] = rgbaArray[i + 2]; // Green
            convertedData[i + 2] = rgbaArray[i + 1]; // Blue
            convertedData[i + 3] = rgbaArray[i]; // Alpha
        }

        blackhole.consume(convertedData);
    }

    @Benchmark
    public void encodeDataBufferABGRWithFastChannelSwap() {
        // Create a new BufferedImage with TYPE_4BYTE_ABGR
        BufferedImage convertedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);

        // Draw the original image onto the new image
        convertedImage.getGraphics().drawImage(image, 0, 0, null);

        // get the 4BYTE ABGR Array
        DataBufferByte dataBuffer = (DataBufferByte) convertedImage.getRaster().getDataBuffer();
        byte[] rgbaArray = dataBuffer.getData();

        // swap the bytes to RGBA (this maybe could be done in C faster)
        swapIntBytes(rgbaArray);

        size = rgbaArray.length;

        blackhole.consume(rgbaArray);
    }

}
