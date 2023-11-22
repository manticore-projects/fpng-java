package com.manticore.tools;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

public interface FPNGEncoder extends Encoder {
    FPNGEncoder ENCODER = (FPNGEncoder) Encoder.load(FPNGEncoder.class, "fpng");

    void fpng_init();

    ByteArray fpng_encode_image_to_memory(byte[] pImage, int w, int h, int num_chans, int flags);

    public static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        ENCODER.fpng_init();

        /*
        num_chans must be 3 or 4. There must be w*3*h or w*4*h bytes pointed to by pImage.
        The image row pitch is always w*3 or w*4 bytes.
        There is no automatic determination if the image actually uses an alpha channel, so if you call it with 4 you will always get a 32bpp .PNG file.
         */

        byte[] rgbaArray = Encoder.getRGBABytes(image, numberOfChannels);

        ByteArray byteArray = ENCODER.fpng_encode_image_to_memory(rgbaArray, image.getWidth(), image.getHeight(), numberOfChannels,  0);
        byte[] data = byteArray.data.getByteArray(0, byteArray.size.intValue() );

        Native.free(Pointer.nativeValue(byteArray.data));

        return data;
    }
}
