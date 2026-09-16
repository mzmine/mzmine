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

  private static final double EPS = 1e-6;

  /**
   * @return x values 0, 1, 2, ... matching the length of the intensities
   */
  private static double[] x(final double[] intensities) {
    final double[] x = new double[intensities.length];
    for (int i = 0; i < x.length; i++) {
      x[i] = i;
    }
    return x;
  }

  private static double fwhm(final double[] intensities) {
    return QualityParameters.calculateFWHM(x(intensities), intensities);
  }

  private static double tailingFactor(final double[] intensities) {
    return QualityParameters.calculateTailingFactor(x(intensities), intensities);
  }

  private static double asymmetryFactor(final double[] intensities) {
    return QualityParameters.calculateAsymmetryFactor(x(intensities), intensities);
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
  @DisplayName("neither flank reaches half maximum: extrapolate both outer flanks")
  void testBothFlanksExtrapolated() {
    final double[] plateau = {60, 80, 100, 80, 60};
    // left line 60@0 -> 80@1 reaches 50 at -0.5, right line 60@4 -> 80@3 reaches 50 at 4.5
    Assertions.assertEquals(5.0, fwhm(plateau), EPS);
  }

  @Test
  @DisplayName("extrapolating two nearly flat flanks reaches beyond the observed range")
  void testExtrapolationCanExceedObservedRange() {
    // documented consequence of extrapolating when neither flank reaches half maximum: the flatter
    // the outer flanks are, the further outside the observed range the crossings end up
    final double[] flat = {70, 80, 100, 80, 72, 80, 75};
    // left line 70@0 -> 80@1 reaches 50 at -2, right line 75@6 -> 80@5 reaches 50 at 11
    Assertions.assertEquals(13.0, fwhm(flat), EPS);
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
  @DisplayName("traces without a usable shape return NaN")
  void testDegenerateTraces() {
    Assertions.assertTrue(
        Double.isNaN(QualityParameters.calculateFWHM(new double[]{0, 1}, new double[]{100, 50})));
    Assertions.assertTrue(Double.isNaN(fwhm(new double[]{0, 0, 0, 0})));
    Assertions.assertTrue(
        Double.isNaN(QualityParameters.calculateFWHM(new double[]{0, 1, 2}, new double[]{0, 100})));
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
  @DisplayName("tailing and asymmetry factor are NaN when a flank is cut off")
  void testCutOffFlankGivesNaNForTailingAndAsymmetry() {
    // the left flank never drops to 5 % or 10 % of the apex
    final double[] cutOffLeft = {60, 80, 100, 50, 5, 0, 0};
    Assertions.assertTrue(Double.isNaN(tailingFactor(cutOffLeft)));
    Assertions.assertTrue(Double.isNaN(asymmetryFactor(cutOffLeft)));
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
