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
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class FPNGE23 implements EncoderBase {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    private static final StructLayout CHAR_ARRAY_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("data"),
            ValueLayout.JAVA_LONG.withName("size")).withByteAlignment(8);

    private static final MethodHandle fpnge_encode1;
    static final MethodHandle native_free;

    static {
        try {
            // 1. Dynamic Extraction Logic (Adapted from your Encoder.load)
            String libPathStr = EncoderBase.getLibraryFileName(FPNGE23.class, "fpnge");

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

            LOGGER.info("FPNGE AVX Encoder initialized via FFM from: " + libPathStr);

            // Most Linux systems provide 'free' in the default lookup,
            // but it's safer to look in the same library if it exports a wrapper,
            // or use the default native linker lookup.
            native_free = LINKER.downcallHandle(
                    LINKER.defaultLookup().find("free").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

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
        MemorySegment nativeImage = getRGBASegment(image, channels);

        try {
            MemorySegment resultPointer = (MemorySegment) fpnge_encode1.invokeExact(
                    1L,
                    (long) channels,
                    nativeImage,
                    (long) width,
                    (long) height,
                    compLevel);

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

    public static MemorySegment getRGBASegment(BufferedImage image, int channels) {
        int type = channels == 4 ? BufferedImage.TYPE_4BYTE_ABGR
                : BufferedImage.TYPE_3BYTE_BGR;

        BufferedImage convertedImage = image;
        if (image.getType() != type) {
            convertedImage = new BufferedImage(image.getWidth(), image.getHeight(), type);
            Graphics g = convertedImage.getGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
        }

        byte[] data = ((DataBufferByte) convertedImage.getRaster().getDataBuffer()).getData();

        // This wraps the Java array into a MemorySegment WITHOUT copying.
        // The C code will read directly from the Java Heap.
        return MemorySegment.ofArray(data);
    }
}
