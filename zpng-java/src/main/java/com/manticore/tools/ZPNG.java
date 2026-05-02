/**
 * FPNG-Java is a Java Wrapper around the fast SSE/AVX optimised FPNG encoders.
 * Copyright (C) 2026 Andreas Reichel <andreas@manticore-projects.com>
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
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * JNA wrapper for the zpng PNG encoder (zlib-ng backed, screen-content tuned).
 *
 * <p>
 * Mirrors the structure of {@link FPNGE} for symmetry. The native library is
 * {@code libzpng.{so,dylib,dll}} and is loaded once via {@link Encoder#load}.
 *
 * <p>
 * Compression levels are 0..9 (zlib-ng convention): 0 = stored, 1 = fastest, 9 = best compression.
 * Per the screen-content benchmark on the Webswing corpus, level 1 is optimal at >25 Mbit/s, level
 * 3 at 10-25 Mbit/s, level 4 for slow links (~1 Mbit/s).
 */
public interface ZPNG extends Encoder, EncoderBase {
    ZPNG ENCODER = (ZPNG) Encoder.load(ZPNG.class, "zpng");

    static byte[] encode(BufferedImage image, int numberOfChannels, int compLevel) {
        /*
         * num_chans must be 3 or 4. There must be w*3*h or w*4*h bytes pointed to by pImage. The
         * image row pitch is always w*3 or w*4 bytes. There is no automatic determination if the
         * image actually uses an alpha channel, so if you call it with 4 you will always get a
         * 32bpp PNG.
         *
         * encode1 expects byte order matching BufferedImage TYPE_4BYTE_ABGR / TYPE_3BYTE_BGR (what
         * getRGBABytes produces) and swaps to RGBA/RGB inside the native call. The buffer is
         * restored before encode1 returns, so callers can reuse it.
         */

        byte[] rgbaArray = EncoderBase.getRGBABytes(image, numberOfChannels);

        NativeLong bytesPerChannel = new NativeLong(1);
        NativeLong numChannels = new NativeLong(numberOfChannels);
        NativeLong width = new NativeLong(image.getWidth());
        NativeLong height = new NativeLong(image.getHeight());

        CharArray byteArray = ENCODER.encode1(bytesPerChannel, numChannels, rgbaArray,
                width, height, compLevel);

        byte[] data = byteArray.data.getByteArray(0, byteArray.size.intValue());
        Native.free(Pointer.nativeValue(byteArray.data));

        return data;
    }

    /**
     * Encodes a pre-arranged BGRA/BGR pixel buffer to PNG.
     *
     * @param bytesPerChannel must be 1 (only 8-bit channels supported here)
     * @param numChannels 3 or 4
     * @param pImage pixels in BufferedImage byte order (TYPE_4BYTE_ABGR / TYPE_3BYTE_BGR). Swapped
     *        in place to RGBA/RGB and restored before return.
     * @param width pixels
     * @param height pixels
     * @param compLevel zlib-ng compression level 0..9 (clamped)
     * @return CharArray with malloc'd encoded PNG bytes; caller must {@link Native#free}
     *         {@code data}.
     */
    @SuppressWarnings({"PMD.MethodNamingConventions"})
    CharArray encode1(NativeLong bytesPerChannel, NativeLong numChannels, byte[] pImage,
            NativeLong width, NativeLong height, int compLevel);

    /**
     * Mirrors zpng.cc's CharArray { unsigned char* data; size_t size; }.
     */
    class CharArray extends Structure {
        public Pointer data;
        public NativeLong size;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data", "size");
        }
    }
}
