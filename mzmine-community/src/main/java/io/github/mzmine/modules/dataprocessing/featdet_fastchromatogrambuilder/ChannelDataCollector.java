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

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;

/**
 * Second pass of {@link FastChromatogramBuilder}: routes every data point into the channel of its
 * trace, the traces of the first pass are recorded in {@link TraceAssignments}. Data points of
 * traces without a channel are loose. At the end of each scan, a loose data point fills the closest
 * channel within the m/z tolerance that has no data point in this scan yet. This fills holes, e.g.,
 * from signals that scattered out of their trace, and keeps the low level signals of the full
 * chromatogram like the ADAP builder.
 * <p>
 * Data points that no channel used are kept for a few scans. When a channel with intense data
 * points continues after a short gap, its holes are filled from these data points with a wider
 * tolerance around the m/z of the neighbors, if the intensity fits between the neighbors.
 */
final class ChannelDataCollector {

  private static final int NONE = -1;
  private static final byte EMPTY = 0;
  private static final byte MEMBER = 1;
  private static final byte LOOSE = 2;

  // decision: 5 mDa bins hold few channels, so a lookup scans only a few values
  private static final double CHANNEL_LOOKUP_BIN_WIDTH = 0.005;

  private final MZTolerance tolerance;
  private final long[] memberIds;
  private final int[] memberChannels;
  private final double[] channelCenters;
  private final int[] memberDataPoints;
  private final BinnedLowerBound channelLookup;
  private final ChannelBuffer[] buffers;

  // channel of the trace in each slot of the sweeper or NONE
  private int[] slotChannel = new int[1024];
  private int memberPointer = 0;
  // traces are numbered in the order in which they start, like in the sweeper
  private long nextTraceId = 0;

  // pending data point of each channel in the current scan
  private final byte[] pendingType;
  private final double[] pendingMz;
  private final double[] pendingIntensity;
  private final double[] pendingDistance;
  private final int[] touchedChannels;
  private int numTouched = 0;

  // loose data points of the current scan, member data points that lost against a more intense
  // data point of their channel are added as well
  private double[] looseMz = new double[256];
  private double[] looseIntensity = new double[256];
  private int numLoose = 0;

  // hole filling: last data point of each channel and the unused data points of the last scans
  private final double holeFillToleranceFactor;
  // max distance of a data point that may fill a hole from the channel center, in tolerances
  private final double holeFillWindowFactor;
  // distance of the last data point in findFreeChannel to the nearest channel, in tolerances
  private double nearestChannelDistance = Double.POSITIVE_INFINITY;
  private final double minHoleFillIntensity;
  private final double logIntensityFactor;
  private final UnusedDataPoints[] recentUnused;
  private final double[] lastMz;
  private final double[] lastIntensity;
  private final int[] lastScan;

  // statistics
  private long numMemberDataPoints = 0;
  private long numMemberConflicts = 0;
  private long numLooseDataPoints = 0;
  private long numLooseAssigned = 0;
  private long numHoleFills = 0;

  /**
   * @param plan                 the channels and their member traces
   * @param tolerance            max m/z distance of a loose data point to a channel center
   * @param minHoleFillIntensity holes are filled with the wider tolerance only between data points
   *                             of at least this intensity, see
   *                             {@link FastChromatogramBuilderOptions#holeFillFlank()}
   * @param options              hole filling, see {@link FastChromatogramBuilderOptions}
   */
  ChannelDataCollector(@NotNull ChannelPlan plan, @NotNull MZTolerance tolerance,
      double minHoleFillIntensity, @NotNull FastChromatogramBuilderOptions options) {
    this.tolerance = tolerance;
    this.memberIds = plan.memberTraceIds();
    this.memberChannels = plan.memberChannels();
    this.channelCenters = plan.channelCenters();
    this.memberDataPoints = plan.memberDataPoints();
    this.channelLookup = new BinnedLowerBound(channelCenters, CHANNEL_LOOKUP_BIN_WIDTH);
    final int numChannels = channelCenters.length;
    buffers = new ChannelBuffer[numChannels];
    pendingType = new byte[numChannels];
    pendingMz = new double[numChannels];
    pendingIntensity = new double[numChannels];
    pendingDistance = new double[numChannels];
    touchedChannels = new int[numChannels];

    holeFillToleranceFactor = options.holeFillToleranceFactor();
    holeFillWindowFactor =
        holeFillToleranceFactor + Math.max(1d, options.complementaryToleranceFactor()) + 1d;
    this.minHoleFillIntensity = minHoleFillIntensity;
    logIntensityFactor = Math.log(options.intensityJumpFactor());
    // one scan more than the longest hole, the current scan is written while older are read
    recentUnused = new UnusedDataPoints[holeFillToleranceFactor > 0d ? options.maxGapScans() + 1
        : 1];
    for (int i = 0; i < recentUnused.length; i++) {
      recentUnused[i] = new UnusedDataPoints();
    }
    lastMz = new double[numChannels];
    lastIntensity = new double[numChannels];
    lastScan = new int[numChannels];
    Arrays.fill(lastScan, NONE);
  }

  /**
   * Routes the data points of one scan in the order of the events of the sweeper: data points that
   * joined a trace in m/z order, then the data points that started a trace in m/z order.
   *
   * @param points the data points of the scan, loaded like in the first pass
   * @param codes  the trace of each data point, see {@link TraceAssignments#scan(int)}
   */
  void routeScan(int scanIndex, @NotNull ScanDataPoints points, @NotNull Object codes) {
    final char[] narrow = codes instanceof char[] c ? c : null;
    final int[] wide = narrow == null ? (int[]) codes : null;
    final int n = points.size();
    if ((narrow != null ? narrow.length : wide.length) != n) {
      throw new IllegalStateException(
          "Scan %d has %d data points, the first pass had %d".formatted(scanIndex, n,
              narrow != null ? narrow.length : wide.length));
    }
    final double[] mzs = points.mzs();
    final double[] intensities = points.intensities();
    for (int i = 0; i < n; i++) {
      final int code = narrow != null ? narrow[i] : wide[i];
      if (!TraceAssignments.isNewTrace(code)) {
        onDataPointAssigned(TraceAssignments.slot(code), mzs[i], intensities[i]);
      }
    }
    for (int i = 0; i < n; i++) {
      final int code = narrow != null ? narrow[i] : wide[i];
      if (TraceAssignments.isNewTrace(code)) {
        final int slot = TraceAssignments.slot(code);
        onTraceCreated(slot);
        onDataPointAssigned(slot, mzs[i], intensities[i]);
      }
    }
    onScanFinished(scanIndex);
  }

  private void onTraceCreated(int slot) {
    if (slot >= slotChannel.length) {
      slotChannel = Arrays.copyOf(slotChannel, Math.max(slot + 1, slotChannel.length * 2));
    }
    // trace ids increase with every new trace, the member ids are sorted
    final long traceId = nextTraceId++;
    while (memberPointer < memberIds.length && memberIds[memberPointer] < traceId) {
      memberPointer++;
    }
    slotChannel[slot] = memberPointer < memberIds.length && memberIds[memberPointer] == traceId
        ? memberChannels[memberPointer] : NONE;
  }

  private void onDataPointAssigned(int slot, double mz, double intensity) {
    final int channel = slotChannel[slot];
    if (channel == NONE) {
      numLooseDataPoints++;
      addLoose(mz, intensity);
      return;
    }
    numMemberDataPoints++;
    if (pendingType[channel] == MEMBER) {
      // two traces of one channel in the same scan, keep the more intense signal like ADAP. The
      // other data point may still fill another channel.
      numMemberConflicts++;
      if (intensity > pendingIntensity[channel]) {
        addLoose(pendingMz[channel], pendingIntensity[channel]);
        pendingMz[channel] = mz;
        pendingIntensity[channel] = intensity;
      } else {
        addLoose(mz, intensity);
      }
      return;
    }
    setPending(channel, MEMBER, mz, intensity, 0d);
  }

  private void onScanFinished(int scanIndex) {
    final UnusedDataPoints unused = recentUnused[scanIndex % recentUnused.length];
    unused.reset(scanIndex);
    for (int i = 0; i < numLoose; i++) {
      assignLoose(looseMz[i], looseIntensity[i], unused);
    }
    numLoose = 0;
    unused.sort();

    for (int i = 0; i < numTouched; i++) {
      final int channel = touchedChannels[i];
      ChannelBuffer buffer = buffers[channel];
      if (buffer == null) {
        // decision: the size of the member traces, loose data points add few more. Growing the
        // buffers took a tenth of the second pass.
        buffer = new ChannelBuffer(memberDataPoints[channel] + 16);
        buffers[channel] = buffer;
      } else if (holeFillToleranceFactor > 0d) {
        fillHoles(channel, buffer, scanIndex);
      }
      buffer.add(scanIndex, pendingMz[channel], pendingIntensity[channel]);
      lastScan[channel] = scanIndex;
      lastMz[channel] = pendingMz[channel];
      lastIntensity[channel] = pendingIntensity[channel];
      pendingType[channel] = EMPTY;
    }
    numTouched = 0;
  }

  /**
   * Visits the channels in the order of their distance to the m/z and fills the first free one
   * within the tolerance. A closer loose data point replaces a farther one, which then tries the
   * next channels. Data points without a channel are kept as unused.
   */
  private void assignLoose(double mz, double intensity, @NotNull UnusedDataPoints unused) {
    double currentMz = mz;
    double currentIntensity = intensity;
    // terminates: a data point only replaces a farther one and channels are finite
    while (true) {
      final int channel = findFreeChannel(currentMz);
      if (channel == NONE) {
        // decision: a hole is filled around the m/z of the flanking data points of a channel,
        // which are at most the complementary tolerance plus the tolerance away from the channel
        // center. Most unused data points of noisy data are far from all channels, keeping them
        // only costs a sort per scan.
        if (holeFillToleranceFactor > 0d && nearestChannelDistance <= holeFillWindowFactor) {
          unused.add(currentMz, currentIntensity);
        }
        return;
      }
      final double distance = Math.abs(currentMz - channelCenters[channel]);
      if (pendingType[channel] == EMPTY) {
        numLooseAssigned++;
        setPending(channel, LOOSE, currentMz, currentIntensity, distance);
        return;
      }
      // replace the farther loose data point and continue with it
      final double replacedMz = pendingMz[channel];
      final double replacedIntensity = pendingIntensity[channel];
      setPending(channel, LOOSE, currentMz, currentIntensity, distance);
      currentMz = replacedMz;
      currentIntensity = replacedIntensity;
    }
  }

  /**
   * Also sets {@link #nearestChannelDistance}.
   *
   * @return the closest channel within the tolerance that has no member data point and no closer
   * loose data point in this scan, or NONE
   */
  private int findFreeChannel(double mz) {
    final int numChannels = channelCenters.length;
    int right = channelLookup.lowerBound(mz);
    int left = right - 1;
    nearestChannelDistance = Double.POSITIVE_INFINITY;
    boolean nearest = true;
    while (left >= 0 || right < numChannels) {
      final double leftDistance = left >= 0 ? mz - channelCenters[left] : Double.POSITIVE_INFINITY;
      final double rightDistance =
          right < numChannels ? channelCenters[right] - mz : Double.POSITIVE_INFINITY;
      final int channel;
      final double distance;
      if (leftDistance <= rightDistance) {
        channel = left--;
        distance = leftDistance;
      } else {
        channel = right++;
        distance = rightDistance;
      }
      final double channelTolerance = tolerance.getMzToleranceForMass(channelCenters[channel]);
      if (nearest) {
        nearestChannelDistance = distance / channelTolerance;
        nearest = false;
      }
      if (distance > channelTolerance) {
        // all remaining channels are farther away
        return NONE;
      }
      final byte type = pendingType[channel];
      if (type == MEMBER || (type == LOOSE && pendingDistance[channel] <= distance)) {
        continue;
      }
      return channel;
    }
    return NONE;
  }

  /**
   * Fills the scans between the last data point of the channel and the pending data point of this
   * scan. Only between intense data points: an intense signal does not vanish for a scan, its data
   * point is usually there with a larger m/z error, e.g., from a coalescing neighbor or a split
   * centroid. The data point closest to the interpolated m/z within the wider tolerance fills the
   * hole if its intensity is within the intensity factor of the interpolated intensity.
   */
  private void fillHoles(int channel, @NotNull ChannelBuffer buffer, int scanIndex) {
    final int previous = lastScan[channel];
    final int gap = scanIndex - previous;
    if (previous == NONE || gap < 2 || gap >= recentUnused.length + 1) {
      return;
    }
    final double intensityBefore = lastIntensity[channel];
    final double intensityAfter = pendingIntensity[channel];
    if (intensityBefore < minHoleFillIntensity || intensityAfter < minHoleFillIntensity) {
      return;
    }
    final double mzBefore = lastMz[channel];
    final double mzAfter = pendingMz[channel];
    final double logBefore = Math.log(intensityBefore);
    final double logAfter = Math.log(intensityAfter);
    for (int hole = previous + 1; hole < scanIndex; hole++) {
      final UnusedDataPoints unused = recentUnused[hole % recentUnused.length];
      if (unused.scanIndex() != hole) {
        continue;
      }
      final double position = (double) (hole - previous) / gap;
      final double mz = mzBefore + position * (mzAfter - mzBefore);
      final double logIntensity = logBefore + position * (logAfter - logBefore);
      final double tol = holeFillToleranceFactor * tolerance.getMzToleranceForMass(mz);
      final int match = unused.findClosest(mz, tol, Math.exp(logIntensity - logIntensityFactor),
          Math.exp(logIntensity + logIntensityFactor));
      if (match < 0) {
        continue;
      }
      unused.markUsed(match);
      buffer.add(hole, unused.mz(match), unused.intensity(match));
      numHoleFills++;
    }
  }

  private void setPending(int channel, byte type, double mz, double intensity, double distance) {
    if (pendingType[channel] == EMPTY) {
      touchedChannels[numTouched++] = channel;
    }
    pendingType[channel] = type;
    pendingMz[channel] = mz;
    pendingIntensity[channel] = intensity;
    pendingDistance[channel] = distance;
  }

  private void addLoose(double mz, double intensity) {
    if (numLoose == looseMz.length) {
      looseMz = Arrays.copyOf(looseMz, numLoose * 2);
      looseIntensity = Arrays.copyOf(looseIntensity, numLoose * 2);
    }
    looseMz[numLoose] = mz;
    looseIntensity[numLoose] = intensity;
    numLoose++;
  }

  int numChannels() {
    return channelCenters.length;
  }

  /**
   * @return the buffer of channel or null if no data point was added
   */
  ChannelBuffer getBuffer(int channel) {
    return buffers[channel];
  }

  double getChannelCenter(int channel) {
    return channelCenters[channel];
  }

  long getNumMemberDataPoints() {
    return numMemberDataPoints;
  }

  long getNumMemberConflicts() {
    return numMemberConflicts;
  }

  long getNumLooseDataPoints() {
    return numLooseDataPoints;
  }

  long getNumLooseAssigned() {
    return numLooseAssigned;
  }

  long getNumHoleFills() {
    return numHoleFills;
  }
}
