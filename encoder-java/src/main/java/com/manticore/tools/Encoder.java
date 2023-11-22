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

abstract interface Encoder extends Library {
    public final static Logger LOGGER = Logger.getLogger(Encoder.class.getName());
    public final static String osName = System.getProperty("os.name").toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    public final static String osArch = System.getProperty("os.arch").toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");

    // if we can't use the SSE or AVX byte shuffling, we could fall back to Java based byte swapping
    public static void swapIntBytes(byte[] bytes) {
        assert bytes.length % 4==0;
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

    public static void encoderTest(Class<? extends Encoder> encoderClass, String fileName, int channels) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Load the PNG file into a BufferedImage
        try (
                InputStream inputStream = ClassLoader.getSystemResourceAsStream(fileName + ".png");
        ) {
            assert inputStream!=null;
            BufferedImage image = ImageIO.read(inputStream);

            Method encode = encoderClass.getMethod("encode", BufferedImage.class, int.class, int.class);
            byte[] data = (byte[]) encode.invoke(null, image, channels, 0);

            File file = File.createTempFile(encoderClass.getSimpleName() + "_" + fileName + "_" + channels + "_", ".png");

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.flush();
            fileOutputStream.close();
        }
    }

    // copy a folder with all its content from inside the JAR to a filesystem destinantion (e.g. '/tmp/)
    static void extractFilesFromURI(URI uri, String target) throws IOException {
        LOGGER.info("Extract native libraries from: " + uri.toASCIIString());

        // see: https://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html
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

        Files.walkFileTree(uriPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, targetFolder.toPath().resolve(uriPath.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(targetFolder.toPath().resolve(uriPath.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static byte[] getRGBABytes(BufferedImage image, int channels) {
        BufferedImage convertedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                channels==4
                ? BufferedImage.TYPE_4BYTE_ABGR
                :BufferedImage.TYPE_3BYTE_BGR
        );

        // Draw the original image onto the new image
        convertedImage.getGraphics().drawImage(image, 0, 0, null);
        DataBufferByte dataBuffer = (DataBufferByte) convertedImage.getRaster().getDataBuffer();
        return dataBuffer.getData();
    }

    static Object load(Class<? extends Library> clazz, String libraryName) {
        String resourcePath = "/lib";
        String prefix = "lib";
        String extension = ".so";
        String targetFolder = System.getProperty("java.io.tmpdir") + File.separator + libraryName + File.separator;

        // clip the prefix
        // on Linux, MacOS: libfpng.*
        // on Windows: fpng.dll
        if (libraryName.startsWith(prefix)) {
            libraryName = libraryName.substring(prefix.length());
            LOGGER.fine("Clipping prefix and setting library name = '" + libraryName + '"');
        }

        // linux/x86-64
        // linux/x86
        // windows/x86-64
        // macOS/x86-64
        String name = targetFolder;

        // credits to Copyright 2014 Trustin Heuiseung Lee.
        // taken from: https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java
        if (osName.startsWith("linux")) {
            name += "linux";
        } else if (osName.startsWith("windows")) {
            name += "windows";
            prefix = "";
            extension = ".dll";
        } else if (osName.startsWith("mac") || osName.startsWith("osx")) {
            name += "macos";
            extension = ".dylib";

            // not supported or tested yet:
        } else if (osName.startsWith("solaris") || osName.startsWith("sunos")) {
            name += "sunos";
        } else if (osName.startsWith("aix")) {
            name += "aix";
            extension = ".a";
        } else if (osName.startsWith("hpux")) {
            name += "hpux";
            extension = ".sl";
        } else if (osName.startsWith("os400") && (osName.length() <= 5 || !Character.isDigit(osName.charAt(5)))) {
            // Avoid the names such as os4000
            name += "os400";
        } else if (osName.startsWith("freebsd")) {
            name += "freebsd";
        } else if (osName.startsWith("openbsd")) {
            name += "openbsd";
        } else if (osName.startsWith("netbsd")) {
            name += "netbsd";
        } else if (osName.startsWith("zos")) {
            name += "zos";
        }

        // credits to Copyright 2014 Trustin Heuiseung Lee.
        // taken from: https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java
        if (osArch.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            name += File.separator + "x86-64";
        } else if (osArch.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            name += File.separator + "x86-32";

            // not supported or tested yet:
        } else if (osArch.matches("^(ia64w?|itanium64)$")) {
            name += File.separator + "itanium-64";
        } else if ("ia64n".equals(osArch)) {
            name += File.separator + "itanium-32";
        } else if (osArch.matches("^(sparc|sparc32)$")) {
            name += File.separator + "sparc-32";
        } else if (osArch.matches("^(sparcv9|sparc64)$")) {
            name += File.separator + "sparc-64";
        } else if (osArch.matches("^(arm|arm32)$")) {
            name += File.separator + "arm-32";
        } else if ("aarch64".equals(osArch)) {
            name += File.separator + "aarch-64";
        } else if (osArch.matches("^(mips|mips32)$")) {
            name += File.separator + "mips-32";
        } else if (osArch.matches("^(mipsel|mips32el)$")) {
            name += File.separator + "mipsel-32";
        } else if ("mips64".equals(osArch)) {
            name += File.separator + "mips-64";
        } else if ("mips64el".equals(osArch)) {
            name += File.separator + "mipsel-64";
        } else if (osArch.matches("^(ppc|ppc32)$")) {
            name += File.separator + "ppc-32";
        } else if (osArch.matches("^(ppcle|ppc32le)$")) {
            name += File.separator + "ppcle-32";
        } else if ("ppc64".equals(osArch)) {
            name += File.separator + "ppc-64";
        } else if ("ppc64le".equals(osArch)) {
            name += File.separator + "ppcle-64";
        } else if ("s390".equals(osArch)) {
            name += File.separator + "s390-32";
        } else if ("s390x".equals(osArch)) {
            name += File.separator + "s390-64";
        } else if (osArch.matches("^(riscv|riscv32)$")) {
            name += File.separator + "riscv";
        } else if ("riscv64".equals(osArch)) {
            name += File.separator + "riscv64";
        } else if ("e2k".equals(osArch)) {
            name += File.separator + "e2k";
        } else if ("loongarch64".equals(osArch)) {
            name += File.separator + "loongarch-64";
        }

        name += File.separator + prefix + libraryName + extension;
        if (new File(name).isFile()) {
            LOGGER.fine("Load native library from " + name);
        } else {
            LOGGER.fine("Extract and Load native library from " + name);
            URL resource = clazz.getResource(resourcePath + "/" + libraryName);
            if (resource!=null) {
                try {
                    extractFilesFromURI(resource.toURI(), targetFolder);
                } catch (IOException | URISyntaxException ex) {
                    throw new RuntimeException("Failed to extract " + resource + " from " + clazz.getCanonicalName() + " to " + targetFolder, ex);
                }
            } else {
                throw new RuntimeException("Resource " + resourcePath + "/" + libraryName + " does not exist in " + clazz.getCanonicalName());
            }
        }

        return Native.load(name, clazz);
    }

    public static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        return null;
    }

    public static class ByteArray extends Structure {
        public Pointer data;
        public NativeLong size;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data", "size");
        }
    }
}
