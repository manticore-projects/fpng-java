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
package com.manticore.tools;

import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.awt.image.BufferedImage;

public interface FPNGEncoder extends Encoder {
    FPNGEncoder ENCODER = (FPNGEncoder) Encoder.load(FPNGEncoder.class, "fpng");

    static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        ENCODER.fpng_init();

        /*
         * num_chans must be 3 or 4. There must be w*3*h or w*4*h bytes pointed to by pImage. The
         * image row pitch is always w*3 or w*4 bytes. There is no automatic determination if the
         * image actually uses an alpha channel, so if you call it with 4 you will always get a
         * 32bpp .PNG file.
         */

        byte[] rgbaArray = Encoder.getRGBABytes(image, numberOfChannels);

        ByteArray byteArray = ENCODER.fpng_encode_image_to_memory(rgbaArray, image.getWidth(),
                image.getHeight(), numberOfChannels, 0);
        byte[] data = byteArray.data.getByteArray(0, byteArray.size.intValue());

        Native.free(Pointer.nativeValue(byteArray.data));

        return data;
    }

    @SuppressWarnings({"PMD.MethodNamingConventions"})
    void fpng_init();

    @SuppressWarnings({"PMD.MethodNamingConventions"})
    ByteArray fpng_encode_image_to_memory(byte[] pImage, int w, int h, int num_chans, int flags);
}
