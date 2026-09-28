/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package import_data.speed;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;

/**
 * Samples the used heap in a background thread and reads the garbage collector counters, so that a
 * run can answer "how much heap does this batch really need" and "does this branch allocate more".
 * <p>
 * This is opt-in ({@link BatchSpeedJob#trackMemory()}) because it costs performance: the sampling
 * thread takes a core slice from the batch and, more importantly, the numbers invite a slower GC
 * configuration. Use one tracked run to learn the peak heap and the GC behaviour, then keep it off
 * for the runs that compare speed.
 */
public class MemorySampler {

  private static final long INTERVAL_MS = 250;

  private final AtomicBoolean running = new AtomicBoolean(true);
  private final Thread thread;
  private final long startGcCount;
  private final long startGcMillis;
  private volatile long peakUsedBytes;

  private MemorySampler() {
    startGcCount = gcCount();
    startGcMillis = gcMillis();

    thread = new Thread(this::sample, "speed-test-memory-sampler");
    // must never keep the JVM alive
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Starts sampling immediately, call {@link #stop()} after the measured work.
   */
  @NotNull
  public static MemorySampler start() {
    return new MemorySampler();
  }

  private void sample() {
    final Runtime runtime = Runtime.getRuntime();
    while (running.get()) {
      final long used = runtime.totalMemory() - runtime.freeMemory();
      if (used > peakUsedBytes) {
        peakUsedBytes = used;
      }
      try {
        Thread.sleep(INTERVAL_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  /**
   * Stops the sampling thread and returns the peak heap and the garbage collection that happened
   * since {@link #start()}.
   */
  @NotNull
  public MemoryMeasurement stop() {
    running.set(false);
    thread.interrupt();
    return new MemoryMeasurement(Math.round(peakUsedBytes / 1e7) / 100d, gcCount() - startGcCount,
        (gcMillis() - startGcMillis) / 1000.0);
  }

  private static long gcCount() {
    long count = 0;
    for (final GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
      // -1 means the collector does not provide the counter
      count += Math.max(bean.getCollectionCount(), 0);
    }
    return count;
  }

  private static long gcMillis() {
    long millis = 0;
    for (final GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
      millis += Math.max(bean.getCollectionTime(), 0);
    }
    return millis;
  }
}
