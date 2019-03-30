/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IIsotope;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.IsotopePeakScannerTask.RatingType;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.IsotopePeakScannerTask.ScanType;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.autocarbon.AutoCarbonParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.FormulaUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;


/**
 * This will scan a peak list for calculated isotope patterns. This class loops through every peak
 * and checks if there are peaks within a specified RT and m/z window. The m/z window is calculated
 * by the maximum mass shift caused by the isotope pattern. Every peak that fits the m/z window is
 * also checked for RT if specified => groupedPeaks. The next step checks for peaks inside the
 * specified m/z window around the expected isotope-pattern m/zs (current peak mass +
 * (isotope[i]-isotope[0])). Every peak that fits the criteria is added to resultBuffer.
 * Furthermore, every peak in the resultBuffer is rated by m/z and intensity, if the rating is
 * better that the previous one the peak will be added as a candidate (by Candidates.java). If the
 * algorithm was able to find a peak inside the peak list for every expected isotope peak the result
 * will be added to a result peak list including a description.
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 * 
 */
public class IsotopePeakScannerTask extends AbstractTask {

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
  private PeakList resultPeakList;
  private MZmineProject project;
  private PeakList peakList;
  private boolean checkRT;
  private ExtendedIsotopePattern[] pattern;
  private PolarityType polarityType;
  private int charge;
  private boolean accurateAvgIntensity;
  private String massListName;
  private String ratingChoice;
  private double minAccurateAvgIntensity;

  private boolean autoCarbon;
  private int carbonRange, autoCarbonMin, autoCarbonMax;
  private int maxPatternSize, maxPatternIndex;
  private int autoCarbonMinPatternSize;
  private boolean excludeZeroCPattern;



  public enum RatingType {
    HIGHEST, TEMPAVG
  };

  public enum ScanType {
    SPECIFIC, AUTOCARBON
  };


  ScanType scanType;

  RatingType ratingType;

  IIsotope[] el;

  /**
   *
   * @param parameters
   * @param peakList
   * @param peakListRow
   * @param peak
   */
  IsotopePeakScannerTask(MZmineProject project, PeakList peakList, ParameterSet parameters) {
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
        parameters.getParameter(IsotopePeakScannerParameters.massList).getValue();
    massListName = parameters.getParameter(IsotopePeakScannerParameters.massList)
        .getEmbeddedParameter().getValue();
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

    if (scanType == ScanType.AUTOCARBON)
      carbonRange = autoCarbonMax - autoCarbonMin + 1;
    else if (scanType == ScanType.SPECIFIC)
      carbonRange = 1;

    if (ratingChoice.equals("Temporary average"))
      ratingType = RatingType.TEMPAVG;
    else
      ratingType = RatingType.HIGHEST;



    polarityType = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
    charge = (charge < 0) ? charge * -1 : charge;

    if (getPeakListPolarity(peakList) != polarityType)
      logger.warning("PeakList.polarityType does not match selected polarity. "
          + getPeakListPolarity(peakList).toString() + "!=" + polarityType.toString());

    if (suffix.equals("auto")) {
      suffix = "";
      if (scanType == ScanType.AUTOCARBON)
        suffix = " autoCarbon";
      suffix += "_-Pat=" + element + "-RT=" + checkRT + "-INT=" + checkIntensity + "-minR="
          + minRating + "-minH=" + minHeight + "_results";
    }

    message = "Got paramenters...";
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0.0;
    return (double) finishedRows / (double) totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return message;
  }

  @Override
  public void run() {

    if (!checkParameters())
      return;

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
    PeakListRow[] rows = peakList.getRows();
    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    PeakListHandler plh = new PeakListHandler();
    plh.setUp(peakList);

    resultPeakList = new SimplePeakList(peakList.getName() + suffix, peakList.getRawDataFiles());
    PeakListHandler resultMap = new PeakListHandler();

    for (int i = 0; i < totalRows; i++) {
      // i will represent the index of the row in peakList
      if (peakList.getRow(i).getPeakIdentities().length > 0) {
        finishedRows++;
        continue;
      }

      message = "Row " + i + "/" + totalRows;

      // now get all peaks that lie within RT and maxIsotopeMassRange: pL[index].mz ->
      // pL[index].mz+maxMass
      ArrayList<PeakListRow> groupedPeaks =
          groupPeaks(rows, i, diff[maxPatternIndex][diff[maxPatternIndex].length - 1]);

      if (groupedPeaks.size() < 2) {
        finishedRows++;
        continue;
      }
      // else
      // logger.info("groupedPeaks.size > 2 in row: " + i + " size: " +
      // groupedPeaks.size());

      ResultBuffer[][] resultBuffer = new ResultBuffer[diff.length][]; // this will store row
                                                                       // indexes
      // TODO: it should be possible to use a single array of result buffer instead of a 2D array
      // which should reduce computation time later on. the problem is that some carbon peaks
      // might pop up within
      // the pattern and change indices. for testing purposes ill do it as it is

      for (int p = 0; p < diff.length; p++) { // resultBuffer[i] index will represent Isotope[i] (if
        // numAtoms = 0)
        resultBuffer[p] = new ResultBuffer[diff[p].length];

        for (int k = 0; k < diff[p].length; k++)
          resultBuffer[p][k] = new ResultBuffer(); // [p][0] will be the isotope with lowest mass#
      }

      // of all features with fitting rt
      // and mz
      boolean trueBuffers[] = new boolean[diff.length];
      Arrays.fill(trueBuffers, false);

      for (int j = 0; j < groupedPeaks.size(); j++) // go through all possible peaks
      {
        for (int p = 0; p < diff.length; p++) {

          for (int k = 0; k < diff[p].length; k++) // check for each peak if it is a possible
                                                   // feature
          // for
          // every diff[](isotope)
          { // this is necessary bc there might be more than one possible feature
            // j represents the row index in groupedPeaks
            // k represents the isotope number the peak will be a candidate for
            // p = pattern index for autoCarbon
            if (mzTolerance.checkWithinTolerance(groupedPeaks.get(0).getAverageMZ() + diff[p][k],
                groupedPeaks.get(j).getAverageMZ())) {
              // this will automatically add groupedPeaks[0] to the list -> isotope with
              // lowest mass
              resultBuffer[p][k].addFound(); // +1 result for isotope k
              resultBuffer[p][k].addRow(j); // row in groupedPeaks[]
              resultBuffer[p][k].addID(groupedPeaks.get(j).getID());
            }
          }
        }
      }

      boolean foundOne = false;

      for (int p = 0; p < diff.length; p++)
        if (checkIfAllTrue(resultBuffer[p])) { // this means that for every isotope we expected to
                                               // find,
          foundOne = true; // we found one or more possible features
          trueBuffers[p] = true;
          // logger.info("Row: " + i + " filled buffer[" + p +"]");
        }
      if (!foundOne) {
        finishedRows++;
        continue;
      }

      Candidates[] candidates = new Candidates[diff.length];
      for (int p = 0; p < diff.length; p++)
        candidates[p] = new Candidates(diff[p].length, minHeight, mzTolerance, pattern[p],
            massListName, plh, ratingType);

      for (int p = 0; p < diff.length; p++) {
        if (!trueBuffers[p])
          continue;
        for (int k = 0; k < resultBuffer[p].length; k++) // reminder: resultBuffer.length =
                                                         // diff.length
        {
          for (int l = 0; l < resultBuffer[p][k].getFoundCount(); l++) {
            // k represents index resultBuffer[k] and thereby the isotope number
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
          // logger.info("Row: " + i + " filled candidates[" + p + "]");
        }
      }
      if (!foundOne) {
        finishedRows++;
        // logger.info("Not enough valid candidates for parent feature " +
        // groupedPeaks.get(0).getAverageMZ() + "\talthough enough peaks were found.") ;
        continue; // jump to next i
      }

      // find best result now, first we have to calc avg ratings if specified by user
      int bestPatternIndex = 0;
      double bestRating = 0.0;
      for (int p = 0; p < diff.length; p++) {

        if (!trueCandidates[p])
          continue;

        if (accurateAvgIntensity)
          candidates[p].calcAvgRatings();
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
      } // TODO: this shouldnt be needed, fix the bug that causes the crash later on.
        // this happens occasionally if the user wants to do accurate average but does not filter
        // by RT. then possible isotope peaks are found, although they are not detected at the same
        // time. This will result in the candidates return -1.0 which will sooner or later return a
        // null pointer Fixing this will be done in a future update, but needs a rework of the
        // candidates class.
        // The results you miss by skipping here would have not been valid results anyway, so this
        // is not urgent. Will be nicer though, because of cleaner code.

//      PeakListRow parent = copyPeakRow(peakList.getRow(i));
      PeakListRow parent = copyPeakRow(plh.getRowByID(candidates[bestPatternIndex].get(0).getCandID()));

      if (resultMap.containsID(parent.getID())) // if we can assign this row multiple times we
                                                // have to copy the comment, because adding it to
                                                // the map twice will overwrite the results
        addComment(parent, resultMap.getRowByID(parent.getID()).getComment());

      addComment(parent, parent.getID() + "--IS PARENT--"); // ID is added to be able to sort by
      // comment to bring all isotope patterns together

      if (carbonRange != 1)
        addComment(parent, "BestPattern: " + pattern[bestPatternIndex].getDescription());

      resultMap.addRow(parent); // add results to resultPeakList

      DataPoint[] dp = new DataPoint[pattern[bestPatternIndex].getNumberOfDataPoints()];
      // we need this to add the IsotopePattern later on

      if (accurateAvgIntensity) {
        dp[0] = new SimpleDataPoint(parent.getAverageMZ(),
            candidates[bestPatternIndex].getAvgHeight(0));
      } else {
        dp[0] = new SimpleDataPoint(parent.getAverageMZ(), parent.getAverageHeight());
      }

      for (int k = 1; k < candidates[bestPatternIndex].size(); k++) // we skip k=0 because ==
                                                                    // groupedPeaks[0]/
      // ==candidates.get(0) which we added before
      {
        PeakListRow child =
            copyPeakRow(plh.getRowByID(candidates[bestPatternIndex].get(k).getCandID()));
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
            pattern[bestPatternIndex].getHighestDataPointIndex()));
        if (accurateAvgIntensity)
          addComment(parent, " Avg pattern rating: "
              + round(candidates[bestPatternIndex].getAvgAccAvgRating(), 3));
        else
          addComment(parent,
              " pattern rating: " + round(candidates[bestPatternIndex].getSimpleAvgRating(), 3));

        ;
        addComment(child,
            (parent.getID() + "-Parent ID" + " m/z-shift(ppm): "
                + round(((child.getAverageMZ() - parent.getAverageMZ()) - diff[bestPatternIndex][k])
                    / child.getAverageMZ() * 1E6, 2)
                + " I(c)/I(p): "
                + round(child.getAverageHeight() / plh
                    .getRowByID(candidates[bestPatternIndex]
                        .get(pattern[bestPatternIndex].getHighestDataPointIndex()).getCandID())
                    .getAverageHeight(), 2)
                + " Identity: " + pattern[bestPatternIndex].getIsotopeComposition(k) + " Rating: "
                + round(candidates[bestPatternIndex].get(k).getRating(), 3) + average));

        resultMap.addRow(child);
      }

      IsotopePattern resultPattern = new SimpleIsotopePattern(dp, IsotopePatternStatus.DETECTED,
          element + " monoisotopic mass: " + parent.getAverageMZ());
      parent.getBestPeak().setIsotopePattern(resultPattern);

      for (int j = 1; j < diff[bestPatternIndex].length; j++)
        resultMap.getRowByID(candidates[bestPatternIndex].get(j).getCandID()).getBestPeak()
            .setIsotopePattern(resultPattern);

      if (isCanceled())
        return;

      finishedRows++;
    }

    ArrayList<Integer> keys = resultMap.getAllKeys();
    for (int j = 0; j < keys.size(); j++)
      resultPeakList.addRow(resultMap.getRowByID(keys.get(j)));

    if (resultPeakList.getNumberOfRows() > 1)
      addResultToProject(/* resultPeakList */);
    else
      message = "Element not found.";
    setStatus(TaskStatus.FINISHED);

  }

  /**
   * 
   * @param b
   * @return true if every b[i].getFoundCount != 0
   */
  private boolean checkIfAllTrue(ResultBuffer[] b) {
    for (int i = 0; i < b.length; i++)
      if (b[i].getFoundCount() == 0)
        return false;
    return true;
  }

  private boolean checkIfAllTrue(Candidate[] cs) {
    for (Candidate c : cs)
      if (c.getRating() == 0)
        return false;
    return true;
  }

  /**
   * This calculates the isotope pattern using ExtendedIsotopePattern and creates an
   * ArrayList<Double> that will contain the mass shift for every expected isotope peak relative to
   * the one with the lowest mass.
   * 
   * @return
   */
  private double[][] setUpDiffAutoCarbon() {

    // ArrayList<Double> diff = new ArrayList<Double>(2);

    double[][] diff;

    if (scanType == ScanType.AUTOCARBON) {

      String[] strPattern = new String[carbonRange];

      ExtendedIsotopePattern patternBuffer[] = new ExtendedIsotopePattern[carbonRange];

      // in the following for we calculate up the patterns
      for (int p = 0; p < carbonRange; p++) {
        if (p + autoCarbonMin != 0)
          strPattern[p] = "C" + (p + autoCarbonMin) + element;
        else
          strPattern[p] = element;

        try {
          patternBuffer[p] =
              (ExtendedIsotopePattern) IsotopePatternCalculator.calculateIsotopePattern(
                  strPattern[p], 0.001, mergeWidth, charge, polarityType, true);
          patternBuffer[p] = (ExtendedIsotopePattern) IsotopePatternCalculator
              .removeDataPointsBelowIntensity(patternBuffer[p], minPatternIntensity);
        } catch (Exception e) {
          logger.warning("The entered Sum formula is invalid.");
          return null;
        }
      }

      int sizeCounter = 0;
      // in this for we check how much of the patterns match the autoCarbonMinPatternSize criteria
      // if they dont fit we null them
      for (int p = 0; p < carbonRange; p++) {
        if (patternBuffer[p].getNumberOfDataPoints() >= autoCarbonMinPatternSize) {
          sizeCounter++;
        } else {
          patternBuffer[p] = null;
        }
      }

      if (sizeCounter == 0)
        throw new MSDKRuntimeException(
            "Min pattern size excludes every calculated isotope pattern.\nPlease increase min pattern intensity for more data points or decrease the minimum pattern size.");

      logger.info("about to add " + sizeCounter + " patterns to the scan.");
      diff = new double[sizeCounter][];
      int addCounter = 0;
      pattern = new ExtendedIsotopePattern[sizeCounter];
      for (int p = 0; p < carbonRange; p++) {

        if (patternBuffer[p] == null)
          continue;

        pattern[addCounter] = patternBuffer[p];
        DataPoint[] points = patternBuffer[p].getDataPoints();
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
      pattern = new ExtendedIsotopePattern[1];
      pattern[0] = (ExtendedIsotopePattern) IsotopePatternCalculator
          .calculateIsotopePattern(element, 0.001, mergeWidth, charge, polarityType, true);
      pattern[0] = (ExtendedIsotopePattern) IsotopePatternCalculator
          .removeDataPointsBelowIntensity(pattern[0], minPatternIntensity);
      DataPoint[] points = pattern[0].getDataPoints();
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
   * 
   * @param pL
   * @param parentIndex index of possible parent peak
   * @param maxMass
   * @return will return ArrayList<PeakListRow> of all peaks within the range of pL[parentIndex].mz
   *         -> pL[parentIndex].mz+maxMass
   */
  private ArrayList<PeakListRow> groupPeaks(PeakListRow[] pL, int parentIndex, double maxDiff) {

    ArrayList<PeakListRow> buf = new ArrayList<PeakListRow>();

    buf.add(pL[parentIndex]); // this means the result will contain row(parentIndex) itself

    double mz = pL[parentIndex].getAverageMZ();
    double rt = pL[parentIndex].getAverageRT();

    for (int i = parentIndex + 1; i < pL.length; i++) // will not add the parent peak itself
    {
      PeakListRow r = pL[i];
      // check for rt

      if (r.getAverageHeight() < minHeight)
        continue;

      if (!(pL[i].getAverageMZ() > mz
          && pL[i].getAverageMZ() <= (mz + maxDiff + mzTolerance.getMzTolerance()))) {
       
        if (pL[i].getAverageMZ() > (mz + maxDiff)) // since pL is sorted by ascending mass, we can
          // stop now
          return buf;
        continue;
      }

      if (checkRT && !rtTolerance.checkWithinTolerance(rt, r.getAverageRT()))
        continue;

      buf.add(pL[i]);
    }
    return buf;
  }

  /**
   * Create a copy of a peak list row.
   *
   * @param row the row to copy.
   * @return the newly created copy.
   */
  private static PeakListRow copyPeakRow(final PeakListRow row) {
    // Copy the peak list row.
    final PeakListRow newRow = new SimplePeakListRow(row.getID());
    PeakUtils.copyPeakListRowProperties(row, newRow);

    // Copy the peaks.
    for (final Feature peak : row.getPeaks()) {
      final Feature newPeak = new SimpleFeature(peak);
      PeakUtils.copyPeakProperties(peak, newPeak);
      newRow.addPeak(peak.getDataFile(), newPeak);
    }

    return newRow;
  }

  /**
   * 
   * @param pattern IsotopePattern to calculate intensity ratios of
   * @param index DataPoint index to normalize the intesitys to
   * @return String of intensity ratios seperated by ":"
   */
  private static String getIntensityRatios(IsotopePattern pattern, int index) {
    DataPoint[] dp = pattern.getDataPoints();

    String ratios = "";
    for (int i = 0; i < dp.length; i++)
      // ratios += round(dp[i].getIntensity(), 2) + ":";
      ratios += round((dp[i].getIntensity() / dp[index].getIntensity()), 2) + ":";
    ratios = (ratios.length() > 0) ? ratios.substring(0, ratios.length() - 1) : ratios;
    return ratios;
  }

  public static double round(double value, int places) { // https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
    if (places < 0)
      throw new IllegalArgumentException();

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
  public static void addComment(PeakListRow row, String str) {
    String current = row.getComment();
    if (current == null)
      row.setComment(str);
    else if (current.contains(str))
      return;
    else
      row.setComment(current + " " + str);
  }

  /**
   * Add peak list to project, delete old if requested, add description to result
   */
  public void addResultToProject() {
    // Add new peakList to the project
    project.addPeakList(resultPeakList);

    // Load previous applied methods
    for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimplePeakListAppliedMethod("IsotopePeakScanner", parameters));
  }

  private PolarityType getPeakListPolarity(PeakList peakList) {
    int[] scans = peakList.getRow(0).getPeaks()[0].getScanNumbers();
    RawDataFile raw = peakList.getRow(0).getPeaks()[0].getDataFile();
    return raw.getScan(scans[0]).getPolarity();
  }

  private boolean checkParameters() {
    if (charge == 0) {
      setStatus(TaskStatus.ERROR);
      MZmineCore.getDesktop().displayMessage(MZmineCore.getDesktop().getMainWindow(),
          "Error: charge may not be 0!");
      return false;
    }
    if (!FormulaUtils.checkMolecularFormula(element)) {
      setStatus(TaskStatus.ERROR);
      MZmineCore.getDesktop().displayMessage(MZmineCore.getDesktop().getMainWindow(),
          "Error: Invalid formula!");
      return false;
    }

    if (accurateAvgIntensity || ratingType == RatingType.TEMPAVG) {
      if (massListName.equals("") && ratingType == RatingType.TEMPAVG) {
        setStatus(TaskStatus.ERROR);
        MZmineCore.getDesktop().displayMessage(MZmineCore.getDesktop().getMainWindow(),
            "Error: Rating Type = temporary average but no masslist was selected.\nYou can"
                + " select a mass list without checking accurate average.");
        return false;
      }
      if (peakList.getNumberOfRawDataFiles() > 1) {
        setStatus(TaskStatus.ERROR);
        MZmineCore.getDesktop().displayMessage(MZmineCore.getDesktop().getMainWindow(),
            "The number of raw data files of peak list \"" + peakList.getName()
                + "\" is greater than 1. This is not supported by this module.");
        return false;
      }

      RawDataFile[] raws = peakList.getRawDataFiles();
      boolean foundMassList = false;
      for (RawDataFile raw : raws) {
        int scanNumbers[] = raw.getScanNumbers();
        for (int scan : scanNumbers) {
          MassList[] massLists = raw.getScan(scan).getMassLists();
          for (MassList list : massLists) {
            if (list.getName().equals(massListName))
              foundMassList = true;
          }
        }
      }
      if (foundMassList == false) {
        setStatus(TaskStatus.ERROR);
        MZmineCore.getDesktop().displayMessage(MZmineCore.getDesktop().getMainWindow(),
            "Peak list \"" + peakList.getName() + "\" does not contain a mass list by the name of "
                + massListName + ".");
        return false;
      }
    }
    return true;
  }
}


