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

package io.github.mzmine.modules.dataprocessing.featdet_msms;

import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.util.FeatureTableFXUtil;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import javafx.application.Platform;

public class MsMsPeakPickingTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int processedScans, totalScans;

  private final MZmineProject project;
  private final RawDataFile dataFile;
  private double binSize, binTime;
  private final ScanSelection scanSelection;

  private ModularFeatureList newFeatureList;

  public MsMsPeakPickingTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters) {
    this.project = project;
    this.dataFile = dataFile;
    binSize = parameters.getParameter(MsMsPeakPickerParameters.mzWindow).getValue();
    binTime = parameters.getParameter(MsMsPeakPickerParameters.rtWindow).getValue();

    scanSelection = parameters.getParameter(MsMsPeakPickerParameters.scanSelection).getValue();
    newFeatureList = new ModularFeatureList(dataFile.getName() + " MS/MS features", dataFile);
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0f;
    return (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Building MS/MS feature list based on MS/MS from " + dataFile;
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    final Scan scans[] = scanSelection.getMatchingScans(dataFile);
    totalScans = scans.length;

    // TODO: Is it correct?
    if (totalScans == 0) {
      throw new IllegalStateException("Unable to create MS/MS feature list because there are no MS/MS scans.");
    }

    for (Scan scan : scans) {
      if (isCanceled())
        return;

      // Get the MS Scan
      Scan bestScan = null;
      Range<Float> rtWindow = Range.closed(scan.getRetentionTime() - (float) (binTime / 2.0),
          scan.getRetentionTime() + (float) (binTime / 2.0));
      Range<Double> mzWindow = Range.closed(scan.getPrecursorMZ() - (binSize / 2.0),
          scan.getPrecursorMZ() + (binSize / 2.0));
      DataPoint point;
      DataPoint maxPoint = null;
      int[] regionScanNumbers = dataFile.getScanNumbers(1, rtWindow);
      for (int regionScanNumber : regionScanNumbers) {
        Scan regionScan = dataFile.getScan(regionScanNumber);
        point = ScanUtils.findBasePeak(regionScan, mzWindow);

        // no datapoint found
        if (point == null) {
          continue;
        }
        if (maxPoint == null) {
          maxPoint = point;
        }
        int result = Double.compare(maxPoint.getIntensity(), point.getIntensity());
        if (result <= 0) {
          maxPoint = point;
          bestScan = regionScan;
        }

      }

      // if no representative dataPoint
      if (bestScan == null) {
        continue;
      }

      assert maxPoint != null;

      ModularFeature newFeature = new ModularFeature(newFeatureList, dataFile, scan.getPrecursorMZ(),
          bestScan.getRetentionTime(), maxPoint.getIntensity(), maxPoint.getIntensity(),
          new int[] {bestScan.getScanNumber()}, new DataPoint[] {maxPoint}, FeatureStatus.DETECTED,
          bestScan.getScanNumber(), scan.getScanNumber(), new int[] {},
          Range.singleton(bestScan.getRetentionTime()), Range.singleton(scan.getPrecursorMZ()),
          Range.singleton((float) maxPoint.getIntensity()));

      ModularFeatureListRow newFeatureListRow =
          new ModularFeatureListRow(newFeatureList, scan.getScanNumber(), dataFile, newFeature);

      newFeatureList.addRow(newFeatureListRow);
      processedScans++;
    }

    // Add new feature list to the project
    project.addFeatureList(newFeatureList);

    // Show new feature list window
    Platform.runLater(() -> {
      FeatureTableFXUtil.addFeatureTableTab(newFeatureList);
    });

    logger.info(
        "Finished MS/MS feature builder on " + dataFile + ", " + processedScans + " scans processed");

    setStatus(TaskStatus.FINISHED);
  }

}
