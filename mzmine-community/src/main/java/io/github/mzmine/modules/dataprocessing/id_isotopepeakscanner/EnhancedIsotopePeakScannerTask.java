/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class EnhancedIsotopePeakScannerTask extends AbstractTask {

  private static ModularFeatureList resultPeakList;
  private ParameterSet parameters;
  private MZmineProject project;
  private FeatureList peakList;
  private MZTolerance mzTolerance;
  private MobilityTolerance mobTolerance;
  private RTTolerance rtTolerance;
  private double minPatternIntensity;
  private String patternFormulas, suffix;
  private int charge;
  private PolarityType polarityType;
  private double minIsotopePatternScore;
  private boolean bestScores;
  private boolean onlyMonoisotopic;
  private boolean resolvedByMobility;
  private double minHeight;


  private static final Logger logger = Logger.getLogger(
      EnhancedIsotopePeakScannerTask.class.getName());

  EnhancedIsotopePeakScannerTask(MZmineProject project, FeatureList peakList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.project = project;
    this.peakList = peakList;

    mzTolerance = parameters.getParameter(IsotopePeakScannerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(IsotopePeakScannerParameters.rtTolerance).getValue();
    mobTolerance = parameters.getParameter(IsotopePeakScannerParameters.mobTolerance).getValue();
    minPatternIntensity = parameters.getParameter(IsotopePeakScannerParameters.minPatternIntensity)
        .getValue();
    patternFormulas = parameters.getParameter(IsotopePeakScannerParameters.formula).getValue();
    suffix = parameters.getParameter(IsotopePeakScannerParameters.suffix).getValue();
    charge = parameters.getParameter(IsotopePeakScannerParameters.charge).getValue();
    minIsotopePatternScore = parameters.getParameter(
        IsotopePeakScannerParameters.minIsotopePatternScore).getValue();
    bestScores = parameters.getParameter(IsotopePeakScannerParameters.bestScores).getValue();
    onlyMonoisotopic = parameters.getParameter(IsotopePeakScannerParameters.onlyMonoisotopic)
        .getValue();
    resolvedByMobility = parameters.getParameter(IsotopePeakScannerParameters.resolvedByMobility)
        .getValue();
    minHeight = parameters.getParameter(IsotopePeakScannerParameters.minHeight).getValue();

    polarityType = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
    charge = (charge < 0) ? charge * -1 : charge;
  }

  @Override
  public void run() {

    if (!checkParameters()) {
      return;
    }
    resultPeakList = FeatureListUtils.createCopy(peakList, suffix, getMemoryMapStorage(), false);

    if (getPeakListPolarity(peakList) != polarityType) {
      logger.warning(
          "PeakList.polarityType does not match selected polarity. " + getPeakListPolarity(
              peakList).toString() + "!=" + polarityType.toString());
    }

    final ObservableList<FeatureListRow> rows = peakList.getRows();
    final PeakListHandler featureMap = new PeakListHandler();
    final Map<Integer, IsotopePattern> detectedIsotopePattern = new HashMap<>();
    final List<FeatureListRow> rowsWithIPs = new ArrayList<>();
    final Map<Integer, Double> rowIdToScore = new HashMap<>();
    final PeakListHandler finalMap;
    final IsotopePatternCalculator ipCalculator = new IsotopePatternCalculator(minPatternIntensity,
        mzTolerance);
    final IsotopePeakFinder isotopePeakFinder = new IsotopePeakFinder();
    final IsotopePatternScoring scoring = new IsotopePatternScoring();
    final MajorIsotopeIdentifier majorIsotopeIdentifier = new MajorIsotopeIdentifier();

    for (FeatureListRow row : rows) {
      featureMap.addRow(row);
    }

    // Scanning for characteristic isotope patterns for each element combination and charge.
    // Stored in "ResultMap" if the Similarity Score is higher than minScore.
    //In case of multiple scores for different charges, only the isotope pattern with the highest
    // score is stored as "detectedIsotopePattern.

    int[] charges = new int[charge];
    for (int i = 0; i < charges.length; i++) {
      charges[i] = i + 1;
    }
    final String[] formulas = Arrays.stream(patternFormulas.split(",")).map(String::trim)
        .toArray(String[]::new);

    for (String formula : formulas) {
      for (int charge : charges) {

        for (FeatureListRow row : rows) {

          final IsotopePattern calculatedPattern = ipCalculator.calculateIsotopePattern(row,
              formula, charge);
          final IsotopePattern detectedPattern = isotopePeakFinder.detectedIsotopePattern(peakList,
              row, calculatedPattern, mzTolerance, minHeight, resolvedByMobility, charge);
          final double score = scoring.calculateIsotopeScore(detectedPattern, calculatedPattern,
              mzTolerance, minHeight);

          if (score >= minIsotopePatternScore) {
            if (rowIdToScore.get(row.getID()) != null && rowIdToScore.get(row.getID()) > score) {
              continue;
            }
            detectedIsotopePattern.put(row.getID(), detectedPattern);
            row.getBestFeature().setCharge(charge);
            rowsWithIPs.add(row);
            rowIdToScore.put(row.getID(), score);
          }
        }
      }
      //Reduction of the features to the monoisotopic signals, whereby the monoisotopic signal is
      // assumed to be the one with the highest SimilarityScore within the MZ range of the IsotopePattern.
      //A tolerance range of 0.01 was applied to avoid excluding isotopic patterns with very similar score values.

      if (onlyMonoisotopic) {
        majorIsotopeIdentifier.findMajorIsotopes(rowsWithIPs, rowIdToScore, detectedIsotopePattern,
            rtTolerance, mobTolerance, resolvedByMobility);
      } else {
        majorIsotopeIdentifier.findAllIsotopes(rowsWithIPs, rowIdToScore, detectedIsotopePattern);
      }
      rowsWithIPs.clear();
      detectedIsotopePattern.clear();
      rowIdToScore.clear();
    }

    // Reduction of features to those with the best similarity values of all considered element combinations.
    if (bestScores) {
      majorIsotopeIdentifier.findMajorIsotopesWithBestScores(rtTolerance, mobTolerance,
          resolvedByMobility);
      finalMap = majorIsotopeIdentifier.getResultMapOfMajorIsotopesWithBestScores();
    } else {
      finalMap = majorIsotopeIdentifier.getResultMapOfIsotopes();
    }

    resultPeakList = finalMap.generateResultPeakList(majorIsotopeIdentifier, resultPeakList);
    addResultToProject(resultPeakList);
    setStatus(TaskStatus.FINISHED);
  }

  private PolarityType getPeakListPolarity(FeatureList peakList) {
    return FeatureListUtils.getPolarity(peakList);
  }

  /**
   * @return The {@link MemoryMapStorage} used to store results of this task (e.g. RawDataFiles,
   * MassLists, FeatureLists). May be null if results shall be stored in ram.
   */
  @Nullable
  public MemoryMapStorage getMemoryMapStorage() {
    return storage;
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }


  public void addResultToProject(ModularFeatureList resultPeakList) {
    // Add new peakList to the project
    project.addFeatureList(resultPeakList);

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("IsotopePeakScanner", IsotopePeakScannerModule.class,
            parameters, getModuleCallDate()));
  }


  private boolean checkParameters() {
    if (charge == 0) {
      error("Error: charge may not be 0!");
      return false;
    }

    return true;
  }

}
