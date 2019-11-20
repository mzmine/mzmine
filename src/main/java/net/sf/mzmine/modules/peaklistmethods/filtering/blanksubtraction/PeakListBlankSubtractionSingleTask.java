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


package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class PeakListBlankSubtractionSingleTask extends AbstractTask {

  private static final String ALIGNED_BLANK_NAME = "Aligned blank";

  private NumberFormat mzFormat, rtFormat, percFormat;

  private static Logger logger =
      Logger.getLogger(PeakListBlankSubtractionSingleTask.class.getName());

  private PeakListBlankSubtractionParameters parameters;

  private boolean checkFoldChange;
  private double foldChange;
  private int minBlankDetections;

  private PeakList alignedFeatureList;
  private PeakListRow[] alignedFeatureRows;
  private RawDataFile[] blankRaws;
  private RawDataFile thisRaw;

  private int finishedRows, totalRows;

  public PeakListBlankSubtractionSingleTask(PeakListBlankSubtractionParameters parameters,
      RawDataFile thisRaw, PeakListRow[] rows) {

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    percFormat = new DecimalFormat("0.0 %");

    this.parameters = parameters;

    this.checkFoldChange =
        parameters.getParameter(PeakListBlankSubtractionParameters.foldChange).getValue();
    this.foldChange = parameters.getParameter(PeakListBlankSubtractionParameters.foldChange)
        .getEmbeddedParameter().getValue();
    this.blankRaws = parameters.getParameter(PeakListBlankSubtractionParameters.blankRawDataFiles)
        .getValue().getMatchingRawDataFiles();
    this.alignedFeatureList =
        parameters.getParameter(PeakListBlankSubtractionParameters.alignedPeakList).getValue()
            .getMatchingPeakLists()[0];
    this.minBlankDetections =
        parameters.getParameter(PeakListBlankSubtractionParameters.minBlanks).getValue();
    this.thisRaw = thisRaw;
    this.alignedFeatureRows = rows;

    finishedRows = 0;
    totalRows = alignedFeatureList.getNumberOfRows();
  }

  @Override
  public String getTaskDescription() {
    return "Blank subtraction for features in " + thisRaw.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return finishedRows / totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.FINISHED);
    ArrayList<PeakListRow> rows = new ArrayList<>();

    // get the rows that contain peaks from the current raw data file
    for (PeakListRow row : alignedFeatureRows) {
      for (RawDataFile raw : row.getRawDataFiles()) {
        if (raw.equals(thisRaw)) {
          rows.add(row);
          break;
        }
      }
    }

    if (rows.isEmpty()) {
      logger.info("Raw data file " + thisRaw.getName() + " did not have any features in "
          + alignedFeatureList.getName());
      finishedRows = 1;
      totalRows = 1;
      setStatus(TaskStatus.FINISHED);
      return;
    }

    totalRows = rows.size();
    int featuresRemoved = 0;

    List<RawDataFile> blankRawsList = Arrays.asList(blankRaws);

    for (PeakListRow row : rows) {

      if (getStatus() == TaskStatus.CANCELED)
        return;

      if (checkFoldChange && (getIntensityIncrease(row, thisRaw, blankRaws) > foldChange)) {
        finishedRows++;
        continue;
      }

      int blankNum = 0;
      for (RawDataFile raw : row.getRawDataFiles()) {
        if (blankRawsList.contains(raw))
          blankNum++;

        if (blankNum >= minBlankDetections)
          break;
      }

      if (blankNum >= minBlankDetections) {
        // other threads are using the same array, so we have to do this synchronized.
        synchronized (row) {
          row.removePeak(thisRaw);
          featuresRemoved++;
        }
      }

      finishedRows++;
    }

    logger.finest(thisRaw.getName() + " - Removed " + featuresRemoved + " features.");
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Calculates the intensity increase of a sample feature compared to a blank/control sample in an
   * aligned feature list row.
   * 
   * @param row The aligned feature list row.
   * @param thisRaw The raw data file of the sample.
   * @param blankRaws The raw data file of blanks/control samples.
   * @return The intensity increase.
   */
  private double getIntensityIncrease(PeakListRow row, RawDataFile thisRaw,
      RawDataFile[] blankRaws) {
    Feature thisFeature = row.getPeak(thisRaw);
    Feature[] blankFeatures = new Feature[blankRaws.length];

    for (int i = 0; i < blankFeatures.length; i++) {
      blankFeatures[i] = row.getPeak(blankRaws[i]);
    }

    double avgBlankIntensity = 0;
    int validBlanks = 0;
    for (Feature blankFeature : blankFeatures) {
      // feature might not show up in all blanks
      if (blankFeature == null)
        continue;

      validBlanks++;
      avgBlankIntensity += blankFeature.getHeight();
    }
    // check for safety, but this should not happen
    // if the feature did not exist in blanks then we return the height.
    if (validBlanks == 0)
      return thisFeature.getHeight();

    avgBlankIntensity /= validBlanks;

    return (thisFeature.getHeight() / avgBlankIntensity);
  }

  
  /**
   * Correlates the features by mz, rt and rt range.
   * 
   * @param peak
   * @param aligned
   * @param mp Matching parameters to look at.
   * @return A score between 1 and 0. 1 = best, 0 = worst
   */
  /*private double getSimpleRowVsAlignedRowScores(PeakListRow peak, PeakListRow aligned,
      MatchingParameter[] mp) {

    double score = 1;
    for (MatchingParameter p : mp) {
      if (p == MatchingParameter.MZ) {
        score *= compare(peak.getAverageMZ(), aligned.getAverageMZ());
      } else if (p == MatchingParameter.RT) {
        score *= compare(peak.getAverageRT(), aligned.getAverageRT());
      } else if (p == MatchingParameter.RTRANGE) {
        score *= scoreRtRange(peak.getBestPeak().getRawDataPointsRTRange(),
            PeakUtils.getPeakListRowAvgRtRange(aligned));
      }
    }
    return score;
  }*/

  /**
   * Returns a the deviation of mz1 from mz2.
   * 
   * @param mz1 The actual number
   * @param mz2 The reference number
   * @return d1/d2
   */
  private double compare(double d1, double d2) {
    double d = (d1 / d2);
    if (d > 1)
      d = 1 / d;
    return d;
  }

  /**
   * Returns a deviation of the upper and lower boundary of two ranges.
   * 
   * @param r1
   * @param r2
   * @return
   */
  private double scoreRtRange(Range<Double> r1, Range<Double> r2) {

    double lower = compare(r1.lowerEndpoint(), r2.lowerEndpoint());
    double upper = compare(r1.upperEndpoint(), r2.upperEndpoint());

    return (lower + upper) / 2;
  }
}
