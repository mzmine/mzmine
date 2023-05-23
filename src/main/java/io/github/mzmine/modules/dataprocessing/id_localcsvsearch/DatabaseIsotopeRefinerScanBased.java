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

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.Weights;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class DatabaseIsotopeRefinerScanBased {


  private static final Logger logger = Logger.getLogger(
      DatabaseIsotopeRefinerScanBased.class.getName());

  /**
   * Apply isotope scoring to filter annotations. Uses cosine similarity to match predicted and
   * measured isotope pattern. Apply isotope finder first
   *
   * @param rows            apply to all rows
   * @param mzTolerance     tolerance for signal matching
   * @param minIntensity    minimum isotope intensity for prediction
   * @param minIsotopeScore minimum isotope score to retain annotations
   */
  public static void refineAnnotationsByIsotopes(List<FeatureListRow> rows, MZTolerance mzTolerance,
      double minIntensity, double minIsotopeScore) {
    for (final FeatureListRow row : rows) {
      refineAnnotationsByIsotopes(row, mzTolerance, minIntensity, minIsotopeScore);
    }
  }

  /**
   * Apply isotope scoring to filter annotations. Uses cosine similarity to match predicted and
   * measured isotope pattern. Apply isotope finder first
   *
   * @param row             apply to this row
   * @param mzTolerance     tolerance for signal matching
   * @param minIntensity    minimum isotope intensity for prediction
   * @param minIsotopeScore minimum isotope score to retain annotations
   */
  public static void refineAnnotationsByIsotopes(FeatureListRow row, MZTolerance mzTolerance,
      double minIntensity, double minIsotopeScore) {
    refineAnnotationsByIsotopes(row, mzTolerance, minIntensity, minIsotopeScore, new HashMap<>());
  }

  /**
   * Apply isotope scoring to filter annotations. Uses cosine similarity to match predicted and
   * measured isotope pattern. Apply isotope finder first
   *
   * @param row                  apply to this row
   * @param mzTolerance          tolerance for signal matching
   * @param minIntensity         minimum isotope intensity for prediction
   * @param minIsotopeScore      minimum isotope score to retain annotations
   * @param ionIsotopePatternMap map to cache ions formula to predicted patterns
   */
  public static void refineAnnotationsByIsotopes(FeatureListRow row, MZTolerance mzTolerance,
      double minIntensity, double minIsotopeScore,
      Map<IMolecularFormula, IsotopePattern> ionIsotopePatternMap) {
    var measuredPattern = row.getBestIsotopePattern();
    if (measuredPattern == null) {
      return;
    }

    var measuredIsotopes = ScanUtils.extractDataPoints(measuredPattern);
    var remainingAnnotations = row.getCompoundAnnotations().stream().filter(annotation ->
        calculateIsotopeScore(annotation, measuredIsotopes, mzTolerance, minIntensity,
            ionIsotopePatternMap) >= minIsotopeScore).toList();
    row.setCompoundAnnotations(remainingAnnotations);
  }

  /**
   * @param annotation           annotation with formula and ion
   * @param measuredIsotopes     all measured isotope signals from an IsotopePattern
   * @param mzTolerance          matching tolerance
   * @param minIntensity         minimum isotope intensity for prediction
   * @param ionIsotopePatternMap map to cache ions formula to predicted patterns
   * @return the isotope pattern score or 0 on error or if no pattern was detected
   */
  private static double calculateIsotopeScore(final CompoundDBAnnotation annotation,
      final DataPoint[] measuredIsotopes, final MZTolerance mzTolerance, final double minIntensity,
      final Map<IMolecularFormula, IsotopePattern> ionIsotopePatternMap) {
    var adductType = annotation.getAdductType();
    if (annotation.getFormula() == null || adductType == null) {
      return 0;
    }
    // create ion formula
    IMolecularFormula ionFormula = FormulaUtils.getIonizedFormula(annotation);
    assert ionFormula != null;
    // cache the ionformula to IsotopePattern to reuse isotope patterns for the same formula
    try {
      final IsotopePattern predictedIsotopePattern = ionIsotopePatternMap.computeIfAbsent(ionFormula,
          key -> getCalculateIsotopePattern(mzTolerance, minIntensity, adductType, ionFormula));
//        predictedIsotopePattern = IsotopePatternCalculator.removeDataPointsBelowIntensity(predictedIsotopePattern,
//            minIntensity);
      var predictedIsotopes = ScanUtils.extractDataPoints(predictedIsotopePattern);

      var similarity = SpectralSimilarityFunction.compositeCosine.getSimilarity(Weights.SQRT, 0,
          HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO, mzTolerance, 0, predictedIsotopes,
          measuredIsotopes);
      // also match with library as ground truth to give more weight to predicted signals
      var similarityLibrary = SpectralSimilarityFunction.compositeCosine.getSimilarity(Weights.SQRT, 0,
          HandleUnmatchedSignalOptions.KEEP_LIBRARY_SIGNALS, mzTolerance, 0, predictedIsotopes,
          measuredIsotopes);

      if (similarity != null && similarityLibrary!=null) {
        var score = (float) (similarity.getScore() + similarityLibrary.getScore()*2)/3f;
        annotation.put(IsotopePatternScoreType.class, score);
        return similarity.getScore();
      }
      return 0;
    }catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot match isotope pattern similarity. Maybe no adduct, formula information", ex);
      return 0;
    }
  }

  @NotNull
  private static IsotopePattern getCalculateIsotopePattern(final MZTolerance mzTolerance,
      final double minIntensity, final IonType adductType, final IMolecularFormula ionFormula) {
    return IsotopePatternCalculator.calculateIsotopePattern(ionFormula, minIntensity,
        mzTolerance.getMzToleranceForMass(FormulaUtils.calculateMzRatio(ionFormula)),
        adductType.getCharge(), adductType.getPolarity(), false);
  }

}



