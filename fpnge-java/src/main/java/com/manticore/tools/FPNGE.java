package com.manticore.tools;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
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
        BufferedImage convertedImage = new BufferedImage(
                width,
                height,
                BufferedImage.TYPE_4BYTE_ABGR
        );

        // Draw the original image onto the new image
        convertedImage.getGraphics().drawImage(image, 0, 0, null);

        // get the 4BYTE ABGR Array
        DataBufferByte dataBuffer = (DataBufferByte) convertedImage.getRaster().getDataBuffer();
        byte[] rgbaArray = dataBuffer.getData();

        // swap the bytes to RGBA (this maybe could be done in C faster)
        // swapIntBytes(rgbaArray);

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
