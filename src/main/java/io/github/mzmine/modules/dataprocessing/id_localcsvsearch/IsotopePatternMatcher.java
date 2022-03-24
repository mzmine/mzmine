package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.Arrays;
import java.util.OptionalDouble;
import org.jetbrains.annotations.Nullable;
import smile.math.DoubleArrayList;

public class IsotopePatternMatcher {

  IsotopePattern predictedIsotopePattern;
  IsotopePattern foundPredictedIsotopePattern;
  DoubleArrayList allMeasuredMZValues;
  DoubleArrayList allMeasuredIntensities;
  DoubleArrayList mustBeDetectedMzs;
  DoubleArrayList mustBeDetectedIntensities;
  double[] measuredMZValues;
  double[] measuredIntensities;
  double[] measuredRTs;
  boolean[] mustBeDetectedMZValues;
  double[] predictedIntensities;
  double[] predictedMzs;
  double[] actualMZValues;
  double[] actualIntensities;
  float newScore;
  float actualScore;
  DoubleArrayList mzs;
  DoubleArrayList predictedIntensitiesOfFoundMzs;


  IsotopePatternMatcher(IsotopePattern isotopePattern, double minIntensity) {
    predictedIsotopePattern = isotopePattern;
    measuredMZValues = new double[isotopePattern.getNumberOfDataPoints()];
    measuredIntensities = new double[isotopePattern.getNumberOfDataPoints()];
    mustBeDetectedMZValues = new boolean[isotopePattern.getNumberOfDataPoints()];
    measuredRTs = new double[isotopePattern.getNumberOfDataPoints()];
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

  public boolean offerDataPoint(double measuredMz, double measuredIntensity, float actualRt,
      float predictedRt, MZTolerance mzTolerance, RTTolerance rtTolerance) {
    // Vergleich measuredMz bzw actualRt mit MZvalues bzw. RTs of predictedIsotopePattern unter
    // berücksichtigung eines MZ Toleranzbereiches, wenn die Werte einander entsprechen,
    // return true und MZs& Intensities werden in measuredMZvalues&measuredIntensities gespeichert,
    // wenn beides matched
    for (int i = 0; i < predictedIsotopePattern.getNumberOfDataPoints(); i++) {
      if (mzTolerance.checkWithinTolerance(measuredMz, predictedIsotopePattern.getMzValue(i))) {
        if (rtTolerance.checkWithinTolerance(actualRt, predictedRt)) {
          allMeasuredMZValues.add(measuredMz);
          allMeasuredIntensities.add(measuredIntensity);
          measuredMZValues[i] = measuredMz;
          measuredIntensities[i] = measuredIntensity;
          measuredRTs[i] = actualRt;
          predictedIntensities[i] = (predictedIsotopePattern.getIntensityValue(i));
          predictedMzs[i] = (predictedIsotopePattern.getMzValue(i));
          return true;

        }
      }
    }
    return false;
  }


  public boolean matches() {
    // Überprüfe ob alle Werte von mustBeDetected in measuredMZValues vorkommen und ob RTs
    // übereinstimmen, dann return true oder false
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
   * @return The detected isotope pattern, if all necessary peaks were detected. Otherwise null.
   */
  @Nullable
  public IsotopePattern measuredIsotopePattern(MZTolerance mzTolerance) {
    if (!matches()) {
      return null;
    }
    //IsotopePattern actualIsotopePattern = new SimpleIsotopePattern(measuredMZValues,
    //    measuredIntensities, IsotopePatternStatus.DETECTED,
    //    predictedIsotopePattern.getDescription());
    //actualScore = IsotopePatternScoreCalculator.getSimilarityScore(predictedIsotopePattern,
    //    actualIsotopePattern, mzTolerance, 300.0);
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
              actualIntensities, predictedIsotopePattern.getCharge(), IsotopePatternStatus.DETECTED,
              predictedIsotopePattern.getDescription());
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

  public IsotopePattern getFoundPredictedIsotopePattern() {
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
    //new isotopePattern = measuredIsotopePattern
    //List<double> measuredMZValuesList = Arrays.asList(measuredMZValues);
    //DatasetBuilder measuredIsotopePattern = new Dataset (measuredMZValues, measuredIntensities);
    //MassSpectrum measuredIsotopeMassSpectrum = new MassSpectrum (measuredMZValuesList, measuredIntensities);
    //IsotopePattern measuredIsotopePatter = new IsotopePattern (measuredIsotopeMassSpectrum);

  }
}



