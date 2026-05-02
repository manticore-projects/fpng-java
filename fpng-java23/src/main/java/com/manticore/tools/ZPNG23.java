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

public class ZPNG23 implements EncoderBase {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final StructLayout CHAR_ARRAY_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("data"),
            ValueLayout.JAVA_LONG.withName("size")).withByteAlignment(8);

    private static final MethodHandle fpnge_encode1;
    private static final MethodHandle fpnge_encode2;
    static final MethodHandle native_free;

    private static final MethodHandle INT_ARGB_TO_RGBA;
    private static final MethodHandle INT_RGB_TO_RGBA;
    private static final MethodHandle INT_RGB_TO_RGB;
    private static final MethodHandle INT_BGR_TO_RGB;

    static {
        try {
            // 1. Dynamic Extraction Logic (Adapted from your Encoder.load)
            String libPathStr = EncoderBase.getLibraryFileName(ZPNG23.class, "zpng");

            // 2. FFM Library Mapping
            // We use Arena.global() so the library stays loaded for the JVM lifetime
            LOOKUP = SymbolLookup.libraryLookup(libPathStr, Arena.global());

            // 3. Linker Binding
            fpnge_encode1 = LINKER.downcallHandle(
                    LOOKUP.find("FPNGEEncode1").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, // ← pointer return
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT),
                    Linker.Option.critical(true));

            fpnge_encode2 = LINKER.downcallHandle(
                    LOOKUP.find("FPNGEEncode2").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, // ← pointer return
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_INT),
                    Linker.Option.critical(true));

            LOGGER.info("FPNGE AVX Encoder initialized via FFM from: " + libPathStr);

            // Most Linux systems provide 'free' in the default lookup,
            // but it's safer to look in the same library if it exports a wrapper,
            // or use the default native linker lookup.
            native_free = LINKER.downcallHandle(
                    LINKER.defaultLookup().find("free").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                    Linker.Option.critical(true));

            INT_ARGB_TO_RGBA = LINKER.downcallHandle(
                    LOOKUP.find("intArgbToRgba").orElseThrow(), // was: LINKER.defaultLookup()
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));

            INT_RGB_TO_RGBA = LINKER.downcallHandle(
                    LOOKUP.find("intRgbToRgba").orElseThrow(), // was: LINKER.defaultLookup()
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));

            INT_RGB_TO_RGB = LINKER.downcallHandle(
                    LOOKUP.find("intRgbToRgb").orElseThrow(), // was: LINKER.defaultLookup()
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));

            INT_BGR_TO_RGB = LINKER.downcallHandle(
                    LOOKUP.find("intBgrToRgb").orElseThrow(), // was: LINKER.defaultLookup()
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT));


        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize FFM Encoder", e);
        }
    }

    /**
     * Encodes image using FFM API and FPNGE AVX-optimized native code.
     */
    public static byte[] encode(BufferedImage image, int channels, int compLevel) {
        int width = image.getWidth();
        int height = image.getHeight();
        ImageSegment img = getRGBASegment(image, channels);
        MethodHandle encodeFn = img.isAlreadyRgba() ? fpnge_encode2 : fpnge_encode1;

        try {
            MemorySegment resultPointer = (MemorySegment) encodeFn.invokeExact(
                    1L, (long) channels, img.data(), (long) width, (long) height, compLevel);

            if (resultPointer.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Native encoding failed: null result");
            }

            MemorySegment structView = resultPointer.reinterpret(CHAR_ARRAY_LAYOUT.byteSize());

            MemorySegment dataPtr = structView.get(ValueLayout.ADDRESS, 0);
            long size = structView.get(ValueLayout.JAVA_LONG, 8);

            if (dataPtr.equals(MemorySegment.NULL) || size <= 0) {
                native_free.invokeExact(resultPointer);
                throw new RuntimeException("Native encoding failed: Invalid result.");
            }

            try {
                return dataPtr.reinterpret(size).toArray(ValueLayout.JAVA_BYTE);
            } finally {
                native_free.invokeExact(dataPtr); // free the pixel data
                native_free.invokeExact(resultPointer); // free the struct itself
            }
        } catch (Throwable t) {
            throw new RuntimeException("FFM AVX Encoding failed", t);
        }
    }

    public record ImageSegment(MemorySegment data, boolean isAlreadyRgba) {}

    public static ImageSegment getRGBASegment(BufferedImage image, int channels) {

        // Fast path 1: byte-packed ABGR/BGR — needs FPNGEEncode1 (swap inside).
        int targetType = channels == 4 ? BufferedImage.TYPE_4BYTE_ABGR
                : BufferedImage.TYPE_3BYTE_BGR;
        if (image.getType() == targetType) {
            byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            return new ImageSegment(MemorySegment.ofArray(data), false);
        }

        // Fast path 2: int-packed → fused C conversion produces RGBA/RGB → FPNGEEncode2.
        if (image.getRaster().getDataBuffer() instanceof DataBufferInt dbi) {
            int srcType = image.getType();
            int n = image.getWidth() * image.getHeight();
            MemorySegment src = MemorySegment.ofArray(dbi.getData());

            // Allocate output buffer; native off-heap to avoid the FFM copy.
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
                return new ImageSegment(dst, true); // already in RGBA/RGB layout — FPNGEEncode2
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        // Slow fallback: drawImage for everything else (indexed, gray, custom rasters).
        BufferedImage converted =
                new BufferedImage(image.getWidth(), image.getHeight(), targetType);
        Graphics g = converted.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        byte[] data = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        return new ImageSegment(MemorySegment.ofArray(data), false);
    }
}
