package com.manticore.tools;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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

    ByteArray fpng_encode_image_to_memory(byte[] pImage, int w, int h, int num_chans, int flags);

    void releaseVectorData(ByteArray data);

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

        ENCODER.fpng_init();

        ByteArray byteArray = ENCODER.fpng_encode_image_to_memory(rgbaArray, width, height, numChannels,  0);
        long size = byteArray.size.longValue();
        byte[] data = byteArray.data.getByteArray(0, (int) size);
        Native.free(Pointer.nativeValue(byteArray.data));

        return data;
    }
}
