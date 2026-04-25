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

public class FPNGEncoder23 implements EncoderBase {

    // 1. FFM Infrastructure
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;

    // Define C struct: struct fpng_alloc_result { void* pData; uint32_t size; }
    // Note: FPNG uses uint32_t for size, which maps to JAVA_INT in FFM
    // Correct Layout for both fpng_alloc_result and ByteArray
    private static final StructLayout RESULT_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.ADDRESS.withName("pData"), // 8 bytes
            ValueLayout.JAVA_LONG.withName("size") // 8 bytes (size_t)
    ).withByteAlignment(8);

    private static final MethodHandle fpng_init;
    private static final MethodHandle fpng_encode_image_to_memory;
    static {
        try {
            // 1. Dynamic Extraction Logic (Adapted from your Encoder.load)
            String libPathStr = EncoderBase.getLibraryFileName(FPNGEncoder23.class, "fpng");

            // 2. FFM Library Mapping
            // We use Arena.global() so the library stays loaded for the JVM lifetime
            LOOKUP = SymbolLookup.libraryLookup(libPathStr, Arena.global());

            fpng_init = LINKER.downcallHandle(
                    LOOKUP.find("fpng_init").orElseThrow(),
                    FunctionDescriptor.ofVoid());

            // Signature: fpng_alloc_result fpng_encode_image_to_memory(void* pImage, uint32_t w,
            // uint32_t h, uint32_t num_chans, uint32_t flags)
            fpng_encode_image_to_memory = LINKER.downcallHandle(
                    LOOKUP.find("fpng_encode_image_to_memory").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, // ← pointer return, NOT struct
                            ValueLayout.ADDRESS, ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT),
                    Linker.Option.critical(true));
            fpng_init.invoke();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize FFM Encoder", e);
        }
    }

    /**
     * Encodes a BufferedImage to PNG bytes using the FFM API.
     */
    public static byte[] encode(BufferedImage image, int channels, int flags) {
        int width = image.getWidth();
        int height = image.getHeight();
        MemorySegment nativeImage = getRGBASegment(image, channels);

        try {
            MemorySegment resultPointer = (MemorySegment) fpng_encode_image_to_memory.invokeExact(
                    nativeImage, width, height, channels, flags);

            if (resultPointer.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Native encoding failed: null result");
            }

            // Reinterpret as our 16-byte struct {pointer, size_t}
            MemorySegment structView = resultPointer.reinterpret(RESULT_LAYOUT.byteSize());

            MemorySegment dataPtr = structView.get(ValueLayout.ADDRESS, 0);
            long size = structView.get(ValueLayout.JAVA_LONG, 8);

            if (dataPtr.equals(MemorySegment.NULL) || size <= 0) {
                FPNGE23.native_free.invokeExact(resultPointer);
                throw new RuntimeException("Native encoding failed: Invalid result.");
            }

            try {
                return dataPtr.reinterpret(size).toArray(ValueLayout.JAVA_BYTE);
            } finally {
                FPNGE23.native_free.invokeExact(dataPtr);
                FPNGE23.native_free.invokeExact(resultPointer);
            }
        } catch (Throwable t) {
            throw new RuntimeException("FFM Encoding failed", t);
        }
    }

    private static MemorySegment getRGBASegment(BufferedImage image, int channels) {
        int targetType = channels == 4 ? BufferedImage.TYPE_4BYTE_ABGR
                : BufferedImage.TYPE_3BYTE_BGR;

        // Fast path 1: already in the target byte-packed format. Zero copy —
        // the byte[] backing the raster is wrapped into a MemorySegment and
        // the C side reads it directly from the Java heap.
        if (image.getType() == targetType) {
            byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            return MemorySegment.ofArray(data);
        }

        // Fast path 2: int-packed source — convert directly via JIT-vectorizable
        // loops in EncoderBase. Avoids drawImage's MaskBlit/Blit overhead, which
        // the profiler showed accounting for ~30% of the 4-channel encode time
        // on large images.
        if (image.getRaster().getDataBuffer() instanceof DataBufferInt dbi) {
            int srcType = image.getType();
            byte[] data = null;

            if (channels == 4 && (srcType == BufferedImage.TYPE_INT_ARGB
                    || srcType == BufferedImage.TYPE_INT_RGB)) {
                data = EncoderBase.intToAbgrBytes(dbi.getData(),
                        srcType == BufferedImage.TYPE_INT_ARGB);
            } else if (channels == 3 && (srcType == BufferedImage.TYPE_INT_RGB
                    || srcType == BufferedImage.TYPE_INT_BGR)) {
                data = EncoderBase.intToBgrBytes(dbi.getData(),
                        srcType == BufferedImage.TYPE_INT_BGR);
            }

            if (data != null) {
                return MemorySegment.ofArray(data);
            }
            // else fall through to the drawImage path for unhandled int-packed combos
            // (e.g. 4-channel target from a TYPE_INT_BGR source)
        }

        // Slow fallback: drawImage handles indexed-color, grayscale, custom rasters,
        // and any int-packed combination not covered above. Triggers MaskBlit/Blit
        // inside Java2D — measurably slower than the fast paths but always correct.
        BufferedImage converted = new BufferedImage(
                image.getWidth(), image.getHeight(), targetType);
        Graphics g = converted.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        byte[] data = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
        return MemorySegment.ofArray(data);
    }
}
