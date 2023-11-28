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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

interface Encoder extends Library {
    Logger LOGGER = Logger.getLogger(Encoder.class.getName());
    String OS_NAME = System
            .getProperty("os.name")
            .toLowerCase(Locale.US)
            .replaceAll("[^a-z0-9]+", "");
    String OS_ARCH = System
            .getProperty("os.arch")
            .toLowerCase(Locale.US)
            .replaceAll("[^a-z0-9]+", "");

    String TMP_FOLDER = System.getProperty("java.io.tmpdir");

    // if we can't use the SSE or AVX byte shuffling, we could fall back to Java based byte swapping
    static void swapIntBytes(byte[] bytes) {
        assert bytes.length % 4 == 0;
        for (int i = 0; i < bytes.length; i += 4) {
            // swap 0 and 3
            byte tmp = bytes[i];
            bytes[i] = bytes[i + 3];
            bytes[i + 3] = tmp;
            // swap 1 and 2
            byte tmp2 = bytes[i + 1];
            bytes[i + 1] = bytes[i + 2];
            bytes[i + 2] = tmp2;
        }
    }


    static void encoderTest(Class<? extends Encoder> encoderClass, String fileName, int channels)
            throws IOException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        encoderTest(encoderClass, fileName, channels, 5, true);
    }

    static void encoderTest(Class<? extends Encoder> encoderClass, String fileName, int channels,
            int compressLevel, boolean writeTempFiles) throws IOException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
        BufferedImage image = readImageFromClasspath(encoderClass, fileName);

        Method encode = encoderClass.getMethod("encode", BufferedImage.class, int.class, int.class);
        byte[] data = (byte[]) encode.invoke(null, image, channels, compressLevel);

        if (writeTempFiles) {
            File file = File.createTempFile(
                    encoderClass.getSimpleName() + "_" + fileName + "_" + channels + "_", ".png");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.flush();
            fileOutputStream.close();
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

    static BufferedImage readImageFromClasspath(Class<? extends Encoder> encoderClass,
            String fileName) throws IOException {

        // We should be able to read the Image from a Class InputStream directly.
        // This has been working well on Windows and Linux.
        // Although it continuously failed on MacOS looking like a specific JDK/OS problem.
        // Now we copy into a file first trying to mitigate this issue.

        String resourceStr = "/" + fileName + ".png";
        File destinationFile = new File(TMP_FOLDER, fileName + ".png");
        destinationFile.deleteOnExit();

        try (InputStream is = encoderClass.getResourceAsStream("/" + fileName + ".png");) {
            if (is != null) {
                Files.copy(is, destinationFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } else {
                throw new IOException("Could not read image " + fileName + " from Class "
                        + encoderClass.getCanonicalName());
            }

            // Load the PNG file into a BufferedImage
            File file = new File(TMP_FOLDER, fileName + ".png");
            if (file.isFile() && file.canRead()) {
                file.deleteOnExit();
                return ImageIO.read(file);
            } else {
                throw new IOException(
                        "Could not read image " + fileName + " from TEMP after extract.");
            }
        }
    }

    static byte[] getRGBABytes(BufferedImage image, int channels) {
        BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(),
                channels == 4 ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);

        // Draw the original image onto the new image
        convertedImage.getGraphics().drawImage(image, 0, 0, null);
        DataBufferByte dataBuffer = (DataBufferByte) convertedImage.getRaster().getDataBuffer();
        return dataBuffer.getData();
    }

    static Object load(Class<? extends Library> clazz, final String libraryName) {
        String resourcePath = "/lib";
        String prefix = "lib";
        String extension = ".so";
        String targetFolder = TMP_FOLDER + File.separator + libraryName
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

        name += File.separator + "stripped" + File.separator + prefix + strippedLibraryName
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
                        + " does not exist in " + clazz.getCanonicalName());
            }
        }

        return Native.load(name, clazz);
    }

    static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        return null;
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
