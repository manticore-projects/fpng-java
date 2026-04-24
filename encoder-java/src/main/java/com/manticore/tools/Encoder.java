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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

interface Encoder extends Library {
    @Deprecated
    Logger LOGGER = EncoderBase.LOGGER;

    String OS_NAME = System
            .getProperty("os.name")
            .toLowerCase(Locale.US)
            .replaceAll("[^a-z0-9]+", "");
    String OS_ARCH = System
            .getProperty("os.arch")
            .toLowerCase(Locale.US)
            .replaceAll("[^a-z0-9]+", "");

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
        String resourcePath = "/lib";
        String prefix = "lib";
        String extension = ".so";
        boolean strippedSymbols = true;
        String targetFolder = EncoderBase.TMP_FOLDER + File.separator + libraryName
                + File.separator;

        // clip the prefix
        // on Linux, MacOS: libfpng.*
        // on Windows: fpng.dll
        String strippedLibraryName = libraryName.startsWith(prefix)
                ? libraryName.substring(prefix.length())
                : libraryName;

        // linux/x86-64
        // linux/x86
        // windows/x86-64
        // macOS/x86-64
        String name = targetFolder;

        // credits to Copyright 2014 Trustin Heuiseung Lee.
        // taken from:
        // https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java
        if (OS_NAME.startsWith("linux")) {
            name += "linux";
        } else if (OS_NAME.startsWith("windows")) {
            name += "windows";
            prefix = "";
            extension = ".dll";
            strippedSymbols = false;
        } else if (OS_NAME.startsWith("mac") || OS_NAME.startsWith("osx")) {
            name += "macos";
            extension = ".dylib";

            // not supported or tested yet:
        } else if (OS_NAME.startsWith("solaris") || OS_NAME.startsWith("sunos")) {
            name += "sunos";
        } else if (OS_NAME.startsWith("aix")) {
            name += "aix";
            extension = ".a";
        } else if (OS_NAME.startsWith("hpux")) {
            name += "hpux";
            extension = ".sl";
        } else if (OS_NAME.startsWith("os400")
                && (OS_NAME.length() <= 5 || !Character.isDigit(OS_NAME.charAt(5)))) {
            // Avoid the names such as os4000
            name += "os400";
        } else if (OS_NAME.startsWith("freebsd")) {
            name += "freebsd";
        } else if (OS_NAME.startsWith("openbsd")) {
            name += "openbsd";
        } else if (OS_NAME.startsWith("netbsd")) {
            name += "netbsd";
        } else if (OS_NAME.startsWith("zos")) {
            name += "zos";
        }

        // credits to Copyright 2014 Trustin Heuiseung Lee.
        // taken from:
        // https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java
        if (OS_ARCH.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            name += File.separator + "x86-64";
        } else if (OS_ARCH.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            name += File.separator + "x86-32";

            // not supported or tested yet:
        } else if (OS_ARCH.matches("^(ia64w?|itanium64)$")) {
            name += File.separator + "itanium-64";
        } else if ("ia64n".equals(OS_ARCH)) {
            name += File.separator + "itanium-32";
        } else if (OS_ARCH.matches("^(sparc|sparc32)$")) {
            name += File.separator + "sparc-32";
        } else if (OS_ARCH.matches("^(sparcv9|sparc64)$")) {
            name += File.separator + "sparc-64";
        } else if (OS_ARCH.matches("^(arm|arm32)$")) {
            name += File.separator + "arm-32";
        } else if ("aarch64".equals(OS_ARCH)) {
            name += File.separator + "aarch-64";
        } else if (OS_ARCH.matches("^(mips|mips32)$")) {
            name += File.separator + "mips-32";
        } else if (OS_ARCH.matches("^(mipsel|mips32el)$")) {
            name += File.separator + "mipsel-32";
        } else if ("mips64".equals(OS_ARCH)) {
            name += File.separator + "mips-64";
        } else if ("mips64el".equals(OS_ARCH)) {
            name += File.separator + "mipsel-64";
        } else if (OS_ARCH.matches("^(ppc|ppc32)$")) {
            name += File.separator + "ppc-32";
        } else if (OS_ARCH.matches("^(ppcle|ppc32le)$")) {
            name += File.separator + "ppcle-32";
        } else if ("ppc64".equals(OS_ARCH)) {
            name += File.separator + "ppc-64";
        } else if ("ppc64le".equals(OS_ARCH)) {
            name += File.separator + "ppcle-64";
        } else if ("s390".equals(OS_ARCH)) {
            name += File.separator + "s390-32";
        } else if ("s390x".equals(OS_ARCH)) {
            name += File.separator + "s390-64";
        } else if (OS_ARCH.matches("^(riscv|riscv32)$")) {
            name += File.separator + "riscv";
        } else if ("riscv64".equals(OS_ARCH)) {
            name += File.separator + "riscv64";
        } else if ("e2k".equals(OS_ARCH)) {
            name += File.separator + "e2k";
        } else if ("loongarch64".equals(OS_ARCH)) {
            name += File.separator + "loongarch-64";
        }

        name += File.separator + (strippedSymbols ? "stripped" + File.separator : "")
                + prefix
                + strippedLibraryName
                + extension;
        if (new File(name).isFile()) {
            LOGGER.info("Load native library from " + name);
        } else {
            LOGGER.info("Extract and Load native library from " + name);
            URL resource = clazz.getResource(resourcePath + "/" + strippedLibraryName);
            if (resource != null) {
                try {
                    extractFilesFromURI(resource.toURI(), targetFolder);
                } catch (IOException | URISyntaxException ex) {
                    throw new RuntimeException("Failed to extract " + resource + " from "
                            + clazz.getCanonicalName() + " to " + targetFolder, ex);
                }
            } else {
                throw new RuntimeException("Resource " + resourcePath + "/" + strippedLibraryName
                        + " does not exist in " + clazz.getCanonicalName()
                        + "\nLikely unsupported Architecture: " + OS_NAME + " " + OS_ARCH);
            }
        }

        return Native.load(name, clazz);
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
