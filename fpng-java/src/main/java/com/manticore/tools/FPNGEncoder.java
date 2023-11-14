package com.manticore.tools;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public interface FPNGEncoder extends Library {
    FPNGEncoder ENCODER = (FPNGEncoder) Native.load("../fpng/build/lib/main/debug/shared/linux/x86-64/libfpng.so", FPNGEncoder.class);

    void fpng_init();

    public static class ByteArray extends Structure {
        public Pointer data;
        public NativeLong size;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data", "size");
        }
    }

    ByteArray fpng_encode_image_to_memory(Pointer pImage, int w, int h, int num_chans, int flags);

    // Declare the function that returns a ByteArray
    ByteArray createDynamicArray();

    void releaseVectorData(ByteArray data);

    public static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {

        // Native.setProtected(true);

        /*
        num_chans must be 3 or 4. There must be w*3*h or w*4*h bytes pointed to by pImage.
        The image row pitch is always w*3 or w*4 bytes.
        There is no automatic determination if the image actually uses an alpha channel, so if you call it with 4 you will always get a 32bpp .PNG file.
         */

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

        // gives a BGRA bytestream
        // DataBufferByte dataBuffer = (DataBufferByte) image.getRaster().getDataBuffer();
        // byte[] rgbaArray = dataBuffer.getData();

        long start = System.currentTimeMillis();

        Pointer pImage = new Memory(rgbaArray.length);
        // Copy the content of the byte array to native memory
        pImage.write(0, rgbaArray, 0, rgbaArray.length);

        int numChannels = 4;

        ENCODER.fpng_init();

        ByteArray byteArray = ENCODER.fpng_encode_image_to_memory(pImage, image.getWidth(), image.getHeight(), numChannels,  0);
        long size = byteArray.size.longValue();
        byte[] data = byteArray.data.getByteArray(0, (int) size);
        Native.free(Pointer.nativeValue(byteArray.data));

        return data;
    }
}
