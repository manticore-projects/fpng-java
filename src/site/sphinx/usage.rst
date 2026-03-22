******************************
How to use it
******************************

Compile from Source Code
==============================

You will need ``JDK 11`` or newer and a C/C++ toolchain (GCC or MSVC) installed.

.. code-block:: shell

    git clone https://github.com/manticore-projects/fpng-java.git
    cd fpng-java
    gradle build


Build Dependencies
==============================

**FPNG-Java** (SSE2 encoder, always safe to load):

.. tab:: Maven

    .. code-block:: xml
        :substitutions:

        <dependency>
            <groupId>com.manticore-projects.tools</groupId>
            <artifactId>fpng-java</artifactId>
            <version>|FPNG_ENCODER_VERSION|</version>
        </dependency>

.. tab:: Gradle

    .. code-block:: groovy
        :substitutions:

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation 'com.manticore-projects.tools:fpng-java:|FPNG_ENCODER_VERSION|'
        }

**FPNGE-Java** (AVX2 encoder, only load on supported hardware):

.. tab:: Maven

    .. code-block:: xml
        :substitutions:

        <dependency>
            <groupId>com.manticore-projects.tools</groupId>
            <artifactId>fpnge-java</artifactId>
            <version>|FPNG_ENCODER_VERSION|</version>
        </dependency>

.. tab:: Gradle

    .. code-block:: groovy
        :substitutions:

        repositories {
            mavenCentral()
        }

        dependencies {
            implementation 'com.manticore-projects.tools:fpnge-java:|FPNG_ENCODER_VERSION|'
        }


Java Usage
==============================

Basic Encoding
------------------------------

.. code-block:: java

    import com.manticore.tools.FPNGEncoder;
    import java.awt.image.BufferedImage;

    // Encode a BufferedImage to PNG bytes (3 or 4 channels)
    byte[] pngBytes = FPNGEncoder.encode(image, 4, 0);


Automatic Encoder Selection (SSE2 vs AVX2)
--------------------------------------------

The SSE2 encoder (FPNG) is always safe to load on any x86-64 CPU. The AVX2 encoder (FPNGE) must only be loaded if the CPU supports it — otherwise the native library will crash on load.

Use the ``hasAVX2()`` probe from the FPNG library to decide at runtime:

.. code-block:: java

    import com.manticore.tools.FPNGEncoder;
    import com.manticore.tools.FPNGEEncoder;

    // Initialise FPNG (always safe) and probe CPU features
    FPNGEncoder.ENCODER.fpng_init();
    boolean useAVX2 = FPNGEncoder.ENCODER.hasAVX2() != 0;

    // Encode using the best available encoder
    byte[] pngBytes;
    if (useAVX2) {
        pngBytes = FPNGEEncoder.encode(image, 4, FPNGE_COMPRESS_LEVEL_DEFAULT);
    } else {
        pngBytes = FPNGEncoder.encode(image, 4, 0);
    }


Extracting Raw Pixel Bytes
------------------------------

Both encoders accept the raw ``byte[]`` from Java's ``DataBufferByte`` directly. The C side handles the channel swap from Java's native ``ABGR``/``BGR`` format to ``RGBA``/``RGB``.

If your ``BufferedImage`` is already ``TYPE_4BYTE_ABGR`` or ``TYPE_3BYTE_BGR``, you can extract the backing array with zero copy:

.. code-block:: java

    import java.awt.image.DataBufferByte;

    byte[] raw = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

For images in other formats (e.g. ``TYPE_INT_ARGB``), convert first:

.. code-block:: java

    BufferedImage converted = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
    Graphics g = converted.getGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    byte[] raw = ((DataBufferByte) converted.getRaster().getDataBuffer()).getData();


Publishing to Maven Central
==============================

Credentials go in ``~/.gradle/gradle.properties``:

.. code-block:: properties

    sonatypeUsername=<your-central-portal-token-username>
    sonatypePassword=<your-central-portal-token-password>

Then publish:

.. code-block:: shell

    ./gradlew publish