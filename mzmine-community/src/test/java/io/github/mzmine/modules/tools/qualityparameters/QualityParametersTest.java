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

package io.github.mzmine.modules.tools.qualityparameters;

import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the peak shape quality parameters. All traces use a spacing of 1 on the x axis so that the
 * expected crossings can be calculated by hand.
 */
class QualityParametersTest {

  /**
   * the results are floats, so the tolerance has to stay above the float precision of the widest
   * trace used here
   */
  private static final double EPS = 1e-5;

  /**
   * @return x values 0, 1, 2, ... matching the length of the intensities
   */
  private static float[] x(final double[] intensities) {
    final float[] x = new float[intensities.length];
    for (int i = 0; i < x.length; i++) {
      x[i] = i;
    }
    return x;
  }

  private static PeakQuality quality(final double[] intensities) {
    return QualityParameters.calculateQualityParameters(x(intensities), intensities);
  }

  private static Float fwhm(final double[] intensities) {
    return quality(intensities).fwhm();
  }

  private static Float tailingFactor(final double[] intensities) {
    return quality(intensities).tailing();
  }

  private static Float asymmetryFactor(final double[] intensities) {
    return quality(intensities).asymmetry();
  }

  @Test
  @DisplayName("symmetric peak with data points exactly at half maximum")
  void testSymmetricPeak() {
    // crossings fall exactly on the data points at index 2 and 4
    Assertions.assertEquals(2.0, fwhm(new double[]{0, 25, 50, 100, 50, 25, 0}), EPS);
  }

  @Test
  @DisplayName("split peak: an interior valley below half maximum does not truncate the peak")
  void testSplitPeakValleyIsIgnored() {
    // valley of 30 at index 3 is below the half maximum of 50 but lies inside the peak
    final double[] split = {0, 20, 100, 30, 80, 20, 0};
    // left crossing between 20@1 and 100@2 = 1.375, right between 80@4 and 20@5 = 4.5
    Assertions.assertEquals(3.125, fwhm(split), EPS);

    // the same peak without the valley must give the same width
    final double[] filled = {0, 20, 100, 90, 80, 20, 0};
    Assertions.assertEquals(fwhm(filled), fwhm(split), EPS);
  }

  @Test
  @DisplayName("a single zero intensity data point inside the peak does not truncate it")
  void testInteriorZeroIsIgnored() {
    final double[] defect = {0, 20, 100, 0, 80, 20, 0};
    Assertions.assertEquals(3.125, fwhm(defect), EPS);
  }

  @Test
  @DisplayName("one flank never reaches half maximum: mirror the crossing flank")
  void testMissingFlankMirrorsCrossingFlank() {
    // left starts at 60 and never drops below 50, right crosses between 70@4 and 40@5 at 4.6667
    final double[] cutOffLeft = {60, 80, 100, 90, 70, 40, 0};
    final double rightHalfWidth = 4.0 + 2.0 / 3.0 - 2.0;
    // the crossing flank (2.6667) is wider than the observed left flank (2.0), so it wins
    Assertions.assertEquals(2 * rightHalfWidth, fwhm(cutOffLeft), EPS);
  }

  @Test
  @DisplayName("one flank never reaches half maximum: keep the wider observed width")
  void testMissingFlankKeepsObservedWidth() {
    // left starts at 60 and never drops below 50, apex at index 4
    final double[] cutOffLeft = {60, 65, 70, 80, 100, 40, 0};
    // right crossing between 100@4 and 40@5 = 4.83333 -> half width 0.83333
    // observed left half width = 4.0 and is larger, so the mirrored side is clamped to the data
    Assertions.assertEquals(4.0 + 5.0 / 6.0, fwhm(cutOffLeft), EPS);
  }

  @Test
  @DisplayName("neither flank reaches half maximum: extrapolate both flanks")
  void testBothFlanksExtrapolated() {
    final double[] plateau = {60, 80, 100, 80, 60};
    // both flanks are straight lines with a slope of 20, so they reach 50 at -0.5 and 4.5
    Assertions.assertEquals(5.0, fwhm(plateau), EPS);
  }

  @Test
  @DisplayName("the extrapolated slope is fitted over the whole flank, not over the edge segment")
  void testExtrapolationUsesWholeFlank() {
    // the outermost segment 60@0 -> 61@1 is almost flat and alone would extrapolate to -10, far
    // enough out to hit the cap. The least squares fit over the whole flank 60, 61, 100 has a
    // slope of 20 and reaches 50 at -11/60, well inside the cap.
    final double[] noisyEdge = {60, 61, 100, 61, 60};
    Assertions.assertEquals(2 * (2.0 + 11.0 / 60.0), fwhm(noisyEdge), EPS);
  }

  @Test
  @DisplayName("extrapolation of a nearly flat flank is capped at twice the observed half width")
  void testExtrapolationIsCapped() {
    final double[] flat = {70, 80, 100, 80, 72, 80, 75};
    // left flank fit reaches 50 at -11/9, a half width of 29/9 that stays below the cap of 2 * 2
    // right flank fit reaches 50 at 10.28, a half width of 8.28 that is capped to 2 * 4 -> x = 10
    Assertions.assertEquals(10.0 + 11.0 / 9.0, fwhm(flat), EPS);
  }

  @Test
  @DisplayName("the FWHM never exceeds twice the observed range of the peak")
  void testFwhmIsBoundedByTwiceTheObservedRange() {
    final double[][] traces = {{0, 25, 50, 100, 50, 25, 0}, {0, 20, 100, 30, 80, 20, 0},
        {60, 80, 100, 90, 70, 40, 0}, {60, 65, 70, 80, 100, 40, 0}, {60, 80, 100, 80, 60},
        {70, 80, 100, 80, 72, 80, 75}, {99, 99, 100, 99, 99}, {100, 60, 40, 20, 0},
        {60, 61, 62, 100, 50, 20, 5}};
    for (final double[] trace : traces) {
      final double observedRange = trace.length - 1.0;
      final Float width = fwhm(trace);
      Assertions.assertNotNull(width, "no width for a usable trace");
      Assertions.assertTrue(width <= 2 * observedRange + EPS,
          "width %s exceeds twice the observed range %s".formatted(width, observedRange));
    }
  }

  @Test
  @DisplayName("a flank that never descends below 85 % of the apex is not extrapolated")
  void testFlatFlanksUseTheDataRange() {
    // both flanks stay above 85 % of the apex, so the width is limited to the observed data
    Assertions.assertEquals(4.0, fwhm(new double[]{99, 99, 100, 99, 99}), EPS);
    Assertions.assertEquals(4.0, fwhm(new double[]{90, 95, 100, 95, 90}), EPS);
    // exactly at the limit still counts as flat
    Assertions.assertEquals(4.0, fwhm(new double[]{85, 92, 100, 92, 85}), EPS);
  }

  @Test
  @DisplayName("a flank that descends below 85 % of the apex is still extrapolated")
  void testFlanksBelowTheFlatLimitAreExtrapolated() {
    // one intensity unit deeper than the flat limit, so both flanks are extrapolated and hit the
    // cap of twice the observed half width
    Assertions.assertEquals(8.0, fwhm(new double[]{84, 92, 100, 92, 84}), EPS);
  }

  @Test
  @DisplayName("only the flat flank is limited to the data, the other one is extrapolated")
  void testFlatFlankMixedWithExtrapolatedFlank() {
    // left flank 90, 95, 100 stays above 85 % and ends at the first data point 0
    // right flank 100, 80, 60 is fitted with a slope of -20 and reaches 50 at 4.5
    final double[] mixed = {90, 95, 100, 80, 60};
    Assertions.assertEquals(4.5, fwhm(mixed), EPS);
  }

  @Test
  @DisplayName("neither flank reaches half maximum and the outer flanks do not rise: use the data edges")
  void testBothFlanksFallBackToEdges() {
    // apex is the first data point, both outer segments fall towards the inside
    final double[] noRise = {100, 60, 80, 60, 100};
    Assertions.assertEquals(4.0, fwhm(noRise), EPS);
  }

  @Test
  @DisplayName("a flat shoulder above half maximum is not extrapolated out of the peak")
  void testFlatShoulderIsNotExtrapolated() {
    // this trace made the previous implementation extrapolate far outside the feature
    final double[] shoulder = {60, 61, 62, 100, 50, 20, 5};
    // right crosses exactly at 50@4, half width 1, observed left half width 3 is larger
    Assertions.assertEquals(4.0, fwhm(shoulder), EPS);
    // the width stays within the observed retention time range of 6
    Assertions.assertTrue(fwhm(shoulder) <= 6.0);
  }

  @Test
  @DisplayName("apex on the first data point")
  void testApexAtFirstDataPoint() {
    final double[] cutOff = {100, 60, 40, 20, 0};
    // right crossing between 60@1 and 40@2 = 1.5, left flank has no observed width
    Assertions.assertEquals(3.0, fwhm(cutOff), EPS);
  }

  @Test
  @DisplayName("outermost data point sits exactly on the threshold")
  void testCrossingOnOutermostDataPoint() {
    final double[] exact = {50, 80, 100, 80, 50};
    Assertions.assertEquals(4.0, fwhm(exact), EPS);
  }

  @Test
  @DisplayName("traces without a usable shape return null")
  void testDegenerateTraces() {
    // too few data points and no intensity at all
    Assertions.assertNull(
        QualityParameters.calculateQualityParameters(new float[]{0, 1}, new double[]{100, 50})
            .fwhm());
    Assertions.assertNull(fwhm(new double[]{0, 0, 0, 0}));
    // x and intensities of different length is a programming error, not a degenerate peak
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> QualityParameters.calculateQualityParameters(new float[]{0, 1, 2},
            new double[]{0, 100}));
  }

  @Test
  @DisplayName("tailing and asymmetry factor of a symmetric peak are 1")
  void testSymmetricTailingAndAsymmetry() {
    final double[] symmetric = {0, 5, 50, 100, 50, 5, 0};
    Assertions.assertEquals(1.0, tailingFactor(symmetric), EPS);

    final double[] symmetricAt10 = {0, 10, 50, 100, 50, 10, 0};
    Assertions.assertEquals(1.0, asymmetryFactor(symmetricAt10), EPS);
  }

  @Test
  @DisplayName("asymmetry factor of a tailing peak")
  void testTailingPeakAsymmetry() {
    // 10 % crossings at 1.0 and 5.0, apex at 2
    final double[] tailing = {0, 10, 100, 60, 30, 10, 0};
    Assertions.assertEquals(3.0, asymmetryFactor(tailing), EPS);
  }

  @Test
  @DisplayName("tailing and asymmetry factor are null when a flank is cut off")
  void testCutOffFlankGivesNullForTailingAndAsymmetry() {
    // the left flank never drops to 5 % or 10 % of the apex
    final double[] cutOffLeft = {60, 80, 100, 50, 5, 0, 0};
    Assertions.assertNull(tailingFactor(cutOffLeft));
    Assertions.assertNull(asymmetryFactor(cutOffLeft));
    // the FWHM is still reported because the missing flank is mirrored
    Assertions.assertEquals(3.0, fwhm(cutOffLeft), EPS);
  }

  @Test
  @DisplayName("mobility FWHM uses the same crossing search")
  void testMobilityFwhm() {
    final double[] mobilities = {0.8, 0.9, 1.0, 1.1, 1.2};
    final double[] intensities = {0, 50, 100, 50, 0};
    final SummedIntensityMobilitySeries series = new SummedIntensityMobilitySeries(null, mobilities,
        intensities);
    // crossings sit exactly on the data points at 0.9 and 1.1
    Assertions.assertEquals(0.2f, QualityParameters.calculateFWHM(series), 1e-5f);
  }
}
