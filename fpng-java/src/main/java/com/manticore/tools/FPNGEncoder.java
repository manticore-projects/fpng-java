package com.manticore.tools;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.awt.image.BufferedImage;

public interface FPNGEncoder extends Encoder {
    FPNGEncoder ENCODER = (FPNGEncoder) Encoder.load(FPNGEncoder.class, "fpng");

    public static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        ENCODER.fpng_init();

        /*
        num_chans must be 3 or 4. There must be w*3*h or w*4*h bytes pointed to by pImage.
        The image row pitch is always w*3 or w*4 bytes.
        There is no automatic determination if the image actually uses an alpha channel, so if you call it with 4 you will always get a 32bpp .PNG file.
         */

        byte[] rgbaArray = Encoder.getRGBABytes(image, numberOfChannels);

        ByteArray byteArray = ENCODER.fpng_encode_image_to_memory(rgbaArray, image.getWidth(), image.getHeight(), numberOfChannels, 0);
        byte[] data = byteArray.data.getByteArray(0, byteArray.size.intValue());

        Native.free(Pointer.nativeValue(byteArray.data));

        return data;
    }

    void fpng_init();

    ByteArray fpng_encode_image_to_memory(byte[] pImage, int w, int h, int num_chans, int flags);
}
