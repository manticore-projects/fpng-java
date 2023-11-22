package com.manticore.tools;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public interface FPNGE extends Encoder {
    FPNGE ENCODER = (FPNGE) Encoder.load(FPNGE.class, "fpnge");

    public static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        /*
        num_chans must be 3 or 4. There must be w*3*h or w*4*h bytes pointed to by pImage.
        The image row pitch is always w*3 or w*4 bytes.
        There is no automatic determination if the image actually uses an alpha channel, so if you call it with 4 you will always get a 32bpp .PNG file.
         */

        byte[] rgbaArray = com.manticore.tools.Encoder.getRGBABytes(image, numberOfChannels);

        NativeLong bytes_per_channel = new NativeLong(1);
        NativeLong num_chan = new NativeLong(numberOfChannels);
        NativeLong height = new NativeLong(image.getHeight());
        NativeLong width = new NativeLong(image.getWidth());

        CharArray byteArray = ENCODER.FPNGEEncode1(bytes_per_channel, num_chan, rgbaArray, width, height);

        byte[] data = byteArray.data.getByteArray(0, byteArray.size.intValue());
        Native.free(Pointer.nativeValue(byteArray.data));

        return data;
    }

    void fpng_init();

    ;

    CharArray FPNGEEncode1(NativeLong bytes_per_channel, NativeLong num_channels, byte[] pImage, NativeLong width,
                           NativeLong height);

    ;

    public static enum FPNGECicpColorspace {
        FPNGE_CICP_NONE, FPNGE_CICP_PQ
    }

    public static enum FPNGEOptionsPredictor {
        FPNGE_PREDICTOR_FIXED_NOOP,
        FPNGE_PREDICTOR_FIXED_SUB,
        FPNGE_PREDICTOR_FIXED_TOP,
        FPNGE_PREDICTOR_FIXED_AVG,
        FPNGE_PREDICTOR_FIXED_PAETH,
        FPNGE_PREDICTOR_APPROX,
        FPNGE_PREDICTOR_BEST
    }

    public static class FPNGEOptions extends Structure {
        public char predictor = 0; // FPNGEOptionsPredictor
        public char huffman_sample = 10; // 0-127: how much of the image to sample
        public char cicp_colorspace = 0; // FPNGECicpColorspace

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
}
