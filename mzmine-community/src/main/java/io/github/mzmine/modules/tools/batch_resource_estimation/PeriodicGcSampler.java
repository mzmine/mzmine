package io.github.mzmine.modules.tools.batch_resource_estimation;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Forces full garbage collections in regular intervals while a subset run of the resource
 * estimation runs. The heap after a full collection is the live data, while the heap after the
 * regular young collections also contains old garbage that depends on the heap size. The
 * {@link io.github.mzmine.modules.batchmode.timing.HeapPeakTracker} of each batch step records the
 * heap after these collections.
 * <p>
 * The wait between two collections grows with their duration, which limits the time spent in the
 * forced collections to about {@link #MAX_GC_TIME_FRACTION} of the run.
 */
public final class PeriodicGcSampler implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(PeriodicGcSampler.class.getName());

  private static final long MIN_INTERVAL_MILLIS = 1000;
  private static final double MAX_GC_TIME_FRACTION = 0.2;

  private final Thread thread;
  private final AtomicLong gcNanos = new AtomicLong(0);
  private final AtomicLong collections = new AtomicLong(0);
  private volatile boolean running = true;

  private PeriodicGcSampler() {
    thread = new Thread(this::sample, "resource-estimation-gc-sampler");
    // must never keep the JVM alive
    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Starts forcing collections, close after the measured work.
   */
  public static @NotNull PeriodicGcSampler start() {
    return new PeriodicGcSampler();
  }

  private void sample() {
    long waitMillis = MIN_INTERVAL_MILLIS;
    while (running) {
      try {
        TimeUnit.MILLISECONDS.sleep(waitMillis);
      } catch (InterruptedException e) {
        return;
      }
      if (!running) {
        return;
      }
      final long start = System.nanoTime();
      System.gc();
      final long duration = System.nanoTime() - start;
      gcNanos.addAndGet(duration);
      collections.incrementAndGet();
      waitMillis = Math.max(MIN_INTERVAL_MILLIS,
          (long) (TimeUnit.NANOSECONDS.toMillis(duration) / MAX_GC_TIME_FRACTION));
    }
  }

  /**
   * @return seconds spent in the forced collections so far
   */
  public double gcSeconds() {
    return gcNanos.get() / 1e9;
  }

  @Override
  public void close() {
    running = false;
    thread.interrupt();
    try {
      // a running collection finishes before the thread ends
      thread.join(TimeUnit.SECONDS.toMillis(30));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    logger.fine(() -> "Forced %d collections in %.1f seconds".formatted(collections.get(),
        gcSeconds()));
  }
}
