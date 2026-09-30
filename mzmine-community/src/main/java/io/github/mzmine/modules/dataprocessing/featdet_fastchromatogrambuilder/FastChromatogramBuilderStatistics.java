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
 * Counters of one {@link FastChromatogramBuilder#build} call, used for logging and benchmarks.
 *
 * @param numScans            processed scans
 * @param numDataPoints       valid data points in all scans
 * @param maxIntensity        intensity of the most intense data point
 * @param numTraces           traces detected in the first pass
 * @param maxActiveTraces     max number of traces that were active at the same time
 * @param numRecordedTraces   traces considered for channels (enough data points)
 * @param numCollisionEvents  events of two traces within the tolerance receiving a data point in
 *                            the same scan
 * @param numCollisionPairs   trace pairs with at least one collision
 * @param numChannels         m/z channels, candidate chromatograms
 * @param numMemberDataPoints data points of traces that belong to a channel
 * @param numMemberConflicts  data points dropped because another trace of the same channel had a
 *                            more intense data point in the same scan
 * @param numLooseDataPoints  data points of traces without channel
 * @param numLooseAssigned    loose data points that filled an empty scan of a channel, includes
 *                            member data points dropped in a conflict
 * @param numHoleFills        holes between intense data points filled with the wider tolerance
 * @param numMergedChannels   channels merged into a passing channel, complementary channels of
 *                            one ion and failed channels
 * @param numBridgedSegments  segments of other channels that filled a dip of a channel, e.g., the
 *                            shifted apex of a saturated ion
 * @param numBridgedDataPoints data points of the bridged segments
 * @param numRecoveredDataPoints data points of failed channels that filled a passing channel
 * @param numCoalescedFills   data points of neighboring channels that also filled a hole, one
 *                            centroid of two unresolved ions
 * @param numChromatograms    chromatograms passing the filters
 * @param firstPassNanos      duration of the first pass
 * @param consolidationNanos  duration of the channel consolidation
 * @param secondPassNanos     duration of the second pass
 * @param finalizationNanos   duration of the merges, the recovery and the filters
 */
public record FastChromatogramBuilderStatistics(int numScans, long numDataPoints,
                                                double maxIntensity, long numTraces,
                                                int maxActiveTraces, int numRecordedTraces,
                                                int numCollisionEvents, int numCollisionPairs,
                                                int numChannels, long numMemberDataPoints,
                                                long numMemberConflicts, long numLooseDataPoints,
                                                long numLooseAssigned, long numHoleFills,
                                                int numMergedChannels, int numBridgedSegments,
                                                long numBridgedDataPoints,
                                                long numRecoveredDataPoints, long numCoalescedFills,
                                                int numChromatograms,
                                                long firstPassNanos, long consolidationNanos,
                                                long secondPassNanos, long finalizationNanos) {

  @Override
  public @NotNull String toString() {
    final String format = """
        scans=%d, data points=%d (max intensity %.3g), traces=%d (max active %d, recorded %d), \
        collisions=%d (pairs %d), channels=%d, member data points=%d (conflicts %d), \
        loose data points=%d (assigned %d), hole fills=%d, merged channels=%d, \
        bridged dips=%d (data points %d), recovered data points=%d, coalesced fills=%d, \
        chromatograms=%d, \
        times [ms]: pass1=%.1f, consolidation=%.1f, pass2=%.1f, finalization=%.1f""";
    return format.formatted(numScans, numDataPoints, maxIntensity, numTraces, maxActiveTraces,
        numRecordedTraces, numCollisionEvents, numCollisionPairs, numChannels, numMemberDataPoints,
        numMemberConflicts, numLooseDataPoints, numLooseAssigned, numHoleFills, numMergedChannels,
        numBridgedSegments, numBridgedDataPoints, numRecoveredDataPoints, numCoalescedFills,
        numChromatograms, firstPassNanos / 1e6, consolidationNanos / 1e6, secondPassNanos / 1e6,
        finalizationNanos / 1e6);
  }
}
