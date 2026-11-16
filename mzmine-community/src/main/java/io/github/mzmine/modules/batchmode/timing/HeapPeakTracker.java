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

package io.github.mzmine.modules.batchmode.timing;

import com.sun.management.GarbageCollectionNotificationInfo;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.openmbean.CompositeData;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks the peak heap usage during a time window, e.g. a batch step, without a sampling thread.
 * The used heap is highest right before a garbage collection, so the tracker listens to the GC
 * notifications of the JVM and records the heap usage before and after each collection. The end of
 * the window is sampled directly in {@link #stop()} to also cover windows without any collection.
 * <p>
 * The heap is shared by the whole JVM, so parallel work outside the window is included. GC
 * notifications are delivered asynchronously, so a collection that ends right before
 * {@link #stop()} may be missed. Collections that started before {@link #start()} are ignored.
 * <p>
 * Requires the jdk.management module for the notifications. Without it, only the end of the
 * window is sampled.
 */
public final class HeapPeakTracker {

  private static final Logger logger = Logger.getLogger(HeapPeakTracker.class.getName());
  private static final double GB = 1 << 30;

  private static final List<HeapPeakTracker> activeTrackers = new CopyOnWriteArrayList<>();
  // installed once on first use
  private static final boolean notificationsAvailable = installGcListener();

  private final long startUptimeMillis;
  private final AtomicLong peakUsedBytes = new AtomicLong(0);
  // -1 while no collection was observed
  private final AtomicLong peakUsedAfterGcBytes = new AtomicLong(-1);
  // after full collections only, which free all garbage. -1 while no full collection was observed
  private final AtomicLong peakUsedAfterFullGcBytes = new AtomicLong(-1);

  private HeapPeakTracker() {
    this(ManagementFactory.getRuntimeMXBean().getUptime());
  }

  HeapPeakTracker(final long startUptimeMillis) {
    this.startUptimeMillis = startUptimeMillis;
  }

  /**
   * Starts tracking immediately, call {@link #stop()} at the end of the window.
   */
  public static @NotNull HeapPeakTracker start() {
    final HeapPeakTracker tracker = new HeapPeakTracker();
    if (notificationsAvailable) {
      activeTrackers.add(tracker);
    }
    return tracker;
  }

  /**
   * Stops tracking and samples the current heap usage as the last value of the window. Call this
   * before an explicit {@link System#gc()} so that the collection is not part of the window.
   */
  public @NotNull HeapPeak stop() {
    activeTrackers.remove(this);
    peakUsedBytes.accumulateAndGet(currentUsedHeapBytes(), Math::max);

    // decision: the heap after a full collection is the live data, a young collection keeps the
    // garbage in the old generation, so full collections are preferred if there were any
    final long afterFullGc = peakUsedAfterFullGcBytes.get();
    final long afterGc = afterFullGc >= 0 ? afterFullGc : peakUsedAfterGcBytes.get();
    final HeapMeasurementQuality quality = afterFullGc >= 0 ? HeapMeasurementQuality.FULL_GC
        : afterGc >= 0 ? HeapMeasurementQuality.PARTIAL_GC : HeapMeasurementQuality.NO_GC;
    return new HeapPeak(toGB(peakUsedBytes.get()), afterGc < 0 ? null : toGB(afterGc), quality);
  }

  void onGc(final long gcStartUptimeMillis, final long usedBeforeBytes,
      final long usedAfterBytes, final boolean fullGc) {
    // decision: attribute a collection by its start time so that the explicit System.gc() at the
    // end of the previous step is not added to the next step when its notification arrives late
    if (gcStartUptimeMillis < startUptimeMillis) {
      return;
    }
    peakUsedBytes.accumulateAndGet(usedBeforeBytes, Math::max);
    peakUsedAfterGcBytes.accumulateAndGet(usedAfterBytes, Math::max);
    if (fullGc) {
      peakUsedAfterFullGcBytes.accumulateAndGet(usedAfterBytes, Math::max);
    }
  }

  private static long currentUsedHeapBytes() {
    return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
  }

  // 3 decimals = MB resolution, matches the other measurement columns
  private static double toGB(final long bytes) {
    return Math.round(bytes / GB * 1e3) / 1e3;
  }

  private static boolean installGcListener() {
    try {
      // the GC notifications report all memory pools, only the heap pools are relevant
      final Set<String> heapPools = ManagementFactory.getMemoryPoolMXBeans().stream()
          .filter(pool -> pool.getType() == MemoryType.HEAP).map(MemoryPoolMXBean::getName)
          .collect(Collectors.toUnmodifiableSet());

      boolean installed = false;
      for (final GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
        if (gcBean instanceof NotificationEmitter emitter) {
          emitter.addNotificationListener(
              (notification, _) -> handleNotification(notification, heapPools), null, null);
          installed = true;
        }
      }
      if (!installed) {
        logger.fine("GC notifications are not available, peak heap is only sampled at the end");
      }
      return installed;
    } catch (LinkageError | RuntimeException e) {
      // assumption: jdk.management may be missing in a custom runtime image
      logger.log(Level.WARNING,
          "Cannot track peak heap usage, peak heap is only sampled at the end", e);
      return false;
    }
  }

  private static void handleNotification(@NotNull final Notification notification,
      @NotNull final Set<String> heapPools) {
    if (activeTrackers.isEmpty() || !GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION
        .equals(notification.getType())) {
      return;
    }
    final GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(
        (CompositeData) notification.getUserData());
    final var gcInfo = info.getGcInfo();
    // e.g. "end of major GC" for full collections like System.gc()
    final boolean fullGc = isFullGc(info.getGcAction());
    final long before = sumHeapPools(gcInfo.getMemoryUsageBeforeGc(), heapPools);
    final long after = sumHeapPools(gcInfo.getMemoryUsageAfterGc(), heapPools);
    for (final HeapPeakTracker tracker : activeTrackers) {
      tracker.onGc(gcInfo.getStartTime(), before, after, fullGc);
    }
  }

  static boolean isFullGc(@NotNull final String action) {
    // HotSpot reports full collections as major GC, young/mixed collections as minor GC and
    // concurrent phases separately. Unknown actions must not imply a confirmed full collection.
    return "end of major GC".equals(action);
  }

  private static long sumHeapPools(@NotNull final Map<String, MemoryUsage> usageByPool,
      @NotNull final Set<String> heapPools) {
    long used = 0;
    for (final var entry : usageByPool.entrySet()) {
      if (heapPools.contains(entry.getKey())) {
        used += entry.getValue().getUsed();
      }
    }
    return used;
  }
}
