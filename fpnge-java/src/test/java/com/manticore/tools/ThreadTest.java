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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadTest {
    @Test
    @Disabled
    void testParallelEncoding() throws InterruptedException, IOException {
        BufferedImage image = Encoder.readImageFromClasspath(FPNGE.class, "example");
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 100000; i++) {
            final int j = i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    FPNGE.encode(image, 3, 1);
                    if (j % 1000 == 0) {
                        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
                        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
                        System.out.println(j + " images: Heap: "
                                + heapMemoryUsage.getUsed() / (1024 * 1024) + " | "
                                + heapMemoryUsage.getMax() / (1024 * 1024) + " Non-Heap: "
                                + nonHeapMemoryUsage.getUsed() / (1024 * 1024));
                    }
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);
    }
}
