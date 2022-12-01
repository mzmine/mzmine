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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.autocarbon.AutoCarbonParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.ScanUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IIsotope;

/**
 * This will scan a feature list for calculated isotope patterns. This class loops through every
 * peak and checks if there are peaks within a specified RT and m/z window. The m/z window is
 * calculated by the maximum mass shift caused by the isotope pattern. Every peak that fits the m/z
 * window is also checked for RT if specified => groupedPeaks. The next step checks for peaks inside
 * the specified m/z window around the expected isotope-pattern m/zs (current peak mass +
 * (isotope[i]-isotope[0])). Every peak that fits the criteria is added to resultBuffer.
 * Furthermore, every peak in the resultBuffer is rated by m/z and intensity, if the rating is
 * better that the previous one the peak will be added as a candidate (by Candidates.java). If the
 * algorithm was able to find a peak inside the feature list for every expected isotope peak the
 * result will be added to a result feature list including a description.
 *
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class IsotopePeakScannerTask extends AbstractTask {

  ScanType scanType;
  RatingType ratingType;
  IIsotope[] el;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ParameterSet parameters;
  private boolean checkIntensity;
  private double minRating;
  private double minHeight;
  private String element, suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private double minPatternIntensity;
  private double mergeWidth;
  private String message;
  private int totalRows, finishedRows;
  private ModularFeatureList resultPeakList;
  private MZmineProject project;
  private FeatureList peakList;
  private boolean checkRT;
  private SimpleIsotopePattern[] pattern;
  private PolarityType polarityType;
  private int charge;
  private boolean accurateAvgIntensity;
  private String ratingChoice;
  private double minAccurateAvgIntensity;
  private boolean autoCarbon;
  private int carbonRange, autoCarbonMin, autoCarbonMax;
  private int maxPatternSize, maxPatternIndex;
  private int autoCarbonMinPatternSize;
  private boolean excludeZeroCPattern;

  /**
   * @param parameters
   * @param peakList
   */
  IsotopePeakScannerTask(MZmineProject project, FeatureList peakList, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.project = project;
    this.peakList = peakList;

    mzTolerance = parameters.getParameter(IsotopePeakScannerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(IsotopePeakScannerParameters.rtTolerance).getValue();
    checkIntensity =
        parameters.getParameter(IsotopePeakScannerParameters.checkIntensity).getValue();
    mergeWidth = parameters.getParameter(IsotopePeakScannerParameters.mergeWidth).getValue();
    minPatternIntensity =
        parameters.getParameter(IsotopePeakScannerParameters.minPatternIntensity).getValue();
    element = parameters.getParameter(IsotopePeakScannerParameters.element).getValue();
    minRating = parameters.getParameter(IsotopePeakScannerParameters.minRating).getValue();
    suffix = parameters.getParameter(IsotopePeakScannerParameters.suffix).getValue();
    checkRT = parameters.getParameter(IsotopePeakScannerParameters.checkRT).getValue();
    minHeight = parameters.getParameter(IsotopePeakScannerParameters.minHeight).getValue();
    charge = parameters.getParameter(IsotopePeakScannerParameters.charge).getValue();
    accurateAvgIntensity =
        parameters.getParameter(IsotopePeakScannerParameters.calculate_accurate_average).getValue();
    ratingChoice = parameters.getParameter(IsotopePeakScannerParameters.ratingChoices).getValue();

    autoCarbon = parameters.getParameter(IsotopePeakScannerParameters.autoCarbonOpt).getValue();
    ParameterSet autoCarbonParameters =
        parameters.getParameter(IsotopePeakScannerParameters.autoCarbonOpt).getEmbeddedParameters();
    autoCarbonMin = autoCarbonParameters.getParameter(AutoCarbonParameters.minCarbon).getValue();
    autoCarbonMax = autoCarbonParameters.getParameter(AutoCarbonParameters.maxCarbon).getValue();
    autoCarbonMinPatternSize =
        autoCarbonParameters.getParameter(AutoCarbonParameters.minPatternSize).getValue();

    scanType = (autoCarbon) ? ScanType.AUTOCARBON : ScanType.SPECIFIC;

    excludeZeroCPattern = true; // TODO: do i need this?

    if (accurateAvgIntensity && !checkIntensity) {
      accurateAvgIntensity = false;
    }

    if (scanType == ScanType.AUTOCARBON) {
      carbonRange = autoCarbonMax - autoCarbonMin + 1;
    } else if (scanType == ScanType.SPECIFIC) {
      carbonRange = 1;
    }

    if (ratingChoice.equals("Temporary average")) {
      ratingType = RatingType.TEMPAVG;
    } else {
      ratingType = RatingType.HIGHEST;
    }

    polarityType = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
    charge = (charge < 0) ? charge * -1 : charge;

    if (getPeakListPolarity(peakList) != polarityType) {
      logger.warning("PeakList.polarityType does not match selected polarity. "
                     + getPeakListPolarity(peakList).toString() + "!=" + polarityType.toString());
    }

    if (suffix.equals("auto")) {
      suffix = "";
      if (scanType == ScanType.AUTOCARBON) {
        suffix = " autoCarbon";
      }
      suffix += "_-Pat=" + element + "-RT=" + checkRT + "-INT=" + checkIntensity + "-minR="
                + minRating + "-minH=" + minHeight + "_results";
    }

    message = "Got paramenters...";
  }

  /**
   * @param pattern IsotopePattern to calculate intensity ratios of
   * @param index   DataPoint index to normalize the intesitys to
   * @return String of intensity ratios seperated by ":"
   */
  private static String getIntensityRatios(IsotopePattern pattern, int index) {
    DataPoint[] dp = ScanUtils.extractDataPoints(pattern);

    String ratios = "";
    for (int i = 0; i < dp.length; i++)
    // ratios += round(dp[i].getIntensity(), 2) + ":";
    {
      ratios += round((dp[i].getIntensity() / dp[index].getIntensity()), 2) + ":";
    }
    ratios = (ratios.length() > 0) ? ratios.substring(0, ratios.length() - 1) : ratios;
    return ratios;
  }

  public static double round(double value,
      int places) { // https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
    if (places < 0) {
      throw new IllegalArgumentException();
    }

    BigDecimal bd = new BigDecimal(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }

  /**
   * adds a comment to a PeakListRow without deleting the current comment
   *
   * @param row PeakListRow to add the comment to
   * @param str comment to be added
   */
  public static void addComment(FeatureListRow row, String str) {
    String current = row.getComment();
    if (current == null) {
      row.setComment(str);
    } else if (current.contains(str)) {
      return;
    } else {
      row.setComment(current + " " + str);
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0;
    }
    return (double) finishedRows / (double) totalRows;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return message;
  }

  @Override
  public void run() {

    if (!checkParameters()) {
      return;
    }

    setStatus(TaskStatus.PROCESSING);

    totalRows = peakList.getNumberOfRows();

    double[][] diff = setUpDiffAutoCarbon();
    if (diff == null) {
      message = "ERROR: could not set up diff.";
      setStatus(TaskStatus.ERROR);
      return;
    }

    logger.info("diff.length: " + diff.length);
    logger.info("maxPatternIndex: " + maxPatternIndex);
    logger.info("maxPatternSize: " + maxPatternSize);

    // get all rows and sort by m/z
    FeatureListRow[] rows = peakList.getRows().toArray(FeatureListRow[]::new);
    Arrays.sort(rows, new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    PeakListHandler plh = new PeakListHandler();
    plh.setUp(peakList);

    resultPeakList =
        new ModularFeatureList(peakList.getName() + " " + suffix, getMemoryMapStorage(),
            peakList.getRawDataFiles());
    PeakListHandler resultMap = new PeakListHandler();

    for (int i = 0; i < totalRows; i++) {
      // i will represent the index of the row in peakList
      if (rows[i].getPeakIdentities().size() > 0) {
        finishedRows++;
        continue;
      }

      message = "Row " + i + "/" + totalRows;

      // now get all peaks that lie within RT and maxIsotopeMassRange:
      // pL[index].mz ->
      // pL[index].mz+maxMass
      ArrayList<FeatureListRow> groupedPeaks =
          groupPeaks(rows, i, diff[maxPatternIndex][diff[maxPatternIndex].length - 1]);

      if (groupedPeaks.size() < 2) {
        finishedRows++;
        continue;
      }
      // else
      // logger.info("groupedPeaks.size > 2 in row: " + i + " size: " +
      // groupedPeaks.size());

      ResultBuffer[][] resultBuffer = new ResultBuffer[diff.length][]; // this
      // will
      // store
      // row
      // indexes
      // TODO: it should be possible to use a single array of result
      // buffer instead of a 2D array
      // which should reduce computation time later on. the problem is
      // that some carbon peaks
      // might pop up within
      // the pattern and change indices. for testing purposes ill do it as
      // it is

      for (int p = 0; p < diff.length; p++) { // resultBuffer[i] index
        // will represent Isotope[i]
        // (if
        // numAtoms = 0)
        resultBuffer[p] = new ResultBuffer[diff[p].length];

        for (int k = 0; k < diff[p].length; k++) {
          resultBuffer[p][k] = new ResultBuffer(); // [p][0] will be
        }
        // the isotope with
        // lowest mass#
      }

      // of all features with fitting rt
      // and mz
      boolean trueBuffers[] = new boolean[diff.length];
      Arrays.fill(trueBuffers, false);

      for (int j = 0; j < groupedPeaks.size(); j++) // go through all
      // possible peaks
      {
        for (int p = 0; p < diff.length; p++) {

          for (int k = 0; k < diff[p].length; k++) // check for each
          // peak if it is a
          // possible
          // feature
          // for
          // every diff[](isotope)
          { // this is necessary bc there might be more than one
            // possible feature
            // j represents the row index in groupedPeaks
            // k represents the isotope number the peak will be a
            // candidate for
            // p = pattern index for autoCarbon
            if (mzTolerance.checkWithinTolerance(groupedPeaks.get(0).getAverageMZ() + diff[p][k],
                groupedPeaks.get(j).getAverageMZ())) {
              // this will automatically add groupedPeaks[0] to
              // the list -> isotope with
              // lowest mass
              resultBuffer[p][k].addFound(); // +1 result for
              // isotope k
              resultBuffer[p][k].addRow(j); // row in
              // groupedPeaks[]
              resultBuffer[p][k].addID(groupedPeaks.get(j).getID());
            }
          }
        }
      }

      boolean foundOne = false;

      for (int p = 0; p < diff.length; p++) {
        if (checkIfAllTrue(resultBuffer[p])) { // this means that for
          // every isotope we
          // expected to
          // find,
          foundOne = true; // we found one or more possible features
          trueBuffers[p] = true;
          // logger.info("Row: " + i + " filled buffer[" + p +"]");
        }
      }
      if (!foundOne) {
        finishedRows++;
        continue;
      }

      Candidates[] candidates = new Candidates[diff.length];
      for (int p = 0; p < diff.length; p++) {
        candidates[p] = new Candidates(diff[p].length, minHeight, mzTolerance, pattern[p], plh,
            ratingType);
      }

      for (int p = 0; p < diff.length; p++) {
        if (!trueBuffers[p]) {
          continue;
        }
        for (int k = 0; k < resultBuffer[p].length; k++) // reminder:
        // resultBuffer.length
        // =
        // diff.length
        {
          for (int l = 0; l < resultBuffer[p][k].getFoundCount(); l++) {
            // k represents index resultBuffer[k] and thereby the
            // isotope number
            // l represents the number of results in resultBuffer[k]
            candidates[p].checkForBetterRating(k, groupedPeaks.get(0),
                groupedPeaks.get(resultBuffer[p][k].getRow(l)), minRating, checkIntensity);

          }
        }
      }

      foundOne = false;
      boolean trueCandidates[] = new boolean[diff.length];
      Arrays.fill(trueCandidates, false);

      for (int p = 0; p < diff.length; p++) {
        if (trueBuffers[p] && checkIfAllTrue(candidates[p].getCandidates())) {
          trueCandidates[p] = true;
          foundOne = true;
          // logger.info("Row: " + i + " filled candidates[" + p +
          // "]");
        }
      }
      if (!foundOne) {
        finishedRows++;
        // logger.info("Not enough valid candidates for parent feature "
        // +
        // groupedPeaks.get(0).getAverageMZ() + "\talthough enough peaks
        // were found.") ;
        continue; // jump to next i
      }

      // find best result now, first we have to calc avg ratings if
      // specified by user
      int bestPatternIndex = 0;
      double bestRating = 0.0;
      for (int p = 0; p < diff.length; p++) {

        if (!trueCandidates[p]) {
          continue;
        }

        if (accurateAvgIntensity) {
          candidates[p].calcAvgRatings();
        }
        // this is a final rating, with averaged intensities in all
        // mass lists that contain EVERY peak that was selected.
        // thats why we can only do it after ALL peaks have been
        // found

        if (accurateAvgIntensity && candidates[p].getAvgAccAvgRating() > bestRating) {
          bestPatternIndex = p;
          bestRating = candidates[p].getAvgAccAvgRating();
        } else if (!accurateAvgIntensity && candidates[p].getSimpleAvgRating() > bestRating) {
          bestPatternIndex = p;
          bestRating = candidates[p].getSimpleAvgRating();
        }
      }

      if (!checkIfAllTrue(candidates[bestPatternIndex].getCandidates())) {
        logger.warning(
            "We were about to add candidates with null pointers.\nThis was no valid result. Continueing.");
        continue;
      } // TODO: this shouldnt be needed, fix the bug that causes the
      // crash later on.
      // this happens occasionally if the user wants to do accurate
      // average but does not filter
      // by RT. then possible isotope peaks are found, although they are
      // not detected at the same
      // time. This will result in the candidates return -1.0 which will
      // sooner or later return a
      // null pointer Fixing this will be done in a future update, but
      // needs a rework of the
      // candidates class.
      // The results you miss by skipping here would have not been valid
      // results anyway, so this
      // is not urgent. Will be nicer though, because of cleaner code.

      // PeakListRow parent = copyPeakRow(peakList.getRow(i));

      boolean allPeaksAddable = true;
      List<FeatureListRow> rowBuffer = new ArrayList<FeatureListRow>();

      ModularFeatureListRow original = getRowFromCandidate(candidates, bestPatternIndex, 0, plh);
      if (original == null) {
        continue;
      }

      ModularFeatureListRow parent = new ModularFeatureListRow(resultPeakList, original.getID(),
          original, true);

      if (resultMap.containsID(parent.getID())) // if we can assign this
      // row multiple times we
      // have to copy the
      // comment, because adding
      // it to
      // the map twice will
      // overwrite the results
      {
        addComment(parent, resultMap.getRowByID(parent.getID()).getComment());
      }

      addComment(parent, parent.getID() + "--IS PARENT--"); // ID is added
      // to be able
      // to sort by
      // comment to bring all isotope patterns together

      if (carbonRange != 1) {
        addComment(parent, "BestPattern: " + pattern[bestPatternIndex].getDescription());
      }

      rowBuffer.add(parent);

      DataPoint[] dp = new DataPoint[pattern[bestPatternIndex].getNumberOfDataPoints()];
      // we need this to add the IsotopePattern later on

      if (accurateAvgIntensity) {
        dp[0] = new SimpleDataPoint(parent.getAverageMZ(),
            candidates[bestPatternIndex].getAvgHeight(0));
      } else {
        dp[0] = new SimpleDataPoint(parent.getAverageMZ(), parent.getAverageHeight());
      }

      for (int k = 1; k < candidates[bestPatternIndex].size(); k++) // we
      // skip
      // k=0
      // because
      // ==
      // groupedPeaks[0]/
      // ==candidates.get(0) which we added before
      {
        ModularFeatureListRow originalChild =
            getRowFromCandidate(candidates, bestPatternIndex, k, plh);

        if (originalChild == null) {
          allPeaksAddable = false;
          continue;
        }
        ModularFeatureListRow child =
            new ModularFeatureListRow(resultPeakList, originalChild.getID(), originalChild, true);

        if (accurateAvgIntensity) {
          dp[k] = new SimpleDataPoint(child.getAverageMZ(),
              candidates[bestPatternIndex].getAvgHeight(k));
        } else {
          dp[k] = new SimpleDataPoint(child.getAverageMZ(), child.getAverageHeight());
        }

        String average = "";
        if (accurateAvgIntensity) {
          average = " AvgRating: " + round(candidates[bestPatternIndex].getAvgRating(k), 3);
        }

        addComment(parent, "Intensity ratios: " + getIntensityRatios(pattern[bestPatternIndex],
            pattern[bestPatternIndex].getBasePeakIndex()));
        if (accurateAvgIntensity) {
          addComment(parent, " Avg pattern rating: "
                             + round(candidates[bestPatternIndex].getAvgAccAvgRating(), 3));
        } else {
          addComment(parent,
              " pattern rating: " + round(candidates[bestPatternIndex].getSimpleAvgRating(), 3));
        }

        addComment(child,
            (parent.getID() + "-Parent ID" + " m/z-shift(ppm): "
             + round(((child.getAverageMZ() - parent.getAverageMZ()) - diff[bestPatternIndex][k])
                     / child.getAverageMZ() * 1E6, 2)
             + " I(c)/I(p): "
             + round(child.getAverageHeight() / plh
                .getRowByID(candidates[bestPatternIndex]
                    .get(pattern[bestPatternIndex].getBasePeakIndex()).getCandID())
                .getAverageHeight(), 2)
             + " Identity: " + pattern[bestPatternIndex].getIsotopeComposition(k) + " Rating: "
             + round(candidates[bestPatternIndex].get(k).getRating(), 3) + average));

        rowBuffer.add(child);
      }

      if (!allPeaksAddable) {
        continue;
      }

      IsotopePattern resultPattern = new SimpleIsotopePattern(dp, charge,
          IsotopePatternStatus.DETECTED, element + " monoisotopic mass: " + parent.getAverageMZ());
      parent.getBestFeature().setIsotopePattern(resultPattern);

      for (FeatureListRow row : rowBuffer) {
        row.getBestFeature().setIsotopePattern(resultPattern);
        resultMap.addRow(row);
      }

      if (isCanceled()) {
        return;
      }

      finishedRows++;
    }

    ArrayList<Integer> keys = resultMap.getAllKeys();
    for (Integer key : keys) {
      resultPeakList.addRow(resultMap.getRowByID(key));
    }

    if (resultPeakList.getNumberOfRows() > 1) {
      addResultToProject(/* resultPeakList */);
    } else {
      message = "Element not found.";
    }
    setStatus(TaskStatus.FINISHED);

  }

  /**
   * @param b
   * @return true if every b[i].getFoundCount != 0
   */
  private boolean checkIfAllTrue(ResultBuffer[] b) {
    for (int i = 0; i < b.length; i++) {
      if (b[i].getFoundCount() == 0) {
        return false;
      }
    }
    return true;
  }

  private boolean checkIfAllTrue(Candidate[] cs) {
    for (Candidate c : cs) {
      if (c.getRating() == 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Extracts a feature list row from a Candidates array.
   *
   * @param candidates
   * @param bestPatternIndex The index of the isotope pattern that was found to be the best fit for
   *                         the detected pattern
   * @param peakIndex        the index of the candidate peak, the feature list row should be
   *                         extracted for.
   * @param plh
   * @return null if no peak with the given parameters exists, the specified feature list row
   * otherwise.
   */
  private @Nullable
  ModularFeatureListRow getRowFromCandidate(@NotNull Candidates[] candidates,
      int bestPatternIndex, int peakIndex, @NotNull PeakListHandler plh) {

    if (bestPatternIndex >= candidates.length) {
      return null;
    }

    if (peakIndex >= candidates[bestPatternIndex].size()) {
      return null;
    }

    Candidate cand = candidates[bestPatternIndex].get(peakIndex);

    if (cand != null) {
      int id = cand.getCandID();
      FeatureListRow original = plh.getRowByID(id);
      return (ModularFeatureListRow) original;
    }
    return null;
  }

  /**
   * This calculates the isotope pattern using SimpleIsotopePattern and creates an ArrayList<Double>
   * that will contain the mass shift for every expected isotope peak relative to the one with the
   * lowest mass.
   *
   * @return
   */
  private double[][] setUpDiffAutoCarbon() {

    // ArrayList<Double> diff = new ArrayList<Double>(2);

    double[][] diff;

    if (scanType == ScanType.AUTOCARBON) {

      String[] strPattern = new String[carbonRange];

      SimpleIsotopePattern patternBuffer[] = new SimpleIsotopePattern[carbonRange];

      // in the following for we calculate up the patterns
      for (int p = 0; p < carbonRange; p++) {
        if (p + autoCarbonMin != 0) {
          strPattern[p] = "C" + (p + autoCarbonMin) + element;
        } else {
          strPattern[p] = element;
        }

        try {
          patternBuffer[p] =
              (SimpleIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(strPattern[p],
                  0.001, mergeWidth, charge, polarityType, true);
          patternBuffer[p] = (SimpleIsotopePattern) IsotopePatternCalculator
              .removeDataPointsBelowIntensity(patternBuffer[p], minPatternIntensity);
        } catch (Exception e) {
          logger.warning("The entered Sum formula is invalid.");
          return null;
        }
      }

      int sizeCounter = 0;
      // in this for we check how much of the patterns match the
      // autoCarbonMinPatternSize criteria
      // if they dont fit we null them
      for (int p = 0; p < carbonRange; p++) {
        if (patternBuffer[p].getNumberOfDataPoints() >= autoCarbonMinPatternSize) {
          sizeCounter++;
        } else {
          patternBuffer[p] = null;
        }
      }

      if (sizeCounter == 0) {
        throw new MSDKRuntimeException(
            "Min pattern size excludes every calculated isotope pattern.\nPlease increase min pattern intensity for more data points or decrease the minimum pattern size.");
      }

      logger.info("about to add " + sizeCounter + " patterns to the scan.");
      diff = new double[sizeCounter][];
      int addCounter = 0;
      pattern = new SimpleIsotopePattern[sizeCounter];
      for (int p = 0; p < carbonRange; p++) {

        if (patternBuffer[p] == null) {
          continue;
        }

        pattern[addCounter] = patternBuffer[p];
        DataPoint[] points = ScanUtils.extractDataPoints(patternBuffer[p]);
        diff[addCounter] = new double[points.length];

        if (maxPatternSize < diff[addCounter].length) {
          maxPatternSize = diff[addCounter].length;
          maxPatternIndex = addCounter;
        }

        for (int i = 0; i < pattern[addCounter].getNumberOfDataPoints(); i++) {
          diff[addCounter][i] = points[i].getMZ() - points[0].getMZ();
        }
        addCounter++;
      }
    } else /* if(scanType == ScanType.SPECIFIC) */ {
      diff = new double[1][];
      pattern = new SimpleIsotopePattern[1];
      pattern[0] = (SimpleIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(element,
          0.001, mergeWidth, charge, polarityType, true);
      pattern[0] = (SimpleIsotopePattern) IsotopePatternCalculator
          .removeDataPointsBelowIntensity(pattern[0], minPatternIntensity);
      DataPoint[] points = ScanUtils.extractDataPoints(pattern[0]);
      diff[0] = new double[points.length];

      if (maxPatternSize < diff[0].length) {
        maxPatternSize = diff[0].length;
        maxPatternIndex = 0;
      }

      for (int i = 0; i < pattern[0].getNumberOfDataPoints(); i++) {
        diff[0][i] = points[i].getMZ() - points[0].getMZ();
      }
    }
    logger.info("diff set up...");
    return diff;
  }

  /**
   * @param pL
   * @param parentIndex index of possible parent peak
   * @return will return ArrayList<PeakListRow> of all peaks within the range of pL[parentIndex].mz
   * -> pL[parentIndex].mz+maxMass
   */
  private ArrayList<FeatureListRow> groupPeaks(FeatureListRow[] pL, int parentIndex,
      double maxDiff) {

    ArrayList<FeatureListRow> buf = new ArrayList<FeatureListRow>();

    buf.add(pL[parentIndex]); // this means the result will contain
    // row(parentIndex) itself

    double mz = pL[parentIndex].getAverageMZ();
    float rt = pL[parentIndex].getAverageRT();

    for (int i = parentIndex + 1; i < pL.length; i++) // will not add the
    // parent peak itself
    {
      FeatureListRow r = pL[i];
      // check for rt

      if (r.getAverageHeight() < minHeight) {
        continue;
      }

      if (!(pL[i].getAverageMZ() > mz
            && pL[i].getAverageMZ() <= (mz + maxDiff + mzTolerance.getMzTolerance()))) {

        if (pL[i].getAverageMZ() > (mz + maxDiff)) // since pL is sorted
        // by ascending mass,
        // we can
        // stop now
        {
          return buf;
        }
        continue;
      }

      if (checkRT && !rtTolerance.checkWithinTolerance(rt, r.getAverageRT())) {
        continue;
      }

      buf.add(pL[i]);
    }
    return buf;
  }

  /**
   * Add feature list to project, delete old if requested, add description to result, show new
   * feature list in a new tab
   */
  public void addResultToProject() {
    // Add new peakList to the project
    project.addFeatureList(resultPeakList);

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("IsotopePeakScanner",
            IsotopePeakScannerModule.class, parameters, getModuleCallDate()));
  }

  private PolarityType getPeakListPolarity(FeatureList peakList) {
    return peakList.getRawDataFiles().stream()
        .map(raw -> raw.getDataPolarity().stream().findFirst().orElse(PolarityType.UNKNOWN))
        .findFirst().orElse(PolarityType.UNKNOWN);
  }

  private boolean checkParameters() {
    if (charge == 0) {
      setErrorMessage("Error: charge may not be 0!");
      setStatus(TaskStatus.ERROR);
      return false;
    }
    if (!FormulaUtils.checkMolecularFormula(element)) {
      setErrorMessage("Error: Invalid formula!");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    if (accurateAvgIntensity || ratingType == RatingType.TEMPAVG) {
      if (ratingType == RatingType.TEMPAVG) {
        setErrorMessage(
            "Error: Rating Type = temporary average but no masslist was selected.\nYou can"
            + " select a mass list without checking accurate average.");
        setStatus(TaskStatus.ERROR);
        return false;
      }
      if (peakList.getNumberOfRawDataFiles() > 1) {
        setErrorMessage("The number of raw data files of feature list \"" + peakList.getName()
                        + "\" is greater than 1. This is not supported by this module.");
        setStatus(TaskStatus.ERROR);
        return false;
      }

      RawDataFile[] raws = peakList.getRawDataFiles().toArray(RawDataFile[]::new);
      boolean foundMassList = false;
      for (RawDataFile raw : raws) {
        ObservableList<Scan> scanNumbers = raw.getScans();
        for (Scan scan : scanNumbers) {
          MassList massList = scan.getMassList();
          if (massList != null) {
            foundMassList = true;
            break;
          }
        }
      }
      if (foundMassList == false) {
        setErrorMessage("Feature list \"" + peakList.getName()
                        + "\" does not contain a mass list");
        setStatus(TaskStatus.ERROR);
        return false;
      }
    }
    return true;
  }

  public enum RatingType {
    HIGHEST, TEMPAVG
  }

  public enum ScanType {
    SPECIFIC, AUTOCARBON
  }
}
