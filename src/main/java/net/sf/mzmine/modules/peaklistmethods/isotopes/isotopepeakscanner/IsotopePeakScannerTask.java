/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.tests.ExtendedIsotopePattern;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class IsotopePeakScannerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ParameterSet parameters;
  private Range<Double> massRange;
  private Range<Double> rtRange;
  private boolean checkIntensity;
  private double minAbundance;
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
  private ExtendedIsotopePattern pattern;
  private PolarityType polarityType;
  private int charge;
  private boolean accurateAvgIntensity;
  private String massListName;
  private String ratingChoice;
  private double minAccurateAvgIntensity;

  private enum ScanType {
    neutralLoss, pattern
  };

  public enum RatingType {
    HIGHEST, TEMPAVG
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
    minAbundance = parameters.getParameter(IsotopePeakScannerParameters.minAbundance).getValue();
    // intensityDeviation =
    // parameters.getParameter(IsotopePeakScannerParameters.intensityDeviation).getValue()
    // / 100;
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

    if (accurateAvgIntensity && !checkIntensity) {
      accurateAvgIntensity = false;
    }

    if (ratingChoice.equals("Temporary average"))
      ratingType = RatingType.TEMPAVG;
    else
      ratingType = RatingType.HIGHEST;

    if (massListName.equals("") && ratingType == RatingType.TEMPAVG)
      throw new MSDKRuntimeException(
          "Error: Rating Type = temporary average but no masslist was selected.\nYou can still select a mass list without checking accurate average.");

    polarityType = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
    charge = (charge < 0) ? charge * -1 : charge;

    if (getPeakListPolarity(peakList) != polarityType)
      logger.warning("PeakList.polarityType does not match selected polarity. "
          + getPeakListPolarity(peakList).toString() + "!=" + polarityType.toString());

    scanType = ScanType.pattern;

    if (suffix.equals("auto")) {
      suffix = "_-Pat_" + element + "-RT" + checkRT + "-INT" + checkIntensity + "-R" + minRating
          + "_results";
    }

    message = "Got paramenters..."; // TODO
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
    setStatus(TaskStatus.PROCESSING);

    totalRows = peakList.getNumberOfRows();

    ArrayList<Double> diff = setUpDiff(scanType);
    if (diff == null) {
      message = "ERROR: could not set up diff.";
      return;
    }

    // get all rows and sort by m/z
    PeakListRow[] rows = peakList.getRows();
    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    PeakListHandler plh = new PeakListHandler();
    plh.setUp(peakList);
    // totalRows = rows.length;

    resultPeakList = new SimplePeakList(peakList.getName() + suffix, peakList.getRawDataFiles());
    PeakListHandler resultMap = new PeakListHandler();

    for (int i = 0; i < totalRows; i++) {
      // i will represent the index of the row in peakList
      if (peakList.getRow(i).getPeakIdentities().length > 0) {
        finishedRows++;
        continue;
      }

      message = "Row " + i + "/" + totalRows;
      massRange = mzTolerance.getToleranceRange(peakList.getRow(i).getAverageMZ());
      rtRange = rtTolerance.getToleranceRange(peakList.getRow(i).getAverageRT());

      // now get all peaks that lie within RT and maxIsotopeMassRange: pL[index].mz ->
      // pL[index].mz+maxMass
      ArrayList<PeakListRow> groupedPeaks =
          groupPeaks(rows, i, diff.get(diff.size() - 1).doubleValue());

      if (groupedPeaks.size() < 2) {
        finishedRows++;
        continue;
      }
      // else
      // logger.info("groupedPeaks.size > 2 in row: " + i + " size: " +
      // groupedPeaks.size());

      ResultBuffer[] resultBuffer = new ResultBuffer[diff.size()]; // this will store row indexes of
                                                                   // all features with fitting rt
                                                                   // and mz
      for (int a = 0; a < diff.size(); a++) // resultBuffer[i] index will represent Isotope[i] (if
                                            // numAtoms = 0)
        resultBuffer[a] = new ResultBuffer(); // [0] will be the isotope with lowest mass#

      for (int j = 0; j < groupedPeaks.size(); j++) // go through all possible peaks
      {
        for (int k = 0; k < diff.size(); k++) // check for each peak if it is a possible feature for
                                              // every diff[](isotope)
        { // this is necessary bc there might be more than one possible feature
          // j represents the row index in groupedPeaks
          // k represents the isotope number the peak will be a candidate for
          if (mzTolerance.checkWithinTolerance(groupedPeaks.get(0).getAverageMZ() + diff.get(k),
              groupedPeaks.get(j).getAverageMZ())) {
            // this will automatically add groupedPeaks[0] to the list -> isotope with
            // lowest mass
            resultBuffer[k].addFound(); // +1 result for isotope k
            resultBuffer[k].addRow(j); // row in groupedPeaks[]
            resultBuffer[k].addID(groupedPeaks.get(j).getID());
          }
        }
      }

      if (!checkIfAllTrue(resultBuffer)) // this means that for every isotope we expected to find,
                                         // we found one or more possible features
      {
        finishedRows++;
        continue;
      }

      Candidates candidates = new Candidates(diff.size(), minHeight, mzTolerance, pattern,
          massListName, plh, ratingType);

      for (int k = 0; k < resultBuffer.length; k++) // reminder: resultBuffer.length = diff.size()
      {
        for (int l = 0; l < resultBuffer[k].getFoundCount(); l++) {
          // k represents index resultBuffer[k] and thereby the isotope number
          // l represents the number of results in resultBuffer[k]
          candidates.checkForBetterRating(k, groupedPeaks.get(0),
              groupedPeaks.get(resultBuffer[k].getRow(l)), minRating, checkIntensity);

        }
      }

      if (!checkIfAllTrue(candidates.getCandidates())) {
        finishedRows++;
        // logger.info("Not enough valid candidates for parent feature " +
        // groupedPeaks.get(0).getAverageMZ() + "\talthough enough peaks were found.") ;
        continue; // jump to next i
      }

      String comParent = "", comChild = "";
      PeakListRow parent = copyPeakRow(peakList.getRow(i));
      
      if(resultMap.containsID(parent.getID())) // if we can assign this row multiple times we have to copy the comment, because adding it to the map twice will overwrite the results
        comParent += resultMap.getRowByID(parent.getID()).getComment();

      comParent += parent.getID() + "--IS PARENT--"; // ID is added to be able to sort by comment to bring all isotope patterns together
      addComment(parent, comParent);

      resultMap.addRow(parent); // add results to resultPeakList

      DataPoint[] dp = new DataPoint[candidates.size()]; // we need this to add the IsotopePattern
                                                         // later on

      if (accurateAvgIntensity) {
        candidates.calcAvgRatings(); // this is a final rating, with averaged intensities in all
                                     // mass lists that contain EVERY peak that was selected.
                                     // thats why we can only do it after ALL peaks have been found
        dp[0] = new SimpleDataPoint(parent.getAverageMZ(), candidates.getAvgHeight(0));
      } else {
        dp[0] = new SimpleDataPoint(parent.getAverageMZ(), parent.getAverageHeight());
      }

      for (int k = 1; k < candidates.size(); k++) // we skip k=0 because == groupedPeaks[0]/ ==candidates.get(0) which we
                                                  // added before
      {
        PeakListRow child = copyPeakRow(plh.getRowByID(candidates.get(k).getCandID()));
        if (accurateAvgIntensity) {
          dp[k] = new SimpleDataPoint(child.getAverageMZ(), candidates.getAvgHeight(k));
        } else {
          dp[k] = new SimpleDataPoint(child.getAverageMZ(), child.getAverageHeight());
        }

        String average = "";
        if (accurateAvgIntensity) {
          average = " AvgRating: " + round(candidates.getAvgRating(k), 3);
        }

        addComment(parent,
            "Intensity ratios: " + getIntensityRatios(pattern, pattern.getHighestDataPointIndex())
                + " Identity: " + pattern.getDetailedPeakDescription(0));
        if (accurateAvgIntensity)
          addComment(parent, " Avg pattern rating: " + round(candidates.getAvgAvgRatings(), 3));

        comChild = (parent.getID() + "-Parent ID" + " m/z-shift(ppm): "
            + round(((child.getAverageMZ() - parent.getAverageMZ()) - diff.get(k))
                / child.getAverageMZ() * 1E6, 2)
            + " I(c)/I(p): "
            + round(child.getAverageHeight()
                / plh.getRowByID(candidates.get(pattern.getHighestDataPointIndex()).getCandID())
                    .getAverageHeight(),
                2)
            + " Identity: " + pattern.getDetailedPeakDescription(k) + " Rating: "
            + round(candidates.get(k).getRating(), 3) + average);
        addComment(child, comChild);

        resultMap.addRow(child);
      }

      IsotopePattern resultPattern = new SimpleIsotopePattern(dp, IsotopePatternStatus.DETECTED,
          element + " monoisotopic mass: " + parent.getAverageMZ());
      parent.getBestPeak().setIsotopePattern(resultPattern);

      for (int j = 1; j < candidates.size(); j++)
        resultMap.getRowByID(candidates.get(j).getCandID()).getBestPeak()
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
   * @return true if every
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

  private ArrayList<Double> setUpDiff(ScanType scanType) {
    ArrayList<Double> diff = new ArrayList<Double>(2);

    pattern = new ExtendedIsotopePattern();
    pattern.setUpFromFormula(element, minAbundance, mergeWidth, minPatternIntensity);
    pattern.normalizePatternToHighestPeak();
    pattern.print();
    pattern.applyCharge(charge, polarityType);

    DataPoint[] points = pattern.getDataPoints();
    logger.info("DataPoints in Pattern: " + points.length);
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      diff.add(i, points[i].getMZ() - points[0].getMZ());
    }
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

      if (!rtTolerance.checkWithinTolerance(rt, r.getAverageRT()) && checkRT)
        continue;

      if (pL[i].getAverageMZ() > mz
          && pL[i].getAverageMZ() <= (mz + maxDiff + mzTolerance.getMzTolerance())) {
        buf.add(pL[i]);
      }

      if (pL[i].getAverageMZ() > (mz + maxDiff)) // since pL is sorted by ascending mass, we can
                                                 // stop now
        return buf;
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
}
