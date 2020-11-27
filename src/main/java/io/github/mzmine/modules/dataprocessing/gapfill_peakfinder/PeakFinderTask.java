/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import com.google.common.collect.Range;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

class PeakFinderTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private FeatureList peakList, processedPeakList;
  private String suffix;
  private double intTolerance;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private boolean rtCorrection;
  private ParameterSet parameters;
  private int totalScans;
  private AtomicInteger processedScans;
  private boolean MASTERLIST = true, removeOriginal;
  private int masterSample = 0;
  private boolean useParallelStream = false;

  PeakFinderTask(MZmineProject project, FeatureList peakList, ParameterSet parameters) {

    this.project = project;
    this.peakList = peakList;
    this.parameters = parameters;

    suffix = parameters.getParameter(PeakFinderParameters.suffix).getValue();
    intTolerance = parameters.getParameter(PeakFinderParameters.intTolerance).getValue();
    mzTolerance = parameters.getParameter(PeakFinderParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(PeakFinderParameters.RTTolerance).getValue();
    rtCorrection = parameters.getParameter(PeakFinderParameters.RTCorrection).getValue();
    removeOriginal = parameters.getParameter(PeakFinderParameters.autoRemove).getValue();
    useParallelStream = parameters.getParameter(PeakFinderParameters.useParallel).getValue();
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running gap filler on " + peakList);

    // Calculate total number of scans in all files
    for (RawDataFile dataFile : peakList.getRawDataFiles()) {
      totalScans += dataFile.getNumOfScans(1);
    }
    processedScans = new AtomicInteger();

    // Create new feature list
    processedPeakList = new ModularFeatureList(peakList + " " + suffix, peakList.getRawDataFiles());

    // Fill new feature list with empty rows
    for (int row = 0; row < peakList.getNumberOfRows(); row++) {
      FeatureListRow sourceRow = peakList.getRow(row);
      FeatureListRow newRow = new ModularFeatureListRow((ModularFeatureList) processedPeakList, sourceRow.getID());
      newRow.setComment(sourceRow.getComment());
      for (FeatureIdentity ident : sourceRow.getPeakIdentities()) {
        newRow.addFeatureIdentity(ident, false);
      }
      if (sourceRow.getPreferredFeatureIdentity() != null) {
        newRow.setPreferredFeatureIdentity(sourceRow.getPreferredFeatureIdentity());
      }
      processedPeakList.addRow(newRow);
    }

    if (rtCorrection) {
      totalScans *= 2;
      // Fill the gaps of a random sample using all the other samples and
      // take it as master list
      // to fill the gaps of the other samples
      masterSample = (int) Math.floor(Math.random() * peakList.getNumberOfRawDataFiles());
      fillList(MASTERLIST);

      // Process all raw features files
      fillList(!MASTERLIST);

    } else {

      // Process all raw features files
      IntStream rawStream = IntStream.range(0, peakList.getNumberOfRawDataFiles());
      if (useParallelStream)
        rawStream = rawStream.parallel();

      rawStream.forEach(i -> {
        // Canceled?
        if (isCanceled()) {
          // inside stream - only skips this element
          return;
        }
        RawDataFile dataFile = peakList.getRawDataFile(i);

        List<Gap> gaps = new ArrayList<Gap>();

        // Fill each row of this raw features file column, create new empty
        // gaps
        // if necessary
        for (int row = 0; row < peakList.getNumberOfRows(); row++) {
          // Canceled?
          if (isCanceled()) {
            // inside stream - only skips this element
            return;
          }

          FeatureListRow sourceRow = peakList.getRow(row);
          FeatureListRow newRow = processedPeakList.getRow(row);

          Feature sourcePeak = sourceRow.getFeature(dataFile);

          if (sourcePeak == null) {

            // Create a new gap

            Range<Double> mzRange = mzTolerance.getToleranceRange(sourceRow.getAverageMZ());
            Range<Float> rtRange = rtTolerance.getToleranceRange(sourceRow.getAverageRT());

            Gap newGap = new Gap(newRow, dataFile, mzRange, rtRange, intTolerance);

            gaps.add(newGap);

          } else {
            newRow.addFeature(dataFile, sourcePeak);
          }
        }

        // Stop processing this file if there are no gaps
        if (gaps.size() == 0) {
          processedScans.addAndGet(dataFile.getNumOfScans());
          return;
        }

        // Get all scans of this features file
        int scanNumbers[] = dataFile.getScanNumbers(1);

        // Process each scan
        for (int scanNumber : scanNumbers) {
          // Canceled?
          if (isCanceled()) {
            // inside stream - only skips this element
            return;
          }

          // Get the scan
          Scan scan = dataFile.getScan(scanNumber);

          // Feed this scan to all gaps
          for (Gap gap : gaps) {
            gap.offerNextScan(scan);
          }

          processedScans.incrementAndGet();
        }

        // Finalize gaps
        for (Gap gap : gaps) {
          gap.noMoreOffers();
        }
      });
    }
    // terminate - stream only skips all elements
    if (isCanceled())
      return;

    // Append processed feature list to the project
    project.addFeatureList(processedPeakList);

    // Add quality parameters to peaks
    //QualityParameters.calculateQualityParameters(processedPeakList);

    // Add task description to peakList
    processedPeakList
        .addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod("Gap filling ", parameters));

    // Remove the original peaklist if requested
    if (removeOriginal)
      project.removeFeatureList(peakList);

    logger.info("Finished gap-filling on " + peakList);
    setStatus(TaskStatus.FINISHED);

  }

  public void fillList(boolean masterList) {
    for (int i = 0; i < peakList.getNumberOfRawDataFiles(); i++) {
      if (i != masterSample) {

        RawDataFile datafile1;
        RawDataFile datafile2;

        if (masterList) {
          datafile1 = peakList.getRawDataFile(masterSample);
          datafile2 = peakList.getRawDataFile(i);
        } else {
          datafile1 = peakList.getRawDataFile(i);
          datafile2 = peakList.getRawDataFile(masterSample);
        }
        RegressionInfo info = new RegressionInfo();

        for (FeatureListRow row : peakList.getRows()) {
          Feature peaki = row.getFeature(datafile1);
          Feature peake = row.getFeature(datafile2);
          if (peaki != null && peake != null) {
            info.addData(peake.getRT(), peaki.getRT());
          }
        }

        info.setFunction();

        // Canceled?
        if (isCanceled()) {
          return;
        }

        Vector<Gap> gaps = new Vector<Gap>();

        // Fill each row of this raw features file column, create new empty
        // gaps
        // if necessary
        for (int row = 0; row < peakList.getNumberOfRows(); row++) {
          FeatureListRow sourceRow = peakList.getRow(row);
          FeatureListRow newRow = processedPeakList.getRow(row);

          Feature sourcePeak = sourceRow.getFeature(datafile1);

          if (sourcePeak == null) {

            // Create a new gap

            double mz = sourceRow.getAverageMZ();
            double rt2 = -1;
            if (!masterList) {
              if (processedPeakList.getRow(row).getFeature(datafile2) != null) {
                rt2 = processedPeakList.getRow(row).getFeature(datafile2).getRT();
              }
            } else {
              if (peakList.getRow(row).getFeature(datafile2) != null) {
                rt2 = peakList.getRow(row).getFeature(datafile2).getRT();
              }
            }

            if (rt2 > -1) {

              float rt = (float) info.predict(rt2);

              if (rt != -1) {

                Range<Double> mzRange = mzTolerance.getToleranceRange(mz);
                Range<Float> rtRange = rtTolerance.getToleranceRange(rt);

                Gap newGap = new Gap(newRow, datafile1, mzRange, rtRange, intTolerance);

                gaps.add(newGap);
              }
            }

          } else {
            newRow.addFeature(datafile1, sourcePeak);
          }

        }

        // Stop processing this file if there are no gaps
        if (gaps.size() == 0) {
          processedScans.addAndGet(datafile1.getNumOfScans());
          continue;
        }

        // Get all scans of this features file
        int scanNumbers[] = datafile1.getScanNumbers(1);

        // Process each scan
        for (int scanNumber : scanNumbers) {

          // Canceled?
          if (isCanceled()) {
            return;
          }

          // Get the scan
          Scan scan = datafile1.getScan(scanNumber);

          // Feed this scan to all gaps
          for (Gap gap : gaps) {
            gap.offerNextScan(scan);
          }
          processedScans.incrementAndGet();
        }

        // Finalize gaps
        for (Gap gap : gaps) {
          gap.noMoreOffers();
        }
      }
    }
  }

  public double getFinishedPercentage() {
    if (totalScans == 0 || processedScans == null) {
      return 0;
    }
    return (double) processedScans.get() / (double) totalScans;
  }

  public String getTaskDescription() {
    return "Gap filling " + peakList;
  }

  FeatureList getPeakList() {
    return peakList;
  }

}
