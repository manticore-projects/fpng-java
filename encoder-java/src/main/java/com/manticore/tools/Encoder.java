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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

interface Encoder extends Library {
    @Deprecated
    Logger LOGGER = EncoderBase.LOGGER;

    @Deprecated
    String TMP_FOLDER = EncoderBase.TMP_FOLDER;

    @Deprecated
    static void swapIntBytes(byte[] bytes) {
        EncoderBase.swapIntBytes(bytes);
    }

    @Deprecated
    static void encoderTest(Class<? extends EncoderBase> encoderClass, String fileName,
            int channels)
            throws IOException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        EncoderBase.encoderTest(encoderClass, fileName, channels, 5, true);
    }

    @Deprecated
    static void encoderTest(Class<? extends EncoderBase> encoderClass, String fileName,
            int channels,
            int compressLevel, boolean writeTempFiles) throws IOException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        EncoderBase.encoderTest(encoderClass, fileName, channels, compressLevel, writeTempFiles);
    }

    @Deprecated
    static void extractFilesFromURI(URI uri, String target) throws IOException {
        EncoderBase.extractFilesFromURI(uri, target);
    }

    @Deprecated
    static void extract(URI uri, String target) throws IOException {
        EncoderBase.extract(uri, target);
    }

    @Deprecated
    static BufferedImage readImageFromClasspath(Class<? extends EncoderBase> encoderClass,
            String fileName) throws IOException {
        return EncoderBase.readImageFromClasspath(encoderClass, fileName);
    }

    @Deprecated
    static byte[] getRGBABytes(BufferedImage image, int channels) {
        return EncoderBase.getRGBABytes(image, channels);
    }


    @SuppressWarnings({"PMD.NcssCount"})
    static Object load(Class<? extends Library> clazz, final String libraryName) {
        String libraryFileName = EncoderBase.getLibraryFileName(clazz, libraryName);
        return Native.load(libraryFileName, clazz);
    }

    @Deprecated
    static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        return EncoderBase.encode(image, numberOfChannels, flags);
    }

    class ByteArray extends Structure {
        public Pointer data;
        public NativeLong size;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data", "size");
        }
    }
}
