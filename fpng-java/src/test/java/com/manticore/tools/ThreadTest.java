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
        BufferedImage image = Encoder.readImageFromClasspath(FPNGEncoder.class, "example");

        ExecutorService executorService = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 100000; i++) {
            final int j = i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    FPNGEncoder.encode(image, 3, 1);
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
