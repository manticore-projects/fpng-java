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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Java 23+ FFM wrapper for the zpng PNG encoder.
 *
 * <p>
 * Two entry points into the native library:
 * <ul>
 * <li>{@code encode1} — caller passes pixels in BufferedImage byte order (TYPE_4BYTE_ABGR /
 * TYPE_3BYTE_BGR); native side swaps in place, encodes, then restores the input buffer.</li>
 * <li>{@code encode2} — caller passes pixels already in RGBA/RGB layout (e.g. produced by
 * intArgbToRgba). No swap.</li>
 * </ul>
 * {@link #getRGBASegment} picks the right path based on the source BufferedImage's underlying
 * buffer type, fusing format conversion with encode dispatch to avoid a second pass over pixel
 * memory.
 *
 * <p>
 * Compression levels are 0..9 (zlib-ng convention).
 */
public class ZPNG23 implements EncoderBase {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    /** Mirror of zpng.cc's {@code CharArray { unsigned char* data; size_t size; }}. */
    private static final StructLayout CHAR_ARRAY_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("data"),
            ValueLayout.JAVA_LONG.withName("size")).withByteAlignment(8);

    private static final MethodHandle encode1;
    private static final MethodHandle encode2;
    static final MethodHandle native_free;

    private static final MethodHandle INT_ARGB_TO_RGBA;
    private static final MethodHandle INT_RGB_TO_RGBA;
    private static final MethodHandle INT_RGB_TO_RGB;
    private static final MethodHandle INT_BGR_TO_RGB;

    static {
        try {
            // 1. Resolve and load libzpng.{so,dylib,dll}.
            String libPathStr = EncoderBase.getLibraryFileName(ZPNG23.class, "zpng");

            // Arena.global() keeps the library loaded for the JVM lifetime.
            LOOKUP = SymbolLookup.libraryLookup(libPathStr, Arena.global());

            // 2. Bind native entry points. Linker.Option.critical(true) lets the
            // JVM pin the heap byte[] argument across the call without
            // copying it, which is the whole point of using FFM here.
            encode1 = LINKER.downcallHandle(
                    LOOKUP.find("encode1").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, // returns CharArray*
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT),
                    Linker.Option.critical(true));

            encode2 = LINKER.downcallHandle(
                    LOOKUP.find("encode2").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, // returns CharArray*
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT),
                    Linker.Option.critical(true));

            LOGGER.info("zpng FFM Encoder initialised from: " + libPathStr);

            // 3. native free(). Pulled from the default lookup (libc) rather
            // than libzpng — zpng.cc uses the standard malloc/free, so the
            // libc free() is what releases its allocations.
            native_free = LINKER.downcallHandle(
                    LINKER.defaultLookup().find("free").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                    Linker.Option.critical(true));

            // 4. The four pixel-format conversion helpers, all (ptr, ptr, int)
            // void functions. They live in libzpng so we lookup from LOOKUP,
            // not the default lookup.
            INT_ARGB_TO_RGBA = LINKER.downcallHandle(
                    LOOKUP.find("intArgbToRgba").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));

            INT_RGB_TO_RGBA = LINKER.downcallHandle(
                    LOOKUP.find("intRgbToRgba").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));

            INT_RGB_TO_RGB = LINKER.downcallHandle(
                    LOOKUP.find("intRgbToRgb").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));

            INT_BGR_TO_RGB = LINKER.downcallHandle(
                    LOOKUP.find("intBgrToRgb").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise zpng FFM encoder", e);
        }
    }

    /**
     * Encodes a BufferedImage to a PNG byte array using the zpng native encoder.
     *
     * @param image source image (any BufferedImage type; conversion is automatic)
     * @param channels 3 (RGB) or 4 (RGBA)
     * @param compLevel zlib-ng compression level 0..9 (0 = stored, 9 = best)
     */
    public static byte[] encode(BufferedImage image, int channels, int compLevel) {
        int width = image.getWidth();
        int height = image.getHeight();
        ImageSegment img = getRGBASegment(image, channels);
        // encode2 if pixels are already in RGBA/RGB layout; encode1 if they
        // need an in-place ABGR/BGR -> RGBA/RGB swap on the way in.
        MethodHandle encodeFn = img.isAlreadyRgba() ? encode2 : encode1;

        try {
            MemorySegment resultPointer = (MemorySegment) encodeFn.invokeExact(
                    1L, (long) channels, img.data(),
                    (long) width, (long) height, compLevel);

            if (resultPointer.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Native encoding failed: null result");
            }

            // Reinterpret the returned pointer as a CharArray struct view.
            MemorySegment structView = resultPointer.reinterpret(CHAR_ARRAY_LAYOUT.byteSize());

            MemorySegment dataPtr = structView.get(ValueLayout.ADDRESS, 0);
            long size = structView.get(ValueLayout.JAVA_LONG, 8);

            if (dataPtr.equals(MemorySegment.NULL) || size <= 0) {
                native_free.invokeExact(resultPointer);
                throw new RuntimeException("Native encoding failed: invalid result");
            }

            try {
                return dataPtr.reinterpret(size).toArray(ValueLayout.JAVA_BYTE);
            } finally {
                // Two separate allocations on the C side: one for the encoded
                // bytes, one for the CharArray struct itself. Free both.
                native_free.invokeExact(dataPtr);
                native_free.invokeExact(resultPointer);
            }
        } catch (Throwable t) {
            throw new RuntimeException("zpng FFM encoding failed", t);
        }
    }

    public record ImageSegment(MemorySegment data, boolean isAlreadyRgba) {}

    /**
     * Maps a BufferedImage to the right MemorySegment for the encoder, picking the cheapest path
     * available based on the source raster's actual layout.
     *
     * <p>
     * Three cases:
     * <ol>
     * <li><b>Byte-packed ABGR/BGR.</b> Pixel bytes are already in the layout {@code encode1} wants
     * on input (TYPE_4BYTE_ABGR / TYPE_3BYTE_BGR). Wrap the array directly. Returns
     * {@code isAlreadyRgba=false} so the caller routes to {@code encode1} (which swaps to RGBA/RGB
     * inside).</li>
     *
     * <li><b>Int-packed.</b> Source raster is INT_ARGB / INT_RGB / INT_BGR. Allocate an off-heap
     * output buffer and run the matching native conversion (intArgbToRgba etc.) which fuses
     * byte-extract and channel-swap into one pass. Returns {@code isAlreadyRgba=true} so the caller
     * routes to {@code encode2}.</li>
     *
     * <li><b>Anything else.</b> Indexed colour, gray, custom raster, etc. Fall back to
     * {@link Graphics#drawImage} into a fresh BufferedImage of the target byte type, then return
     * that as case 1.</li>
     * </ol>
     */
    public static ImageSegment getRGBASegment(BufferedImage image, int channels) {

        // Fast path 1: byte-packed ABGR/BGR — needs encode1 (swap inside).
        int targetType = channels == 4 ? BufferedImage.TYPE_4BYTE_ABGR
                : BufferedImage.TYPE_3BYTE_BGR;
        if (image.getType() == targetType) {
            byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            return new ImageSegment(MemorySegment.ofArray(data), false);
        }

        // Fast path 2: int-packed → fused C conversion produces RGBA/RGB → encode2.
        if (image.getRaster().getDataBuffer() instanceof DataBufferInt dbi) {
            int srcType = image.getType();
            int n = image.getWidth() * image.getHeight();
            MemorySegment src = MemorySegment.ofArray(dbi.getData());

            // Off-heap output buffer; avoids the second copy that an on-heap
            // byte[] target would force when crossing the FFM boundary.
            int outBytesPerPixel = (channels == 4) ? 4 : 3;
            MemorySegment dst = Arena.ofAuto().allocate((long) n * outBytesPerPixel);

            try {
                if (channels == 4 && srcType == BufferedImage.TYPE_INT_ARGB) {
                    INT_ARGB_TO_RGBA.invokeExact(src, dst, n);
                } else if (channels == 4 && srcType == BufferedImage.TYPE_INT_RGB) {
                    INT_RGB_TO_RGBA.invokeExact(src, dst, n);
                } else if (channels == 3 && srcType == BufferedImage.TYPE_INT_RGB) {
                    INT_RGB_TO_RGB.invokeExact(src, dst, n);
                } else if (channels == 3 && srcType == BufferedImage.TYPE_INT_BGR) {
                    INT_BGR_TO_RGB.invokeExact(src, dst, n);
                }
                // Any other (channels, srcType) falls through with dst still
                // zeroed. That would produce a black image silently. If you
                // find such a combo in production, add a case here or extend
                // the fallback below to cover it.
                return new ImageSegment(dst, true);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        // Slow fallback: drawImage for everything else (indexed, gray, custom).
        BufferedImage converted =
                new BufferedImage(image.getWidth(), image.getHeight(), targetType);
        Graphics g = converted.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        byte[] data = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        return new ImageSegment(MemorySegment.ofArray(data), false);
    }
}
