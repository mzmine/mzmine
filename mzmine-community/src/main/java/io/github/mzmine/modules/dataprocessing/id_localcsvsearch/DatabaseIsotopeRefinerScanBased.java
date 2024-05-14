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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.IsotopePeakScannerModule;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.IsotopePeakScannerParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.Comparators;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import io.github.mzmine.util.scans.similarity.Weights;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class DatabaseIsotopeRefinerScanBased extends AbstractTask {


  private static final Logger logger = Logger.getLogger(
      DatabaseIsotopeRefinerScanBased.class.getName());
  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   *                       {@link
   *                       MZmineRunnableModule(MZmineProject,
   *                       ParameterSet, Instant)}.
   * @param moduleCallDate
   */
  protected DatabaseIsotopeRefinerScanBased(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
  }

  @Override
  public Instant getModuleCallDate() {
    return super.getModuleCallDate();
  }
  public class ModuleDate {
    public void getModuleCallDate (){
      getModuleCallDate();
    }
  }
  private final ModuleDate date = new ModuleDate();


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
    Map<IMolecularFormula, IsotopePattern> ionIsotopePatternMap = new HashMap<>();
    for (final FeatureListRow row : rows) {
      refineAnnotationsByIsotopes(row, mzTolerance, minIntensity, minIsotopeScore,
          ionIsotopePatternMap);
    }
  }

  public void refineAnnotationsByIsotopesDifferentResolutions(ParameterSet parameters,
      MZmineProject project, FeatureList peakList, List<FeatureListRow> rows,
      MZTolerance mzTolerance, io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance rtTolerance, double minIntensity, double minIsotopeScore, boolean removeIsotopes, String suffix) {
    TreeMap<Integer, FeatureListRow> reducedList = new TreeMap<>();
    Map<IMolecularFormula, Map<Double, IsotopePattern>> ionIsotopePatternMap = new HashMap<>();
    for ( FeatureListRow row : rows){
      reducedList.put(row.getID(),row);
    }

    for ( FeatureListRow row : rows) {
      refineAnnotationsByIsotopesDifferentResolutions(row, mzTolerance, minIntensity,
          minIsotopeScore, ionIsotopePatternMap);
    }
    for ( FeatureListRow row : rows) {
      if (removeIsotopes){
        reducedList = removeIsotopes(reducedList, row, rtTolerance);
      }
    }
    ModularFeatureList resultPeakList = new ModularFeatureList(peakList.getName() + " " + suffix,getMemoryMapStorage(),peakList.getRawDataFiles() );
    for (Integer key : reducedList.keySet()) {
      ModularFeatureListRow bestRow = new ModularFeatureListRow(resultPeakList, key, (ModularFeatureListRow) reducedList.get(key), true);
      resultPeakList.addRow(bestRow);
    }
    addResultToProject(resultPeakList, project, parameters);

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
    // patterns might be split by charge
    Int2ObjectMap<DataPoint[]> chargeIsotopeMap = new Int2ObjectArrayMap<>();
    if (measuredPattern instanceof MultiChargeStateIsotopePattern multiPattern) {
      for (final IsotopePattern pattern : multiPattern.getPatterns()) {
        chargeIsotopeMap.put(Math.abs(pattern.getCharge()), ScanUtils.extractDataPoints(pattern));
      }
    } else if (measuredPattern != null) {
      chargeIsotopeMap.put(Math.abs(measuredPattern.getCharge()),
          ScanUtils.extractDataPoints(measuredPattern));
    }

    // in case no isotope pattern was detected for a charge state - use MS1 scan
    var ms1Scan = row.getBestFeature().getRepresentativeScan();
    final DataPoint[] ms1DefaultPattern;
    if (ms1Scan != null) {
      final var ms1DefaultScan = ms1Scan.getMassList();
      if (ms1DefaultScan == null) {
        throw new MissingMassListException(ms1Scan);
      }
      ms1DefaultPattern = ScanUtils.extractDataPoints(ms1DefaultScan);
    } else {
      ms1DefaultPattern = null;
    }

    var remainingAnnotations = new ArrayList<>(
        row.getCompoundAnnotations().stream().filter(annotation -> {
          var adduct = annotation.getAdductType();
          // >=1 charge
          var absCharge = Math.max(1, adduct != null ? adduct.getAbsCharge() : 1);
          var measuredIsotopes = chargeIsotopeMap.getOrDefault(absCharge, null);
          if (measuredIsotopes == null) {
            // replace missing pattern by MS1 scan
            measuredIsotopes = ms1DefaultPattern;
          }
          if (measuredIsotopes == null) {
            // no isotope pattern and no default MS1 scan. e.g., if just a feature list loaded
            return false;
          }
          var score = calculateIsotopeScore(annotation, measuredIsotopes, mzTolerance, minIntensity,
              ionIsotopePatternMap);
          return score >= minIsotopeScore;
        }).sorted(Comparator.comparing(CompoundDBAnnotation::getIsotopePatternScore,
            Comparators.scoreDescending())).toList());

    row.setCompoundAnnotations(remainingAnnotations);
  }

  public static void refineAnnotationsByIsotopesDifferentResolutions(FeatureListRow row,
      MZTolerance mzTolerance, double minIntensity, double minIsotopeScore,
      Map<IMolecularFormula, Map<Double, IsotopePattern>> ionIsotopePatternMap) {
    var measuredPattern = row.getBestIsotopePattern();
    // patterns might be split by charge
    Int2ObjectMap<DataPoint[]> chargeIsotopeMap = new Int2ObjectArrayMap<>();
    if (measuredPattern instanceof MultiChargeStateIsotopePattern multiPattern) {
      for (final IsotopePattern pattern : multiPattern.getPatterns()) {
        chargeIsotopeMap.put(Math.abs(pattern.getCharge()), ScanUtils.extractDataPoints(pattern));
      }
    } else if (measuredPattern != null) {
      chargeIsotopeMap.put(Math.abs(measuredPattern.getCharge()),
          ScanUtils.extractDataPoints(measuredPattern));
    }

    // in case no isotope pattern was detected for a charge state - use MS1 scan
    var ms1Scan = row.getBestFeature().getRepresentativeScan();
    final DataPoint[] ms1DefaultPattern;
    if (ms1Scan != null) {
      final var ms1DefaultScan = ms1Scan.getMassList();
      if (ms1DefaultScan == null) {
        throw new MissingMassListException(ms1Scan);
      }
      ms1DefaultPattern = ScanUtils.extractDataPoints(ms1DefaultScan);
    } else {
      ms1DefaultPattern = null;
    }

    var remainingAnnotations = new ArrayList<>(
        row.getCompoundAnnotations().stream().filter(annotation -> {
          var adduct = annotation.getAdductType();
          // >=1 charge
          var absCharge = Math.max(1, adduct != null ? adduct.getAbsCharge() : 1);
          var measuredIsotopes = chargeIsotopeMap.getOrDefault(absCharge, null);
          if (measuredIsotopes == null) {
            // replace missing pattern by MS1 scan
            measuredIsotopes = ms1DefaultPattern;
          }
          if (measuredIsotopes == null) {
            // no isotope pattern and no default MS1 scan. e.g., if just a feature list loaded
            return false;
          }
          var score = calculateIsotopeScoreDifferentResolutions(annotation, measuredIsotopes, minIntensity,
              ionIsotopePatternMap);
          return score >= minIsotopeScore;
        }).sorted(Comparator.comparing(CompoundDBAnnotation::getIsotopePatternScore,
            Comparators.scoreDescending())).toList());

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
      final IsotopePattern predictedIsotopePattern = ionIsotopePatternMap.computeIfAbsent(
          ionFormula,
          key -> IsotopePatternCalculator.calculateIsotopePattern(ionFormula, minIntensity,
              mzTolerance.getMzToleranceForMass(FormulaUtils.calculateMzRatio(ionFormula)),
              adductType.getCharge(), adductType.getPolarity(), false));
      var predictedIsotopes = ScanUtils.extractDataPoints(predictedIsotopePattern);

      // also match with library as ground truth to give more weight to predicted signals
      var similarityLibrary = SpectralSimilarityFunction.compositeCosine.getSimilarity(Weights.SQRT,
          0, HandleUnmatchedSignalOptions.KEEP_LIBRARY_SIGNALS, mzTolerance, 0, predictedIsotopes,
          measuredIsotopes);

      if (similarityLibrary != null) {
        annotation.put(IsotopePatternScoreType.class, (float)similarityLibrary.getScore());
        return similarityLibrary.getScore();
      }
      return 0;
    } catch (Exception ex) {
      logger.log(Level.WARNING,
          "Cannot match isotope pattern similarity. Maybe no adduct, formula information", ex);
      return 0;
    }
  }

  private static double calculateIsotopeScoreDifferentResolutions(final CompoundDBAnnotation annotation,
      final DataPoint[] measuredIsotopes, final double minIntensity,
      final Map<IMolecularFormula, Map <Double,IsotopePattern>> ionIsotopePatternMap) {
    var adductType = annotation.getAdductType();
    if (annotation.getFormula() == null || adductType == null) {
      return 0;
    }
    // create ion formula
    IMolecularFormula ionFormula = FormulaUtils.getIonizedFormula(annotation);
    assert ionFormula != null;
    // cache the ionformula to IsotopePattern to reuse isotope patterns for the same formula
    float finalScore = 0;
    ionIsotopePatternMap.computeIfAbsent(ionFormula, key -> IsotopePatternCalculator.calculateIsotopePatternForResolutions(ionFormula, minIntensity,
        MZTolerance.getDefaultResolutions(), adductType.getCharge(), adductType.getPolarity(), false));
    for (MZTolerance mzTol : MZTolerance.getDefaultResolutions()) {
      try {
        IsotopePattern predictedIsotopePattern = ionIsotopePatternMap.get(ionFormula).get(mzTol.getMzTolerance());
        var predictedIsotopes = ScanUtils.extractDataPoints(predictedIsotopePattern);
        var similarity = SpectralSimilarityFunction.compositeCosine.getSimilarity(Weights.SQRT, 0,
            HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO,mzTol, 0,
            predictedIsotopes, measuredIsotopes);
        // also match with library as ground truth to give more weight to predicted signals
        var similarityLibrary = SpectralSimilarityFunction.compositeCosine.getSimilarity(
            Weights.SQRT, 0, HandleUnmatchedSignalOptions.KEEP_LIBRARY_SIGNALS, mzTol, 0,
            predictedIsotopes, measuredIsotopes);

        if (similarity != null && similarityLibrary != null) {
          var score = (float) (similarity.getScore() + similarityLibrary.getScore() * 2) / 3f;
          if (score >= finalScore) {
            finalScore = score;
          }
        }

      } catch (Exception ex) {
        logger.log(Level.WARNING,
            "Cannot match isotope pattern similarity. Maybe no adduct, formula information", ex);
      }
    }
    annotation.put(IsotopePatternScoreType.class, finalScore);
    return finalScore;
  }

  // Removes the features of the isotopic signals from database matches, so that only the
  // monoisotopic signals of the found components are listed
  public static TreeMap <Integer, FeatureListRow> removeIsotopes  (TreeMap <Integer, FeatureListRow> rows, FeatureListRow row, RTTolerance RTTolerance) {
    TreeMap <Integer, FeatureListRow > newList = new TreeMap <>();

    if(row.getBestFeature().getIsotopePattern() ==null || row.getCompoundAnnotations().isEmpty()){
      return rows;
    }

    for (int actualRow: rows.keySet()){
      if (row.getBestFeature().getIsotopePattern().getDataPointMZRange().contains(rows.get(actualRow).getAverageMZ())&&
          RTTolerance.checkWithinTolerance(rows.get(actualRow).getAverageRT(), row.getAverageRT())&& rows.get(actualRow).getCompoundAnnotations().isEmpty()
      ) {
        continue;
      }
      newList.put(actualRow, rows.get(actualRow));
    }
    return newList;
  }

  public void addResultToProject(ModularFeatureList resultPeakList, MZmineProject project,
      ParameterSet parameters) {
    // Add new peakList to the project
    project.addFeatureList(resultPeakList);

    //   Load previous applied methods
    for (FeatureListAppliedMethod proc : resultPeakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("IsotopePeakScanner", IsotopePeakScannerModule.class,
            parameters, moduleCallDate));
  }


  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {

  }
  public MemoryMapStorage getMemoryMapStorage() {
    return storage;
  }

}



