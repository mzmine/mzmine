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

package io.github.mzmine.modules.visualization.intensitymap.sampling;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Selects the mobility frame shown in the visualizer: the most intense frame, the frame closest to
 * a retention time, or the average of all frames in a retention time range. Selections derived from
 * one instance share its cache of averaged frames, which lives as long as its visualizer.
 */
public final class IntensityMapFrameCache {

  // averaged frames are expensive, resampling after zoom or region changes reuses the last ones
  private static final int CACHE_SIZE = 4;

  private final @NotNull Map<List<Frame>, Frame> averaged;
  private final @Nullable Range<Float> retentionTimes;

  /**
   * Selects the most intense frame.
   */
  public IntensityMapFrameCache() {
    this(new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(final Map.Entry<List<Frame>, Frame> eldest) {
        return size() > CACHE_SIZE;
      }
    }, null);
  }

  private IntensityMapFrameCache(@NotNull final Map<List<Frame>, Frame> averaged,
      @Nullable final Range<Float> retentionTimes) {
    this.averaged = averaged;
    this.retentionTimes = retentionTimes;
  }

  /**
   * @param retentionTimes null selects the most intense frame (base peak), a single retention time
   *                       the closest frame, and a range the average of the frames within it
   * @return a selection sharing the cache of averaged frames
   */
  public @NotNull IntensityMapFrameCache select(@Nullable final Range<Float> retentionTimes) {
    return new IntensityMapFrameCache(averaged, retentionTimes);
  }

  public @Nullable Range<Float> retentionTimes() {
    return retentionTimes;
  }

  /**
   * @return the selected frame, or the average of the selected frames
   */
  public @NotNull Frame frame(final Scan @NotNull [] scans) {
    if (retentionTimes == null || isSingle(retentionTimes)) {
      return closest(scans, retentionTimes == null ? null : retentionTimes.lowerEndpoint());
    }
    final List<Frame> frames = within(scans, retentionTimes);
    if (frames.size() <= 1) {
      // decision: a range narrower than the frame spacing shows the frame closest to its center
      return closest(scans, (retentionTimes.lowerEndpoint() + retentionTimes.upperEndpoint()) / 2);
    }
    return average(frames);
  }

  public static boolean isSingle(@NotNull final Range<Float> retentionTimes) {
    return retentionTimes.lowerEndpoint().equals(retentionTimes.upperEndpoint());
  }

  public static @NotNull List<Frame> within(final Scan @NotNull [] scans,
      @NotNull final Range<Float> retentionTimes) {
    final List<Frame> frames = new ArrayList<>();
    for (final Scan scan : scans) {
      if (scan instanceof Frame frame && retentionTimes.contains(frame.getRetentionTime())) {
        frames.add(frame);
      }
    }
    return frames;
  }

  /**
   * @param retentionTime null selects the most intense frame
   */
  private static @NotNull Frame closest(final Scan @NotNull [] scans,
      @Nullable final Float retentionTime) {
    Frame best = null;
    double bestScore = Double.POSITIVE_INFINITY;
    for (final Scan scan : scans) {
      if (!(scan instanceof Frame frame)) {
        continue;
      }
      final double score =
          retentionTime == null ? -Objects.requireNonNullElse(frame.getBasePeakIntensity(), 0d)
              : Math.abs(frame.getRetentionTime() - retentionTime);
      if (score < bestScore || best == null) {
        best = frame;
        bestScore = score;
      }
    }
    if (best == null) {
      throw new IllegalArgumentException("No mobility frame matches the scan selection");
    }
    return best;
  }

  private @NotNull Frame average(@NotNull final List<Frame> frames) {
    final List<Frame> key = List.copyOf(frames);
    synchronized (averaged) {
      final Frame cached = averaged.get(key);
      if (cached != null) {
        return cached;
      }
    }
    final Frame frame;
    try {
      // same merging as the frame range selection of the ion mobility raw data overview, but with
      // averaged instead of maximum intensities. Signals must occur in a few mobility scans, which
      // removes single noise points.
      frame = SpectraMerging.getMergedFrame(null, SpectraMerging.defaultMs1MergeTol, key, 1,
          IntensityMergingType.AVERAGE, null, null, Math.min(key.size() - 1, 5),
          new AtomicDouble());
    } catch (final AssertionError ex) {
      // thrown for frames of different polarity or MS level
      throw new IllegalArgumentException("Cannot average these frames: " + ex.getMessage(), ex);
    }
    synchronized (averaged) {
      averaged.put(key, frame);
    }
    return frame;
  }
}
