/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.tools.msmsspectramerge;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.util.scans.ScanUtils;

import java.util.Arrays;

/**
 * Calculates some quality score given a MS/MS and its surrounding MS1
 */
public interface Ms2QualityScoreModel {

  /**
   * Calculate the quality score for all MS/MS scans within the fragment scan object
   * 
   * @param fragmentScan
   * @return a score which is higher for high quality MS/MS and negative for scans that should be
   *         removed
   */
  public double[] calculateQualityScore(FragmentScan fragmentScan);

  public static Ms2QualityScoreModel SelectByTIC = new Ms2QualityScoreModel() {
    @Override
    public double[] calculateQualityScore(FragmentScan fragmentScan) {
      final double[] scores = new double[fragmentScan.ms2ScanNumbers.length];
      Range<Double> mzRange =
          fragmentScan.feature.getMZ() < 75 ? Range.closed(50d, fragmentScan.feature.getMZ())
              : Range.closed(0d, fragmentScan.feature.getMZ() - 20d);
      for (int i = 0; i < scores.length; ++i) {
        double tic = ScanUtils
            .calculateTIC(fragmentScan.ms2ScanNumbers[i], mzRange);
        scores[i] = tic;
      }
      return scores;
    }
  };

  public static Ms2QualityScoreModel SelectBy20HighestPeaks = new Ms2QualityScoreModel() {
    @Override
    public double[] calculateQualityScore(FragmentScan fragmentScan) {
      final double[] scores = new double[fragmentScan.ms2ScanNumbers.length];
      Range<Double> mzRange =
          fragmentScan.feature.getMZ() < 75 ? Range.closed(50d, fragmentScan.feature.getMZ())
              : Range.closed(0d, fragmentScan.feature.getMZ());
      for (int i = 0; i < scores.length; ++i) {
        DataPoint[] spectrum = fragmentScan.ms2ScanNumbers[i].getMassList().getDataPoints();
        Arrays.sort(spectrum, (u, v) -> Double.compare(v.getIntensity(), u.getIntensity()));
        for (int j = 0; j < spectrum.length; ++j)
          scores[i] += spectrum[j].getIntensity();
      }
      return scores;
    }
  };

  public static Ms2QualityScoreModel SelectByMs1Intensity = new Ms2QualityScoreModel() {
    @Override
    public double[] calculateQualityScore(FragmentScan fragmentScan) {
      return fragmentScan.getInterpolatedPrecursorAndChimericIntensities()[0];
    }
  };

  public static Ms2QualityScoreModel SelectByLowChimericIntensityRelativeToMs1Intensity =
      new Ms2QualityScoreModel() {
        @Override
        public double[] calculateQualityScore(FragmentScan fragmentScan) {
          final double[] scores = new double[fragmentScan.ms2ScanNumbers.length];
          final double[][] interpolations =
              fragmentScan.getInterpolatedPrecursorAndChimericIntensities();
          for (int k = 0; k < scores.length; ++k) {
            if (interpolations[0][k] <= 0)
              continue;
            final double relative = Math.min(10, interpolations[0][k] / interpolations[1][k]);
            scores[k] = interpolations[0][k] * relative;
          }
          return scores;
        }
      };

}
