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

package io.github.mzmine.modules.dataprocessing.align_path.scorer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.align_path.PathAlignerParameters;
import io.github.mzmine.modules.dataprocessing.align_path.functions.AlignmentPath;
import io.github.mzmine.modules.dataprocessing.align_path.functions.ScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.RangeUtils;

public class RTScore implements ScoreCalculator {

  private final static double WORST_SCORE = Double.MAX_VALUE;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;

  public double calculateScore(AlignmentPath path, FeatureListRow peak, ParameterSet parameters) {
    try {
      rtTolerance = parameters.getParameter(PathAlignerParameters.RTTolerance).getValue();
      mzTolerance = parameters.getParameter(PathAlignerParameters.MZTolerance).getValue();
      Range<Float> rtRange = rtTolerance.getToleranceRange((float) path.getRT());
      Range<Double> mzRange = mzTolerance.getToleranceRange(path.getMZ());

      if (!rtRange.contains(peak.getAverageRT()) || !mzRange.contains(peak.getAverageMZ())) {
        return WORST_SCORE;
      }

      double mzDiff = Math.abs(path.getMZ() - peak.getAverageMZ());

      double rtDiff = Math.abs(path.getRT() - peak.getAverageRT());

      double score = ((mzDiff / (RangeUtils.rangeLength(mzRange) / 2.0))) + ((rtDiff / (
          RangeUtils.rangeLength(rtRange) / 2.0)));

      if (parameters.getParameter(PathAlignerParameters.SameChargeRequired).getValue()) {
        if (!FeatureUtils.compareChargeState(path.convertToAlignmentRow(0), peak)) {
          return WORST_SCORE;
        }
      }

      if (parameters.getParameter(PathAlignerParameters.SameIDRequired).getValue()) {
        if (!FeatureUtils.compareIdentities(path.convertToAlignmentRow(0), peak)) {
          return WORST_SCORE;
        }
      }

      if (parameters.getParameter(PathAlignerParameters.compareIsotopePattern).getValue()) {
        IsotopePattern ip1 = path.convertToAlignmentRow(0).getBestIsotopePattern();
        IsotopePattern ip2 = peak.getBestIsotopePattern();

        if ((ip1 != null) && (ip2 != null)) {
          boolean compareIsotopePattern = parameters
              .getParameter(JoinAlignerParameters.compareIsotopePattern).getValue();
          final ParameterSet isoParam = parameters
              .getParameter(JoinAlignerParameters.compareIsotopePattern).getEmbeddedParameters();

          Double minIsotopeScore = isoParam
              .getValue(IsotopePatternScoreParameters.isotopePatternScoreThreshold);
          Double isotopeNoiseLevel = isoParam
              .getValue(IsotopePatternScoreParameters.isotopeNoiseLevel);
          MZTolerance isotopeMZTolerance = isoParam
              .getValue(IsotopePatternScoreParameters.mzTolerance);

          if (compareIsotopePattern && !IsotopePatternScoreCalculator
              .checkMatch(ip1, ip2, isotopeMZTolerance, isotopeNoiseLevel, minIsotopeScore)) {
            return WORST_SCORE;
          }
        }
      }

      return score;
    } catch (NullPointerException e) {
      e.printStackTrace();
      return WORST_SCORE;
    }
  }

  public boolean matches(AlignmentPath path, FeatureListRow peak, ParameterSet parameters) {
    rtTolerance = parameters.getParameter(PathAlignerParameters.RTTolerance).getValue();
    mzTolerance = parameters.getParameter(PathAlignerParameters.MZTolerance).getValue();
    Range<Float> rtRange = rtTolerance.getToleranceRange((float) path.getRT());
    Range<Double> mzRange = mzTolerance.getToleranceRange(path.getMZ());

    if (!rtRange.contains(peak.getAverageRT()) || !mzRange.contains(peak.getAverageMZ())) {
      return false;
    }
    return true;
  }

  public double getWorstScore() {
    return WORST_SCORE;
  }

  public boolean isValid(FeatureListRow peak) {
    return true;
  }

  public String name() {
    return "Uses MZ and retention time";
  }
}
