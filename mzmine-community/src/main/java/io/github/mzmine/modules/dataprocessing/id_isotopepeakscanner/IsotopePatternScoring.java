package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.Weights;
import io.github.mzmine.util.scans.similarity.impl.composite.CompositeCosineSpectralSimilarity;

public class IsotopePatternScoring {
  private IsotopePattern nDetectedPattern;
  private IsotopePattern nCalculatedPattern;

  /**
   *
   * @param detectedPattern
   * @param calculatedPattern
   * @param mzTolerance
   * @return isotope pattern similarity score
   * by comparing the normalised data points of the detected isotope pattern with the calculated (theoretical) pattern
   */

  public Double calculateIsotopeScore (IsotopePattern detectedPattern, IsotopePattern calculatedPattern, MZTolerance mzTolerance, double minHeight) {
    nDetectedPattern = normalisationOfIsotopePattern(detectedPattern, minHeight);
    nCalculatedPattern = normalisationOfIsotopePattern(calculatedPattern, minHeight);
    DataPoint[] detectedDataPoints = new DataPoint[nDetectedPattern.getNumberOfDataPoints()];
    for (int i = 0; i < detectedDataPoints.length; i++) {
      SimpleDataPoint dp = new SimpleDataPoint ( nDetectedPattern.getMzValue(i), nDetectedPattern.getIntensityValue(i));
      detectedDataPoints[i]=dp;
    }
    DataPoint[] nCalculatedDataPoints = new DataPoint[nCalculatedPattern.getNumberOfDataPoints()];
    for (int i = 0; i < nCalculatedDataPoints.length; i++) {
      SimpleDataPoint dp = new SimpleDataPoint ( nCalculatedPattern.getMzValue(i), nCalculatedPattern.getIntensityValue(i));
      nCalculatedDataPoints[i]=dp;
    }

    var similarityLibrary = CompositeCosineSpectralSimilarity.getSimilarity(Weights.SQRT, 0, HandleUnmatchedSignalOptions.KEEP_LIBRARY_SIGNALS, mzTolerance, 0,
        nCalculatedDataPoints, detectedDataPoints);
    double score = 0.0;
    if (similarityLibrary != null){
      score = similarityLibrary.getScore();
    }

    return score;
  }

  public IsotopePattern normalisationOfIsotopePattern (IsotopePattern pattern, double minHeight) {
    double [] mzs = new double [pattern.getNumberOfDataPoints()];
    double [] intensities = new double [pattern.getNumberOfDataPoints()];
    double [] normalisatedIntensities = new double [pattern.getNumberOfDataPoints()];
    double highestIntensity = 0;
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      intensities[i] = pattern.getIntensityValue(i);
      mzs[i] = pattern.getMzValue(i);
      if (intensities[i]> highestIntensity){
        highestIntensity = intensities[i];
      }
      if (intensities[i]< minHeight){
        intensities[i] =0;
      }
    }
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      normalisatedIntensities[i] = intensities[i]/highestIntensity;
    }
    IsotopePattern normalisatedPattern;
    if (pattern.getStatus()== IsotopePatternStatus.DETECTED) {
      normalisatedPattern = new SimpleIsotopePattern(mzs, normalisatedIntensities, pattern.getCharge(), IsotopePatternStatus.DETECTED,
          "");
    }
    else {
      normalisatedPattern = new SimpleIsotopePattern(mzs, normalisatedIntensities, pattern.getCharge(), IsotopePatternStatus.PREDICTED,
          "");
    }
    return normalisatedPattern;

  }

}
