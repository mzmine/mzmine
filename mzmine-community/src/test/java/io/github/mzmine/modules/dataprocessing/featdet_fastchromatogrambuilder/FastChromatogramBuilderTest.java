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

import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.GroundTruthEvaluator.Summary;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.SyntheticLcmsData.Ion;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FastChromatogramBuilderTest {

  private static final MZTolerance TOLERANCE = new MZTolerance(0.002, 10);
  private static final int MIN_CONSECUTIVE = 5;
  private static final double MIN_GROUP_INTENSITY = 1E3;
  private static final double MIN_HEIGHT = 1E4;

  @NotNull
  private static FastChromatogramBuilder builder() {
    return new FastChromatogramBuilder(TOLERANCE, MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT);
  }

  @NotNull
  private static List<BuiltChromatogram> build(@NotNull SyntheticLcmsData data) {
    return build(builder(), data);
  }

  @NotNull
  private static List<BuiltChromatogram> build(@NotNull FastChromatogramBuilder builder,
      @NotNull SyntheticLcmsData data) {
    final List<BuiltChromatogram> chromatograms = builder.build(data.scans(), null, null);
    Assertions.assertNotNull(chromatograms);
    return chromatograms;
  }

  @NotNull
  private static Summary evaluate(@NotNull SyntheticLcmsData data,
      @NotNull List<BuiltChromatogram> chromatograms) {
    return GroundTruthEvaluator.evaluate(data, EvaluatedChromatogram.of(chromatograms),
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT);
  }

  private static double ppm(double mz, double ppm) {
    return mz * (1 + ppm * 1E-6);
  }

  @Test
  void singleIonWithScatterFormsOneCompleteChromatogram() {
    final SyntheticLcmsData data = SyntheticLcmsData.builder(60).ion(new Ion(500, 30, 4, 1E6, 2, 0))
        .detectionThreshold(500).maxErrorFactor(2).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size());
    final Summary summary = evaluate(data, chromatograms);
    Assertions.assertEquals(1, summary.foundIons());
    Assertions.assertEquals(1d, summary.meanCompleteness(), 1E-9);
    Assertions.assertEquals(0, summary.holes());
  }

  @Test
  void isobaricIonsAtDifferentRetentionTimesShareOneChromatogram() {
    // 6 ppm apart, within the tolerance but eluting at different times
    final SyntheticLcmsData data = SyntheticLcmsData.builder(120)
        .ion(new Ion(500, 25, 4, 1E6, 1, 0)).ion(new Ion(ppm(500, 6), 85, 4, 5E5, 1, 0))
        .detectionThreshold(500).maxErrorFactor(2).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size(),
        "No duplicate chromatogram within the tolerance");
    final Summary summary = evaluate(data, chromatograms);
    Assertions.assertEquals(2, summary.foundIons());
    Assertions.assertEquals(1d, summary.meanCompleteness(), 1E-9);
    Assertions.assertEquals(0, summary.splitIons());
  }

  @Test
  void coelutingResolvedIonsStaySeparate() {
    // both ions are resolved in every scan, 7 ppm apart
    final SyntheticLcmsData data = SyntheticLcmsData.builder(60)
        .ion(new Ion(500, 30, 4, 1E6, 0.5, 0)).ion(new Ion(ppm(500, 7), 31, 4, 6E5, 0.5, 0))
        .detectionThreshold(500).maxErrorFactor(2).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(2, chromatograms.size());
    final Summary summary = evaluate(data, chromatograms);
    Assertions.assertEquals(2, summary.foundIons());
    Assertions.assertEquals(0, summary.splitIons());
    Assertions.assertEquals(0, summary.foreign(), "No signal of the other ion in a chromatogram");
    Assertions.assertTrue(summary.meanCompleteness() > 0.99, summary.toString());
  }

  @Test
  void coelutingIonsWithinToleranceMergeWithoutCollisionSeparation() {
    final SyntheticLcmsData data = SyntheticLcmsData.builder(60)
        .ion(new Ion(500, 30, 4, 1E6, 0.5, 0)).ion(new Ion(ppm(500, 7), 31, 4, 6E5, 0.5, 0))
        .detectionThreshold(500).maxErrorFactor(2).build();
    final FastChromatogramBuilder merging = new FastChromatogramBuilder(TOLERANCE, MIN_CONSECUTIVE,
        MIN_GROUP_INTENSITY, MIN_HEIGHT,
        FastChromatogramBuilderOptions.DEFAULT.withSeparateCollidingTraces(false));
    final List<BuiltChromatogram> chromatograms = build(merging, data);
    Assertions.assertEquals(1, chromatograms.size());
    // the more intense data point wins each scan like in the ADAP builder
    final BuiltChromatogram chromatogram = chromatograms.getFirst();
    for (int i = 0; i < chromatogram.getNumberOfDataPoints(); i++) {
      final int scan = chromatogram.getScanIndex(i);
      double max = 0;
      for (final double intensity : data.intensities[scan]) {
        max = Math.max(max, intensity);
      }
      Assertions.assertEquals(max, chromatogram.getIntensity(i));
    }
  }

  @Test
  void leadingEdgeScatterDoesNotSplitTheChromatogram() {
    // the leading edge scatters to +6 ppm, the first data point after it is 11 ppm away and starts
    // a new trace. Both traces are within the tolerance of the final center and form one channel.
    final double mz = 500;
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    final double[] leading = {3E3, 6E3, 1.2E4};
    for (int s = 0; s < leading.length; s++) {
      builder.dataPoint(10 + s, ppm(mz, 6), leading[s], 0);
    }
    builder.dataPoint(13, ppm(mz, -5), 3E4, 0);
    final double[] rest = {1E5, 3E5, 6E5, 1E6, 6E5, 3E5, 1E5, 3E4, 1E4, 3E3};
    for (int s = 0; s < rest.length; s++) {
      builder.dataPoint(14 + s, ppm(mz, s % 2 == 0 ? -1 : -2), rest[s], 0);
    }
    // the ion definition with zero height only labels the explicit data points
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 17, 3, 0)).build();

    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size(), "The scattered leading edge is no duplicate");
    Assertions.assertEquals(leading.length + 1 + rest.length,
        chromatograms.getFirst().getNumberOfDataPoints());
  }

  @Test
  void centroidJumpBeyondToleranceIsJoinedByComplementaryTraces() {
    // the centroid of one ion jumps from +6 to -6 ppm within the peak, 12 ppm is more than the
    // tolerance. Both halves are traces that never share a scan and form one chromatogram.
    final double mz = 500;
    final double[] profile = {5E3, 2E4, 1E5, 4E5, 8E5, 1E6, 8E5, 4E5, 1E5, 2E4, 5E3};
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    for (int s = 0; s < profile.length; s++) {
      builder.dataPoint(10 + s, ppm(mz, s < 5 ? 6 : -6), profile[s], 0);
    }
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 15, 2, 0)).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size(), "No duplicate from the centroid jump");
    Assertions.assertEquals(profile.length, chromatograms.getFirst().getNumberOfDataPoints());

    final FastChromatogramBuilder withoutComplementary = new FastChromatogramBuilder(TOLERANCE,
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT,
        FastChromatogramBuilderOptions.DEFAULT.withComplementaryToleranceFactor(1));
    Assertions.assertEquals(2, build(withoutComplementary, data).size(),
        "Without joining, both halves are chromatograms");
  }

  /**
   * The apex trace at +5 ppm ends before 4 scans without data point, the next trace at -2 ppm joins
   * its channel. The tail at -13 ppm is 18 ppm from the apex and starts 12 scans after the apex
   * trace, too late for the complementary check of the consolidation against the seed trace.
   */
  @NotNull
  private static SyntheticLcmsData tailFarFromTheApexTrace(int tailScans) {
    final double mz = 500;
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(60);
    final double[] apex = {4E5, 8E5, 1E6, 8E5, 4E5};
    for (int s = 0; s < apex.length; s++) {
      builder.dataPoint(20 + s, ppm(mz, 5), apex[s], 0);
    }
    final double[] decline = {3E5, 2.5E5, 2E5, 1.6E5, 1.3E5, 1E5, 8E4};
    for (int s = 0; s < decline.length; s++) {
      builder.dataPoint(29 + s, ppm(mz, -2), decline[s], 0);
    }
    for (int s = 0; s < tailScans; s++) {
      builder.dataPoint(36 + s, ppm(mz, -13), 6E4 - 5E3 * s, 0);
    }
    return builder.ion(new Ion(mz, 22, 4, 0)).build();
  }

  @Test
  void complementaryChannelFarFromTheSeedTraceIsMerged() {
    final SyntheticLcmsData data = tailFarFromTheApexTrace(9);
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size(), "The tail is no chromatogram of its own");
    Assertions.assertEquals(5 + 7 + 9, chromatograms.getFirst().getNumberOfDataPoints());

    final FastChromatogramBuilder withoutComplementary = new FastChromatogramBuilder(TOLERANCE,
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT,
        FastChromatogramBuilderOptions.DEFAULT.withComplementaryToleranceFactor(1));
    Assertions.assertEquals(2, build(withoutComplementary, data).size());
  }

  @Test
  void failedComplementaryChannelMergesIntoThePassingChannel() {
    // the tail alone is too short for the min consecutive scans
    final SyntheticLcmsData data = tailFarFromTheApexTrace(MIN_CONSECUTIVE - 1);
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size());
    Assertions.assertEquals(5 + 7 + MIN_CONSECUTIVE - 1,
        chromatograms.getFirst().getNumberOfDataPoints(), "The data points of the tail are kept");
  }

  @Test
  void complementaryIonsAtDistantTimesStaySeparate() {
    // 15 ppm apart, never in the same scan but 20 scans apart
    final double mz = 500;
    final double[] profile = {5E3, 2E4, 1E5, 4E5, 8E5, 1E6, 8E5, 4E5, 1E5, 2E4, 5E3};
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(60);
    for (int s = 0; s < profile.length; s++) {
      builder.dataPoint(10 + s, mz, profile[s], 0);
      builder.dataPoint(40 + s, ppm(mz, 15), profile[s], 1);
    }
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 15, 2, 0))
        .ion(new Ion(ppm(mz, 15), 45, 2, 0)).build();
    Assertions.assertEquals(2, build(data).size());
  }

  @Test
  void peakInTheGapOfAnotherChannelStaysSeparate() {
    // an intense ion at +17 ppm with a weak baseline that is missing while another ion elutes,
    // both channels never share a scan and touch at the baseline
    final double mz = 500;
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(60);
    final double[] intense = {1E5, 5E5, 2E6, 5E6, 8E6, 1E7, 8E6, 5E6, 2E6, 5E5, 1E5};
    for (int s = 0; s < 60; s++) {
      if (s >= 45 && s <= 55) {
        builder.dataPoint(s, ppm(mz, 17), intense[s - 45], 0);
      } else if (s < 20 || s >= 40) {
        builder.dataPoint(s, ppm(mz, 17), 3E3, 0);
      }
    }
    final double[] peak = {5E3, 1E4, 3E4, 8E4, 2E5, 5E5, 8E5, 1E6, 8E5, 5E5, 2E5, 8E4, 3E4, 1E4,
        5E3};
    for (int s = 0; s < peak.length; s++) {
      builder.dataPoint(21 + s, mz, peak[s], 1);
    }
    final SyntheticLcmsData data = builder.ion(new Ion(ppm(mz, 17), 50, 2, 0))
        .ion(new Ion(mz, 28, 3, 0)).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(2, chromatograms.size(), "The peak is not part of the baseline");
    Assertions.assertEquals(peak.length, chromatograms.getFirst().getNumberOfDataPoints());
  }

  /**
   * A side signal at +16 ppm co-elutes with the ion in 3 scans. In the apex scan, only the ion is
   * detected, shifted towards the side signal, and the trace of the side signal takes it. The side
   * signal trace starts a channel that fails the min consecutive scans.
   */
  @NotNull
  private static SyntheticLcmsData apexTakenBySideSignal(double apexPpm) {
    final double mz = 400;
    final double[] profile = {5E3, 2E4, 8E4, 2E5, 4E5, 8E5, 1.2E6, 1E6, 7E5, 4E5, 2E5, 8E4, 2E4,
        5E3, 2E3};
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    for (int s = 0; s < profile.length; s++) {
      builder.dataPoint(10 + s, ppm(mz, s == 6 ? apexPpm : 0), profile[s], 0);
    }
    builder.dataPoint(15, ppm(mz, 16), 5E5, 1).dataPoint(17, ppm(mz, 16), 5E5, 1)
        .dataPoint(18, ppm(mz, 16), 4E5, 1);
    return builder.ion(new Ion(mz, 16, 2, 0)).ion(new Ion(ppm(mz, 16), 16, 1, 0)).build();
  }

  @Test
  void apexTakenByFailedChannelIsRecovered() {
    // the shifted apex is within the tolerance of the ion channel
    final List<BuiltChromatogram> chromatograms = build(apexTakenBySideSignal(9));
    Assertions.assertEquals(1, chromatograms.size());
    final BuiltChromatogram chromatogram = chromatograms.getFirst();
    Assertions.assertEquals(15, chromatogram.getNumberOfDataPoints());
    Assertions.assertEquals(1.2E6, chromatogram.getMaxIntensity());
    Assertions.assertEquals(16, chromatogram.getScanIndex(6));
    Assertions.assertEquals(ppm(400, 9), chromatogram.getMz(6), 1E-9);
  }

  @Test
  void apexTakenByFailedChannelFillsTheHoleWithWiderTolerance() {
    // the shifted apex is beyond the tolerance but within the hole fill tolerance
    final List<BuiltChromatogram> chromatograms = build(apexTakenBySideSignal(14));
    Assertions.assertEquals(1, chromatograms.size());
    final BuiltChromatogram chromatogram = chromatograms.getFirst();
    Assertions.assertEquals(15, chromatogram.getNumberOfDataPoints());
    Assertions.assertEquals(ppm(400, 14), chromatogram.getMz(6), 1E-9);

    final FastChromatogramBuilder withoutHoleFill = new FastChromatogramBuilder(TOLERANCE,
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT,
        FastChromatogramBuilderOptions.DEFAULT.withHoleFillToleranceFactor(0));
    Assertions.assertEquals(14,
        build(withoutHoleFill, apexTakenBySideSignal(14)).getFirst().getNumberOfDataPoints());
  }

  /**
   * An ion that saturates the detector: the apex is a plateau whose m/z drifts to +25..+35 ppm, 2.5
   * to 3.5 times the tolerance, like GC-EI-QTOF data. The plateau data points form a channel of
   * their own and leave a dip in the channel of the ion.
   *
   * @param peaks    number of saturated peaks of the ion, 40 scans apart
   * @param baseline intensity of the ion between the peaks, 0 for none
   */
  @NotNull
  private static SyntheticLcmsData saturatedIon(int peaks, double baseline) {
    final double mz = 204.1;
    final double[] rising = {1E4, 3E4, 1E5, 3E5, 8E5, 1.5E6, 3E6, 5E6, 7E6};
    final int plateau = 12;
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40 + 40 * peaks);
    for (int p = 0; p < peaks; p++) {
      final int start = 10 + 40 * p;
      for (int s = 0; s < rising.length; s++) {
        builder.dataPoint(start + s, ppm(mz, s % 2 == 0 ? 1 : -1), rising[s], 0);
        builder.dataPoint(start + rising.length + plateau + s, ppm(mz, s % 2 == 0 ? -1 : 1),
            rising[rising.length - 1 - s], 0);
      }
      for (int s = 0; s < plateau; s++) {
        builder.dataPoint(start + rising.length + s, ppm(mz, plateauShift(s)), 7.4E6 + 1E3 * s, 0);
      }
      if (baseline > 0 && p + 1 < peaks) {
        // the ion stays intense between the peaks
        for (int s = start + 2 * rising.length + plateau; s < start + 40; s++) {
          builder.dataPoint(s, ppm(mz, s % 2 == 0 ? 1 : -1), baseline, 0);
        }
      }
    }
    return builder.ion(new Ion(mz, 25, 5, 0)).build();
  }

  /**
   * @return the shift of the saturated plateau of 12 scans, 25 ppm at the edges, 35 ppm in the
   * center
   */
  private static double plateauShift(int scanInPlateau) {
    return 35 - 10 * Math.abs(scanInPlateau - 5.5) / 5.5;
  }

  @Test
  void saturatedApexWithShiftedMzBridgesTheDip() {
    final SyntheticLcmsData data = saturatedIon(1, 0);
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size(), "The plateau is no chromatogram of its own");
    final BuiltChromatogram chromatogram = chromatograms.getFirst();
    Assertions.assertEquals(30, chromatogram.getNumberOfDataPoints());
    Assertions.assertEquals(7.4E6 + 11E3, chromatogram.getMaxIntensity());
    // the plateau keeps its shifted m/z values
    Assertions.assertEquals(ppm(204.1, plateauShift(6)), chromatogram.getMz(9 + 6), 1E-9);

    final FastChromatogramBuilder withoutBridge = new FastChromatogramBuilder(TOLERANCE,
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT,
        FastChromatogramBuilderOptions.DEFAULT.withDipBridgeToleranceFactor(0));
    Assertions.assertEquals(2, build(withoutBridge, data).size(),
        "Without the bridge, the ion channel has a dip at the apex");
  }

  @Test
  void ionBetweenSaturatedApexesStaysInItsChannel() {
    // the long segment of the ion between both plateaus also connects the two plateau segments of
    // the other channel, it must not move into the plateau channel
    final SyntheticLcmsData data = saturatedIon(2, 2E6);
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size());
    Assertions.assertEquals(30 + 30 + 10, chromatograms.getFirst().getNumberOfDataPoints());
    Assertions.assertEquals(204.1, chromatograms.getFirst().getCenterMz(), 204.1 * 2E-6);
  }

  @Test
  void separatePeakInTheDipOfAnIntenseChannelStaysSeparate() {
    // an intense ion without signal in scans 20-32 and a separate ion 30 ppm away that elutes in
    // this gap. Its peak starts and ends far below the intense channel, no continuation.
    final double mz = 204.1;
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(60);
    for (int s = 5; s < 55; s++) {
      if (s < 20 || s > 32) {
        builder.dataPoint(s, ppm(mz, s % 2 == 0 ? 1 : -1), 2E5, 0);
      }
    }
    final double[] peak = {1E4, 4E4, 1E5, 2E5, 4E5, 6E5, 7E5, 6E5, 4E5, 2E5, 1E5, 4E4, 1E4};
    for (int s = 0; s < peak.length; s++) {
      builder.dataPoint(20 + s, ppm(mz, 30), peak[s], 1);
    }
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 30, 20, 0))
        .ion(new Ion(ppm(mz, 30), 26, 3, 0)).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(2, chromatograms.size());
    Assertions.assertEquals(peak.length, chromatograms.getLast().getNumberOfDataPoints());
  }

  @Test
  void alternatingScatterIsJoinedWithSingleDataPointGap() {
    // one ion alternates between -6 and +6 ppm, two interleaved traces that never share a scan.
    // Each trace alone has no consecutive scans, only joined they form the chromatogram. Traces
    // with one data point need to survive one scan without data point to form the traces.
    final double mz = 500;
    final double[] profile = {5E3, 2E4, 1E5, 4E5, 8E5, 1E6, 8E5, 4E5, 1E5, 2E4, 5E3};
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    for (int s = 0; s < profile.length; s++) {
      builder.dataPoint(10 + s, ppm(mz, s % 2 == 0 ? -6 : 6), profile[s], 0);
    }
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 15, 2, 0)).build();
    final FastChromatogramBuilder gapBuilder = new FastChromatogramBuilder(TOLERANCE,
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT,
        FastChromatogramBuilderOptions.DEFAULT.withMaxGapScans(3, 1));
    final List<BuiltChromatogram> chromatograms = build(gapBuilder, data);
    Assertions.assertEquals(1, chromatograms.size());
    Assertions.assertEquals(profile.length, chromatograms.getFirst().getNumberOfDataPoints());
  }

  @Test
  void coelutingIonsBeyondToleranceStaySeparate() {
    // 15 ppm apart and present in the same scans, complementary joining must not merge them
    final SyntheticLcmsData data = SyntheticLcmsData.builder(60)
        .ion(new Ion(500, 30, 4, 1E6, 0.5, 0)).ion(new Ion(ppm(500, 15), 30, 4, 8E5, 0.5, 0))
        .detectionThreshold(500).maxErrorFactor(2).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(2, chromatograms.size());
    final Summary summary = evaluate(data, chromatograms);
    Assertions.assertEquals(2, summary.foundIons());
    Assertions.assertEquals(0, summary.foreign());
  }

  @Test
  void noiseCloseToCenterDoesNotReplaceTheSignal() {
    final double mz = 500;
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    final double[] profile = {3E3, 1E4, 5E4, 2E5, 6E5, 1E6, 6E5, 2E5, 5E4, 1E4, 3E3};
    for (int s = 0; s < profile.length; s++) {
      // the apex scatters to +4 ppm
      builder.dataPoint(10 + s, ppm(mz, s == 5 ? 4 : 0), profile[s], 0);
    }
    // noise very close to the trace center in the apex scan
    builder.dataPoint(15, ppm(mz, 0.5), 1E3, SyntheticLcmsData.NOISE);
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 15, 2, 0)).build();

    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size());
    final BuiltChromatogram chromatogram = chromatograms.getFirst();
    Assertions.assertEquals(1E6, chromatogram.getMaxIntensity(),
        "Apex must not be replaced by noise");
    Assertions.assertEquals(0, evaluate(data, chromatograms).replaced());
  }

  @Test
  void driftingIonFormsOneChromatogram() {
    // drifts 0.5 ppm per scan, the edges are further apart than the tolerance
    final SyntheticLcmsData data = SyntheticLcmsData.builder(80)
        .ion(new Ion(500, 40, 6, 1E6, 0.5, 0.5)).detectionThreshold(1E3).maxErrorFactor(2).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    final Summary summary = evaluate(data, chromatograms);
    Assertions.assertEquals(1, summary.foundIons());
    Assertions.assertEquals(0, summary.splitIons(), summary.toString());
    Assertions.assertTrue(summary.meanCompleteness() > 0.95, summary.toString());
  }

  @Test
  void holeBetweenIntenseDataPointsIsFilledWithWiderTolerance() {
    // the apex centroid is 15 ppm off, beyond the tolerance of 10 ppm but within twice of it
    final double mz = 400;
    final double[] profile = {5E3, 2E4, 1E5, 4E5, 8E5, 1E6, 8E5, 4E5, 1E5, 2E4, 5E3};
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    for (int s = 0; s < profile.length; s++) {
      builder.dataPoint(10 + s, ppm(mz, s == 5 ? 15 : 0), profile[s], 0);
    }
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 15, 2, 0)).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size());
    final BuiltChromatogram chromatogram = chromatograms.getFirst();
    Assertions.assertEquals(profile.length, chromatogram.getNumberOfDataPoints());
    Assertions.assertEquals(ppm(mz, 15), chromatogram.getMz(5), 1E-9);

    final FastChromatogramBuilder withoutHoleFill = new FastChromatogramBuilder(TOLERANCE,
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT,
        FastChromatogramBuilderOptions.DEFAULT.withHoleFillToleranceFactor(0));
    Assertions.assertEquals(profile.length - 1,
        build(withoutHoleFill, data).getFirst().getNumberOfDataPoints(), "Hole without filling");
  }

  /**
   * Two co-eluting ions 20 ppm apart, twice the tolerance. In the apex scans 5 to 9 the instrument
   * does not resolve them and yields one centroid at +13 ppm, within the tolerance of the second
   * ion only, like m/z 262.120 and 262.133 on GC-EI-QTOF data.
   *
   * @param coalescedMz m/z of the coalesced centroids
   */
  @NotNull
  private static SyntheticLcmsData coalescedIons(double coalescedMz) {
    final double mz = 500;
    final double[] profile = {5E3, 2E4, 5E4, 1E5, 2E5, 3E5, 4E5, 4.5E5, 4E5, 3E5, 2E5, 1E5, 5E4,
        2E4, 5E3};
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    for (int s = 0; s < profile.length; s++) {
      if (s >= 5 && s <= 9) {
        builder.dataPoint(10 + s, coalescedMz, profile[s], 1);
      } else {
        builder.dataPoint(10 + s, ppm(mz, s % 2 == 0 ? 1 : -1), profile[s], 0);
        builder.dataPoint(10 + s, ppm(mz, s % 2 == 0 ? 19 : 21), 0.8 * profile[s], 1);
      }
    }
    return builder.ion(new Ion(mz, 17, 3, 0)).ion(new Ion(ppm(mz, 20), 17, 3, 0)).build();
  }

  @Test
  void coalescedCentroidOfTwoIonsFillsBothChromatograms() {
    final SyntheticLcmsData data = coalescedIons(ppm(500, 13));
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(2, chromatograms.size());
    for (final BuiltChromatogram chromatogram : chromatograms) {
      Assertions.assertEquals(15, chromatogram.getNumberOfDataPoints(), "No hole in the apex");
    }
    // the shared centroid keeps its m/z in both chromatograms
    Assertions.assertEquals(ppm(500, 13), chromatograms.getFirst().getMz(7), 1E-9);
    Assertions.assertEquals(ppm(500, 13), chromatograms.getLast().getMz(7), 1E-9);

    final FastChromatogramBuilder withoutCoalescedFill = new FastChromatogramBuilder(TOLERANCE,
        MIN_CONSECUTIVE, MIN_GROUP_INTENSITY, MIN_HEIGHT,
        FastChromatogramBuilderOptions.DEFAULT.withCoalescedMaxHoleScans(0));
    Assertions.assertEquals(10,
        build(withoutCoalescedFill, data).getFirst().getNumberOfDataPoints(),
        "Without the fill, the first ion misses the apex scans");
  }

  @Test
  void centroidCloseToItsChannelDoesNotFillTheHoleOfTheNeighbor() {
    // +18 ppm is regular scatter of the second ion, shifted by less than half the tolerance
    final List<BuiltChromatogram> chromatograms = build(coalescedIons(ppm(500, 18)));
    Assertions.assertEquals(2, chromatograms.size());
    Assertions.assertEquals(10, chromatograms.getFirst().getNumberOfDataPoints());
  }

  @Test
  void holeFillRejectsDataPointsWithImplausibleIntensity() {
    // the apex is missing, a weak noise signal 15 ppm off must not fill the hole
    final double mz = 400;
    final double[] profile = {5E3, 2E4, 1E5, 4E5, 8E5, 1E6, 8E5, 4E5, 1E5, 2E4, 5E3};
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    for (int s = 0; s < profile.length; s++) {
      if (s == 5) {
        builder.dataPoint(10 + s, ppm(mz, 15), 2E3, SyntheticLcmsData.NOISE);
      } else {
        builder.dataPoint(10 + s, mz, profile[s], 0);
      }
    }
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 15, 2, 0)).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size());
    final BuiltChromatogram chromatogram = chromatograms.getFirst();
    Assertions.assertEquals(profile.length - 1, chromatogram.getNumberOfDataPoints());
    for (int i = 0; i < chromatogram.getNumberOfDataPoints(); i++) {
      Assertions.assertNotEquals(15, chromatogram.getScanIndex(i), "Noise filled the hole");
    }
  }

  @Test
  void missingScansInsidePeakKeepOneChromatogram() {
    final double mz = 300;
    final SyntheticLcmsData.Builder builder = SyntheticLcmsData.builder(40);
    final double[] profile = {5E3, 2E4, 1E5, 4E5, 8E5, 1E6, 8E5, 4E5, 1E5, 2E4, 5E3};
    for (int s = 0; s < profile.length; s++) {
      // two missing scans, the scans 3 to 7 remain consecutive
      if (s == 2 || s == 8) {
        continue;
      }
      builder.dataPoint(10 + s, mz, profile[s], 0);
    }
    final SyntheticLcmsData data = builder.ion(new Ion(mz, 15, 2, 0)).build();
    final List<BuiltChromatogram> chromatograms = build(data);
    Assertions.assertEquals(1, chromatograms.size());
    Assertions.assertEquals(profile.length - 2, chromatograms.getFirst().getNumberOfDataPoints());
  }

  @Test
  void noisyRandomDataMatchesTheGroundTruth() {
    final SyntheticLcmsData data = randomData(300, 400, 150, 7);
    final List<BuiltChromatogram> chromatograms = build(data);
    final Summary summary = evaluate(data, chromatograms);
    Assertions.assertTrue(summary.detectableIons() > 250, summary.toString());
    Assertions.assertTrue(summary.foundIons() >= summary.detectableIons() * 0.99,
        summary.toString());
    Assertions.assertTrue(summary.meanCompleteness() > 0.98, summary.toString());
    Assertions.assertTrue(summary.splitIons() <= summary.detectableIons() * 0.01,
        summary.toString());
  }

  @Test
  void secondPassReplaysTheFirstPass() {
    final SyntheticLcmsData data = randomData(200, 300, 400, 11);
    final FastChromatogramBuilder builder = builder();
    final List<BuiltChromatogram> first = build(builder, data);
    final FastChromatogramBuilderStatistics statistics = builder.getStatistics();
    Assertions.assertNotNull(statistics);
    Assertions.assertEquals(data.numDataPoints(), statistics.numDataPoints());
    // the replay visits every data point once, either routed into a channel or loose
    Assertions.assertEquals(statistics.numDataPoints(),
        statistics.numMemberDataPoints() + statistics.numLooseDataPoints());
    Assertions.assertTrue(statistics.numMemberDataPoints() > 0);
    assertChromatogramsEqual(first, build(data));
  }

  @Test
  void unsortedScansGiveTheSameResult() {
    final SyntheticLcmsData data = randomData(100, 200, 100, 3);
    final Random random = new Random(1);
    final double[][] mzs = new double[data.numScans()][];
    final double[][] intensities = new double[data.numScans()][];
    for (int s = 0; s < data.numScans(); s++) {
      final int n = data.mzs[s].length;
      final List<Integer> order = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        order.add(i);
      }
      Collections.shuffle(order, random);
      mzs[s] = new double[n];
      intensities[s] = new double[n];
      for (int i = 0; i < n; i++) {
        mzs[s][i] = data.mzs[s][order.get(i)];
        intensities[s][i] = data.intensities[s][order.get(i)];
      }
    }
    final List<BuiltChromatogram> sorted = build(data);
    final List<BuiltChromatogram> shuffled = builder().build(new ArrayScans(mzs, intensities), null,
        null);
    Assertions.assertNotNull(shuffled);
    assertChromatogramsEqual(sorted, shuffled);
  }

  @Test
  void invalidValuesAreIgnored() {
    final double[][] mzs = new double[20][];
    final double[][] intensities = new double[20][];
    for (int s = 0; s < 20; s++) {
      mzs[s] = new double[]{200, 300, 400, Double.NaN};
      intensities[s] = new double[]{0, 5E4, -1, 5E4};
    }
    final List<BuiltChromatogram> chromatograms = builder().build(new ArrayScans(mzs, intensities),
        null, null);
    Assertions.assertNotNull(chromatograms);
    Assertions.assertEquals(1, chromatograms.size());
    Assertions.assertEquals(300, chromatograms.getFirst().getCenterMz(), 1E-9);
    Assertions.assertEquals(20, chromatograms.getFirst().getNumberOfDataPoints());
  }

  @Test
  void cancelStopsTheBuild() {
    final SyntheticLcmsData data = randomData(50, 100, 50, 5);
    Assertions.assertNull(builder().build(data.scans(), () -> true, null));
  }

  @Test
  void filterNeedsConsecutiveScansAboveGroupIntensityAndHeightInSegment() {
    final int[] scans = {0, 1, 2, 3, 4, 6, 7, 8, 9, 10};
    // first segment is long enough but too low, second segment is high but too short
    final double[] intensities = {2E3, 2E3, 2E3, 2E3, 2E3, 5E3, 2E4, 5E3, 5E2, 3E3};
    Assertions.assertFalse(
        FastChromatogramBuilder.passesFilters(scans, intensities, scans.length, 5, 1E3, 1E4));
    Assertions.assertTrue(
        FastChromatogramBuilder.passesFilters(scans, intensities, scans.length, 3, 1E3, 1E4));
    Assertions.assertTrue(
        FastChromatogramBuilder.passesFilters(scans, intensities, scans.length, 5, 1E3, 2E3));
    // a single scan is enough with min consecutive scans of 1
    Assertions.assertTrue(
        FastChromatogramBuilder.passesFilters(scans, intensities, scans.length, 1, 1E3, 1E4));
    Assertions.assertFalse(
        FastChromatogramBuilder.passesFilters(scans, intensities, scans.length, 1, 1E3, 1E5));
  }

  /**
   * Random ions with realistic m/z errors plus random noise.
   */
  @NotNull
  static SyntheticLcmsData randomData(int numIons, int numScans, int noisePerScan, long seed) {
    final Random random = new Random(seed);
    final List<Ion> ions = new ArrayList<>();
    for (int i = 0; i < numIons; i++) {
      final double mz = 100 + random.nextDouble() * 900;
      final double apex = 10 + random.nextDouble() * (numScans - 20);
      final double sigma = 2 + random.nextDouble() * 4;
      final double height = Math.exp(Math.log(5E3) + random.nextDouble() * Math.log(1E3));
      ions.add(new Ion(mz, apex, sigma, height, 0.5 + random.nextDouble(), 0));
    }
    return SyntheticLcmsData.builder(numScans).ions(ions).noise(noisePerScan, 100, 1000, 1E2, 3E3)
        .detectionThreshold(5E2).maxErrorFactor(3).seed(seed).build();
  }

  private static void assertChromatogramsEqual(@NotNull List<BuiltChromatogram> expected,
      @NotNull List<BuiltChromatogram> actual) {
    Assertions.assertEquals(expected.size(), actual.size());
    for (int c = 0; c < expected.size(); c++) {
      final EvaluatedChromatogram a = EvaluatedChromatogram.of(expected.get(c));
      final EvaluatedChromatogram b = EvaluatedChromatogram.of(actual.get(c));
      Assertions.assertArrayEquals(a.scans(), b.scans());
      Assertions.assertArrayEquals(a.mzs(), b.mzs());
      Assertions.assertArrayEquals(a.intensities(), b.intensities());
    }
  }
}
