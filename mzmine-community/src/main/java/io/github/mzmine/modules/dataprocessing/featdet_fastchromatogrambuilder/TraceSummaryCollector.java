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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * First pass of {@link FastChromatogramBuilder}: records the statistics of closed traces and the
 * collisions between co-eluting traces within the m/z tolerance. Data points are not stored.
 */
final class TraceSummaryCollector implements MassTraceSweepListener {

  private final int minDataPoints;
  private final int minDataPointsWithoutHeight;
  private final double minHeight;
  private final boolean trackCollisions;

  // recorded traces, index is the record index
  final LongArrayList traceIds = new LongArrayList();
  final DoubleArrayList centers = new DoubleArrayList();
  final DoubleArrayList sumIntensities = new DoubleArrayList();
  final DoubleArrayList sumMzIntensities = new DoubleArrayList();
  final DoubleArrayList maxIntensities = new DoubleArrayList();
  final IntArrayList counts = new IntArrayList();
  final IntArrayList firstScans = new IntArrayList();
  final IntArrayList lastScans = new IntArrayList();

  // one entry per collision event, trace ids
  final LongArrayList collisionTraceA = new LongArrayList();
  final LongArrayList collisionTraceB = new LongArrayList();

  /**
   * Traces that are not recorded contribute their data points as loose data points in the second
   * pass, which fill empty scans of the closest channel. Recording is only needed for traces that
   * may start a channel or that are long enough to be a signal of their own.
   *
   * @param minDataPoints              traces with fewer data points are never recorded
   * @param minDataPointsWithoutHeight traces below the minimum height need this many data points
   * @param minHeight                  traces reaching this height may start a channel
   * @param trackCollisions            record collisions of co-eluting traces within the tolerance
   */
  TraceSummaryCollector(int minDataPoints, int minDataPointsWithoutHeight, double minHeight,
      boolean trackCollisions) {
    this.minDataPoints = minDataPoints;
    this.minDataPointsWithoutHeight = minDataPointsWithoutHeight;
    this.minHeight = minHeight;
    this.trackCollisions = trackCollisions;
  }

  @Override
  public void onCollision(@NotNull MassTraceSweeper sweeper, int slotA, int slotB) {
    collisionTraceA.add(sweeper.getTraceId(slotA));
    collisionTraceB.add(sweeper.getTraceId(slotB));
  }

  @Override
  public void onTraceClosed(@NotNull MassTraceSweeper sweeper, int slot) {
    final int count = sweeper.getCount(slot);
    if (count < minDataPoints || (count < minDataPointsWithoutHeight
        && sweeper.getMaxIntensity(slot) < minHeight)) {
      return;
    }
    traceIds.add(sweeper.getTraceId(slot));
    centers.add(sweeper.getCenter(slot));
    sumIntensities.add(sweeper.getSumIntensity(slot));
    sumMzIntensities.add(sweeper.getSumMzIntensity(slot));
    maxIntensities.add(sweeper.getMaxIntensity(slot));
    counts.add(count);
    firstScans.add(sweeper.getFirstScan(slot));
    lastScans.add(sweeper.getLastScan(slot));
  }

  @Override
  public boolean isCollisionTrackingEnabled() {
    return trackCollisions;
  }

  int getNumRecordedTraces() {
    return traceIds.size();
  }

  int getNumCollisionEvents() {
    return collisionTraceA.size();
  }
}
