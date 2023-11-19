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
import java.util.List;

abstract interface Encoder extends Library {

    // if we can't use the SSE or AVX byte shuffling, we could fall back to Java based byte swapping
    public static void swapIntBytes(byte[] bytes) {
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

    public static void encoderTest(Class<? extends Encoder> encoderClass, String fileName, int channels) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Load the PNG file into a BufferedImage
        try (
                InputStream inputStream = ClassLoader.getSystemResourceAsStream(fileName + ".png");
        ) {
            assert inputStream!=null;
            BufferedImage image = ImageIO.read(inputStream);

            Method encode = encoderClass.getMethod("encode", BufferedImage.class, int.class, int.class);
            byte[] data = (byte[]) encode.invoke(null, image, channels, 0);

            File file = File.createTempFile(  encoderClass.getSimpleName() + "_" + fileName + "_" + channels + "_", ".png");

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(data);
            fileOutputStream.flush();
            fileOutputStream.close();
        }
    }

    public static class ByteArray extends Structure {
        public Pointer data;
        public NativeLong size;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("data", "size");
        }
    }

    // copy a folder with all its content from inside the JAR to a filesystem destinantion (e.g. '/tmp/)
    static void extractFilesFromURI(URI uri, String target) throws IOException {
        Path uriPath = Paths.get(uri);
        File targetFolder = new File(target);
        targetFolder.deleteOnExit();

        Files.walkFileTree(uriPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, targetFolder.toPath().resolve( uriPath.relativize(file) ) , StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories( targetFolder.toPath().resolve( uriPath.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    static byte[] getRGBABytes(BufferedImage image, int channels) {
        BufferedImage convertedImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                channels == 4
                    ? BufferedImage.TYPE_4BYTE_ABGR
                    :  BufferedImage.TYPE_3BYTE_BGR
        );

        // Draw the original image onto the new image
        convertedImage.getGraphics().drawImage(image, 0, 0, null);
        DataBufferByte dataBuffer = (DataBufferByte) convertedImage.getRaster().getDataBuffer();
        return dataBuffer.getData();
    }

    static Object load(Class<? extends Library> clazz, String libraryName) {
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        String resourcePath = "/lib";
        String extension = ".so";
        String targetFolder = System.getProperty("java.io.tmpdir") + File.separator + clazz.getSimpleName() + File.separator;

        try {
            extractFilesFromURI(clazz.getResource("/lib").toURI(), targetFolder);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // linux/x86-64
        // linux/x86
        // windows/x86-64
        // macOS/x86-64
        if ( osName.equalsIgnoreCase("linux") ) {
            targetFolder += "linux";
        } else if ( osName.equalsIgnoreCase("windows") ) {
            targetFolder += "windows";
            extension = ".dll";
        } else if ( osName.equalsIgnoreCase("macos") ) {
            targetFolder += "macos";
            extension = ".dylib";
        }

        if ( osArch.contains("64") ) {
            targetFolder += File.separator + "x86-64";
        } else {
            targetFolder += File.separator + "x86-32";
        }

        return Native.load(targetFolder + File.separator + libraryName + extension, clazz);
    }

    public static byte[] encode(BufferedImage image, int numberOfChannels, int flags) {
        return null;
    };
}
