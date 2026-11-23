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

import org.jetbrains.annotations.NotNull;

/**
 * Result of {@link ChannelConsolidation}: which traces form which m/z channel. A channel becomes
 * one chromatogram across all scans.
 */
final class ChannelPlan {

  static final ChannelPlan EMPTY = new ChannelPlan(new long[0], new int[0], new double[0],
      new int[0], 0, 0);

  private final long[] memberTraceIds;
  private final int[] memberChannels;
  private final double[] channelCenters;
  private final int[] memberDataPoints;
  private final int numRecordedTraces;
  private final int numCollisionPairs;

  /**
   * @param memberTraceIds    trace ids of all traces that belong to a channel, sorted ascending
   * @param memberChannels    channel index of each member trace
   * @param channelCenters    intensity weighted center m/z of each channel, sorted ascending. The
   *                          channel index is the index in this array.
   * @param memberDataPoints  data points of the member traces of each channel
   * @param numRecordedTraces number of traces considered for channels
   * @param numCollisionPairs number of trace pairs that collided at least once
   */
  ChannelPlan(@NotNull long[] memberTraceIds, @NotNull int[] memberChannels,
      @NotNull double[] channelCenters, @NotNull int[] memberDataPoints, int numRecordedTraces,
      int numCollisionPairs) {
    if (memberTraceIds.length != memberChannels.length) {
      throw new IllegalArgumentException("Member ids and channels differ in length");
    }
    if (channelCenters.length != memberDataPoints.length) {
      throw new IllegalArgumentException("Channel centers and data points differ in length");
    }
    this.memberTraceIds = memberTraceIds;
    this.memberChannels = memberChannels;
    this.channelCenters = channelCenters;
    this.memberDataPoints = memberDataPoints;
    this.numRecordedTraces = numRecordedTraces;
    this.numCollisionPairs = numCollisionPairs;
  }

  /**
   * @return data points of the member traces of each channel, the expected size of the chromatogram
   * before loose data points
   */
  @NotNull int[] memberDataPoints() {
    return memberDataPoints;
  }

  @NotNull long[] memberTraceIds() {
    return memberTraceIds;
  }

  @NotNull int[] memberChannels() {
    return memberChannels;
  }

  @NotNull double[] channelCenters() {
    return channelCenters;
  }

  int numChannels() {
    return channelCenters.length;
  }

  int numMembers() {
    return memberTraceIds.length;
  }

  int numRecordedTraces() {
    return numRecordedTraces;
  }

  int numCollisionPairs() {
    return numCollisionPairs;
  }
}
