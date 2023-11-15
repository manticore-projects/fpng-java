package com.manticore.tools;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public interface FPNGE extends Library {
    FPNGE ENCODER = (FPNGE) Native.load("../fpnge/build/lib/main/debug/shared/linux/x86-64/libfpnge.so", FPNGE.class);

    void fpng_init();

    public static enum FPNGECicpColorspace {
        FPNGE_CICP_NONE
        , FPNGE_CICP_PQ
    };

    public static enum FPNGEOptionsPredictor {
        FPNGE_PREDICTOR_FIXED_NOOP,
        FPNGE_PREDICTOR_FIXED_SUB,
        FPNGE_PREDICTOR_FIXED_TOP,
        FPNGE_PREDICTOR_FIXED_AVG,
        FPNGE_PREDICTOR_FIXED_PAETH,
        FPNGE_PREDICTOR_APPROX,
        FPNGE_PREDICTOR_BEST
    };

    public static class FPNGEOptions extends Structure {
        public char predictor=0; // FPNGEOptionsPredictor
        public char huffman_sample=10; // 0-127: how much of the image to sample
        public char cicp_colorspace=0; // FPNGECicpColorspace

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("predictor", "huffman_sample", "cicp_colorspace");
        }
    }

    public static class CharArray extends Structure {
        public Pointer data;
        public NativeLong size;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data", "size");
        }
    }


    CharArray FPNGEEncode1(NativeLong bytes_per_channel, NativeLong num_channels, byte[] pImage, NativeLong width,
                          NativeLong height);


    void releaseVectorData(CharArray data);

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

        NativeLong bytes_per_channel = new NativeLong(1);
        NativeLong num_channels = new NativeLong(4);
        NativeLong height1 = new NativeLong(height);
        NativeLong width1 = new NativeLong(width);

        CharArray byteArray = ENCODER.FPNGEEncode1( bytes_per_channel,  num_channels, rgbaArray,  width1,  height1);

        long size = byteArray.size.longValue();
        byte[] data = byteArray.data.getByteArray(0, (int) size);
        Native.free(Pointer.nativeValue(byteArray.data));

        return data;
    }
}
