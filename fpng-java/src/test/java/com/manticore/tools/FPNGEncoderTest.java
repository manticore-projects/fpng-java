package com.manticore.tools;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

class FPNGEncoderTest {

    @ParameterizedTest
    @CsvSource({
            "example, 3", "example, 4", "looklet-look-scale6, 3", "looklet-look-scale6, 4"
    })
    void encodeTest(String fileName, int channels) throws IOException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
        Encoder.encoderTest(FPNGEncoder.class, fileName, channels);
    }
}
