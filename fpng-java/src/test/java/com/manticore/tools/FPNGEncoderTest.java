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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

class FPNGEncoderTest {

    @ParameterizedTest
    @CsvSource({
            "example, 3", "example, 4", "looklet-look-scale6, 3", "looklet-look-scale6, 4",
            "failure16044445656400295224, 3", "failure16044445656400295224, 4",
            "stsci-01h44ay5ztcv1npb227b2p650j, 3", "stsci-01h44ay5ztcv1npb227b2p650j, 4"
    })
    void encodeTest(String fileName, int channels) throws IOException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
        Encoder.encoderTest(FPNGEncoder.class, fileName, channels);
    }

    @ParameterizedTest
    @CsvSource({
            "failure16044445656400295224, 4"
    })
    void encodeTestFailure(String fileName, int channels)
            throws IOException, InvocationTargetException,
            NoSuchMethodException, IllegalAccessException {
        Encoder.encoderTest(FPNGEncoder.class, fileName, channels);
    }
}
