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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.ScanAlignment;
import org.jetbrains.annotations.Nullable;
import smile.math.DoubleArrayList;

/**
 * Currently unused code. Was intended for Isotope pattern alignment but we are using the
 * {@link ScanAlignment} now and regular spectral similarity
 */
@Deprecated
public class IsotopePatternMatcher {

  IsotopePattern predictedIsotopePattern;
  IsotopePattern foundPredictedIsotopePattern;
  DoubleArrayList allMeasuredMZValues;
  DoubleArrayList allMeasuredIntensities;
  DoubleArrayList mustBeDetectedMzs;
  DoubleArrayList mustBeDetectedIntensities;
  double[] measuredMZValues;
  double[] measuredIntensities;
  boolean[] mustBeDetectedMZValues;
  double[] predictedIntensities;
  double[] predictedMzs;
  double[] actualMZValues;
  double[] actualIntensities;
  float newScore;
  float actualScore;


  IsotopePatternMatcher(IsotopePattern isotopePattern, double minIntensity) {
    predictedIsotopePattern = isotopePattern;
    measuredMZValues = new double[isotopePattern.getNumberOfDataPoints()];
    measuredIntensities = new double[isotopePattern.getNumberOfDataPoints()];
    mustBeDetectedMZValues = new boolean[isotopePattern.getNumberOfDataPoints()];
    predictedIntensities = new double[isotopePattern.getNumberOfDataPoints()];
    predictedMzs = new double[isotopePattern.getNumberOfDataPoints()];
    mustBeDetectedIntensities = new DoubleArrayList();
    mustBeDetectedMzs = new DoubleArrayList();
    allMeasuredIntensities = new DoubleArrayList();
    allMeasuredMZValues = new DoubleArrayList();
    // Definition of mustBeDetected, if intensity is higher than minIntensity then mustBeDetected
    // is true
    for (int i = 0; i < isotopePattern.getNumberOfDataPoints(); i++) {
      if (predictedIsotopePattern.getIntensityValue(i) >= minIntensity) {
        mustBeDetectedMZValues[i] = true;
      }
    }
  }

  public boolean offerDataPoint(double measuredMz, double measuredIntensity,
      MZTolerance mzTolerance) {
    // Comparison of measuredMz & actualRt with MZvalues & RT of predictedIsotopePattern under
    // taking into account a MZ tolerance range, if the values correspond to each other,
    // return true and MZs & intensities are stored in measuredMZvalues & measuredIntensities,
    // if both matched
    for (int i = 0; i < predictedIsotopePattern.getNumberOfDataPoints(); i++) {
      if (mzTolerance.checkWithinTolerance(measuredMz, predictedIsotopePattern.getMzValue(i))) {
        allMeasuredMZValues.add(measuredMz);
        allMeasuredIntensities.add(measuredIntensity);
        measuredMZValues[i] = measuredMz;
        measuredIntensities[i] = measuredIntensity;
        predictedIntensities[i] = (predictedIsotopePattern.getIntensityValue(i));
        predictedMzs[i] = (predictedIsotopePattern.getMzValue(i));
        return true;
      }
    }
    return false;
  }


  public boolean matches() {
    // Check whether all values of mustBeDetected occur in measuredMZValues and whether RTs
    // match, then return true or false
    for (int i = 0; i < mustBeDetectedMZValues.length; i++) {
      if (mustBeDetectedMZValues[i]) {
        if (measuredIntensities[i] == 0d) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * @return The detected isotope pattern, if all necessary peaks were detected. Otherwise null. If
   * several signals are found (allMeasuredMZValues.size()>measuredMZValues.length), checks which
   * one fits best (highest score) and selects this as the detected isotope pattern.
   */
  @Nullable
  public IsotopePattern measuredIsotopePattern(MZTolerance mzTolerance) {
    if (!matches()) {
      return null;
    }
    if (allMeasuredMZValues.size() > measuredMZValues.length) {
      this.actualMZValues = measuredMZValues;
      this.actualIntensities = measuredIntensities;
      for (int i = 0; i < predictedIsotopePattern.getNumberOfDataPoints(); i++) {
        for (int j = 0; j < allMeasuredMZValues.size(); j++) {
          if (mzTolerance.checkWithinTolerance(allMeasuredMZValues.get(j),
              predictedIsotopePattern.getMzValue(i))) {
            IsotopePattern actualIsotopePattern = new SimpleIsotopePattern(measuredMZValues,
                measuredIntensities, predictedIsotopePattern.getCharge(),
                IsotopePatternStatus.DETECTED, predictedIsotopePattern.getDescription());
            actualScore = IsotopePatternScoreCalculator.getSimilarityScore(predictedIsotopePattern,
                actualIsotopePattern, mzTolerance, 300.0);
            actualMZValues[i] = allMeasuredMZValues.get(j);
            actualIntensities[i] = allMeasuredIntensities.get(j);
            IsotopePattern newIsotopePattern = new SimpleIsotopePattern(actualMZValues,
                actualIntensities, predictedIsotopePattern.getCharge(),
                IsotopePatternStatus.DETECTED, predictedIsotopePattern.getDescription());
            newScore = IsotopePatternScoreCalculator.getSimilarityScore(predictedIsotopePattern,
                newIsotopePattern, mzTolerance, 300.0);
            if (newScore > actualScore) {
              this.actualScore = newScore;
              this.measuredIntensities[i] = actualIntensities[i];
              this.measuredMZValues[i] = actualMZValues[i];
            }
          }
        }
      }
    }
//returns the detected isotope pattern signals above minIntensity (mustBeDetectedMZValues)

    DoubleArrayList mzs = new DoubleArrayList();
    DoubleArrayList intensities = new DoubleArrayList();
    for (int i = 0; i < measuredMZValues.length; i++) {
      if (mustBeDetectedMZValues[i]) {
        if (measuredIntensities[i] != 0 && measuredMZValues[i] != 0) {
          mzs.add(measuredMZValues[i]);
          intensities.add(measuredIntensities[i]);
        }
      }
    }

    return new SimpleIsotopePattern(mzs.toArray(), intensities.toArray(),
        predictedIsotopePattern.getCharge(), IsotopePatternStatus.DETECTED,
        predictedIsotopePattern.getDescription());
  }

  // calculation of the isotope pattern score
  public float score(MZTolerance mzTolerance) {
    if (measuredIsotopePattern(mzTolerance) == null) {
      return 0.0f;
    }
    DoubleArrayList mzsScore = new DoubleArrayList();
    DoubleArrayList predictedIntensitiesOfFoundMzs = new DoubleArrayList();
    for (int i = 0; i < measuredMZValues.length; i++) {
      if (mustBeDetectedMZValues[i]) {
        if (measuredIntensities[i] != 0 && measuredMZValues[i] != 0) {
          mzsScore.add(predictedMzs[i]);
          predictedIntensitiesOfFoundMzs.add(predictedIntensities[i]);
        }
      }
    }
    foundPredictedIsotopePattern = new SimpleIsotopePattern(mzsScore.toArray(),
        predictedIntensitiesOfFoundMzs.toArray(), predictedIsotopePattern.getCharge(),
        IsotopePatternStatus.PREDICTED, predictedIsotopePattern.getDescription());
    final float isotopePatternScore = IsotopePatternScoreCalculator.getSimilarityScore(
        foundPredictedIsotopePattern, measuredIsotopePattern(mzTolerance), mzTolerance, 100.0);
    return isotopePatternScore;
  }

  //unused so far:
  //Returns the theoretical isotope pattern to the detected signals:

  public IsotopePattern getTheoreticalIsotopePattern() {
    DoubleArrayList foundMzs = new DoubleArrayList();
    DoubleArrayList predictedIntensitiesOfFoundMzs = new DoubleArrayList();
    for (int i = 0; i < measuredMZValues.length; i++) {
      if (measuredIntensities[i] != 0 && measuredMZValues[i] != 0) {
        foundMzs.add(predictedMzs[i]);
        predictedIntensitiesOfFoundMzs.add(predictedIntensities[i]);
      }
    }
    return new SimpleIsotopePattern(foundMzs.toArray(), predictedIntensitiesOfFoundMzs.toArray(),
        predictedIsotopePattern.getCharge(), IsotopePatternStatus.PREDICTED,
        predictedIsotopePattern.getDescription());
  }
}



