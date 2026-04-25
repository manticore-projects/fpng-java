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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public interface EncoderBase {
    Logger LOGGER = Logger.getLogger(EncoderBase.class.getName());
    String TMP_FOLDER = System.getProperty("java.io.tmpdir");

    /**
     * Reverse the byte order of every 4-byte group, in place. Equivalent to ABGR &lt;-&gt; RGBA
     * conversion. Used as a Java-side fallback when the native SSE/AVX shuffler is unavailable.
     *
     * Loop body has no internal dependency, so HotSpot's auto-vectorizer will collapse it to
     * vpshufb-based SIMD on AVX2 capable CPUs.
     */
    static void swapIntBytes(byte[] bytes) {
        assert bytes.length % 4 == 0;
        for (int i = 0; i < bytes.length; i += 4) {
            byte b0 = bytes[i];
            byte b1 = bytes[i + 1];
            byte b2 = bytes[i + 2];
            byte b3 = bytes[i + 3];
            bytes[i] = b3;
            bytes[i + 1] = b2;
            bytes[i + 2] = b1;
            bytes[i + 3] = b0;
        }
    }

    /**
     * Convert TYPE_INT_ARGB (0xAARRGGBB) or TYPE_INT_RGB (0x00RRGGBB) to TYPE_4BYTE_ABGR memory
     * layout (A,B,G,R per pixel).
     */
    static byte[] intToAbgrBytes(int[] src, boolean hasAlpha) {
        byte[] out = new byte[src.length * 4];
        for (int i = 0; i < src.length; i++) {
            int p = src[i];
            int o = i * 4;
            out[o] = hasAlpha ? (byte) (p >>> 24) : (byte) 0xFF; // A
            out[o + 1] = (byte) p; // B (low byte)
            out[o + 2] = (byte) (p >>> 8); // G
            out[o + 3] = (byte) (p >>> 16); // R (high byte of RGB)
        }
        return out;
    }

    /**
     * Convert TYPE_INT_RGB (0x00RRGGBB) or TYPE_INT_BGR (0x00BBGGRR) to TYPE_3BYTE_BGR memory
     * layout (B,G,R per pixel).
     */
    static byte[] intToBgrBytes(int[] src, boolean isBgr) {
        byte[] out = new byte[src.length * 3];
        if (isBgr) {
            // 0x00BBGGRR: B is high, R is low
            for (int i = 0; i < src.length; i++) {
                int p = src[i];
                int o = i * 3;
                out[o] = (byte) (p >>> 16); // B
                out[o + 1] = (byte) (p >>> 8); // G
                out[o + 2] = (byte) p; // R
            }
        } else {
            // 0x00RRGGBB: R is high, B is low
            for (int i = 0; i < src.length; i++) {
                int p = src[i];
                int o = i * 3;
                out[o] = (byte) p; // B
                out[o + 1] = (byte) (p >>> 8); // G
                out[o + 2] = (byte) (p >>> 16); // R
            }
        }
        return out;
    }

    static void encoderTest(Class<? extends EncoderBase> encoderClass, String fileName,
            int channels)
            throws IOException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        encoderTest(encoderClass, fileName, channels, 5,
                Boolean.getBoolean("encoder.test.writeTempFiles"));
    }

    static void encoderTest(Class<? extends EncoderBase> encoderClass, String fileName,
            int channels,
            int compressLevel, boolean writeTempFiles) throws IOException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        BufferedImage image = readImageFromClasspath(encoderClass, fileName);

        Method encode = encoderClass.getMethod("encode", BufferedImage.class, int.class, int.class);
        byte[] data = (byte[]) encode.invoke(null, image, channels, compressLevel);

        if (writeTempFiles) {
            String name = encoderClass.getSimpleName() + "_" + fileName + "_" + channels + ".png";
            File file = new File(System.getProperty("java.io.tmpdir"), name);
            try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                fileOutputStream.write(data);
            }
        }
    }

    // copy a folder with all its content from inside the JAR to a filesystem destination (e.g.
    // '/tmp/)
    static void extractFilesFromURI(URI uri, String target) throws IOException {
        LOGGER.info("Extract native libraries from: " + uri.toASCIIString());

        // see:
        // https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
        // create the file system before we can access the path within the zip
        // this will fail when not having a zip
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        try (FileSystem ignored = FileSystems.newFileSystem(uri, env)) {
            extract(uri, target);
        } catch (Exception ex) {
            extract(uri, target);
        }
    }

    static void extract(URI uri, String target) throws IOException {
        Path uriPath = Paths.get(uri);
        File targetFolder = new File(target);
        targetFolder.deleteOnExit();

        Files.walkFileTree(uriPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {

                        Files.copy(file,
                                targetFolder.toPath().resolve(uriPath.relativize(file).toString()),
                                StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc)
                            throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                            throws IOException {
                        Files.createDirectories(
                                targetFolder.toPath().resolve(uriPath.relativize(dir).toString()));
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    static BufferedImage readImageFromClasspath(Class<? extends EncoderBase> encoderClass,
            String fileName) throws IOException {

        String fileNameWithExtension = fileName + ".png";
        String resourceStr = "/" + fileNameWithExtension;
        File destinationFile = new File(TMP_FOLDER, fileNameWithExtension);
        destinationFile.deleteOnExit();

        try (InputStream is = encoderClass.getResourceAsStream(resourceStr);) {
            if (is != null) {
                Files.copy(is, destinationFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new IOException("Could not read image " + resourceStr + " from Class "
                        + encoderClass.getCanonicalName());
            }

            // Load the PNG file into a BufferedImage
            File file = new File(TMP_FOLDER, fileNameWithExtension);
            if (file.isFile() && file.canRead()) {
                file.deleteOnExit();
                return ImageIO.read(file);
            } else {
                throw new IOException(
                        "Could not read image " + fileNameWithExtension
                                + " from TEMP after extract.");
            }
        }
    }

    static byte[] getRGBABytes(BufferedImage image, int channels) {
        int targetType = channels == 4 ? BufferedImage.TYPE_4BYTE_ABGR
                : BufferedImage.TYPE_3BYTE_BGR;

        // Fast path 1: already in target byte-packed format. Zero copy.
        DataBuffer dataBuffer = image.getRaster().getDataBuffer();
        if (image.getType() == targetType) {
            return ((DataBufferByte) dataBuffer).getData();
        }

        // Fast path 2: int-packed source. JIT-vectorizable conversion to byte-packed
        // ABGR/BGR — same byte order the C side gets from a TYPE_4BYTE_ABGR / TYPE_3BYTE_BGR
        // raster, so the existing C SSE/AVX swap pipeline works unchanged.
        if (dataBuffer instanceof DataBufferInt) {
            DataBufferInt dbi = (DataBufferInt) dataBuffer;
            int srcType = image.getType();
            if (channels == 4 && (srcType == BufferedImage.TYPE_INT_ARGB
                    || srcType == BufferedImage.TYPE_INT_RGB)) {
                return intToAbgrBytes(dbi.getData(), srcType == BufferedImage.TYPE_INT_ARGB);
            }
            if (channels == 3 && (srcType == BufferedImage.TYPE_INT_RGB
                    || srcType == BufferedImage.TYPE_INT_BGR)) {
                return intToBgrBytes(dbi.getData(), srcType == BufferedImage.TYPE_INT_BGR);
            }
        }

        // Slow fallback: drawImage for indexed, grayscale, custom rasters.
        BufferedImage converted =
                new BufferedImage(image.getWidth(), image.getHeight(), targetType);
        Graphics g = converted.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();
    }

    @SuppressWarnings({"PMD.NcssCount"})
    static String getLibraryFileName(Class<?> clazz, final String libraryName) {
        String OS_NAME = System
                .getProperty("os.name")
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "");
        String OS_ARCH = System
                .getProperty("os.arch")
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "");

        String resourcePath = "/lib";
        String prefix = "lib";
        String extension = ".so";
        boolean strippedSymbols = true;
        String targetFolder = TMP_FOLDER.endsWith(File.separator)
                ? TMP_FOLDER + libraryName + File.separator
                : TMP_FOLDER + File.separator + libraryName + File.separator;

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
                && (OS_NAME.length() == 5 || !Character.isDigit(OS_NAME.charAt(5)))) {
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
        return name;
    }

    static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        return null;
    }
}
