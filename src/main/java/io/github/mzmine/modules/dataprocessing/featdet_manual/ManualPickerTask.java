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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

class ManualPickerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int processedScans, totalScans;

  private final MZmineProject project;
  private final FeatureTableFX table;
  private final ModularFeatureList featureList;
  private FeatureListRow featureListRow;
  private RawDataFile dataFiles[];
  private Range<Double> mzRange;
  private Range<Float> rtRange;

  ManualPickerTask(MZmineProject project, FeatureListRow featureListRow, RawDataFile dataFiles[],
      ManualPickerParameters parameters, FeatureList featureList, FeatureTableFX table) {

    this.project = project;
    this.featureListRow = featureListRow;
    this.dataFiles = dataFiles;
    this.featureList = (ModularFeatureList) featureList;
    this.table = table;

    // TODO: FloatRangeParameter
    rtRange = RangeUtils.toFloatRange(parameters.getParameter(ManualPickerParameters.retentionTimeRange).getValue());
    mzRange = parameters.getParameter(ManualPickerParameters.mzRange).getValue();

  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    return (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Manually picking features from " + Arrays.toString(dataFiles);
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.finest("Starting manual feature picker, RT: " + rtRange + ", m/z: " + mzRange);

    // Calculate total number of scans to process
    for (RawDataFile dataFile : dataFiles) {
      int[] scanNumbers = dataFile.getScanNumbers(1, rtRange);
      totalScans += scanNumbers.length;
    }

    // Find feature in each data file
    for (RawDataFile dataFile : dataFiles) {

      ManualFeature newFeature = new ManualFeature(dataFile);
      boolean dataPointFound = false;

      int[] scanNumbers = dataFile.getScanNumbers(1, rtRange);

      for (int scanNumber : scanNumbers) {

        if (isCanceled())
          return;

        // Get next scan
        Scan scan = dataFile.getScan(scanNumber);

        // Find most intense m/z feature
        DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

        if (basePeak != null) {
          if (basePeak.getIntensity() > 0)
            dataPointFound = true;
          newFeature.addDatapoint(scan.getScanNumber(), basePeak);
        } else {
          final double mzCenter = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2.0;
          DataPoint fakeDataPoint = new SimpleDataPoint(mzCenter, 0);
          newFeature.addDatapoint(scan.getScanNumber(), fakeDataPoint);
        }

        processedScans++;

      }

      if (dataPointFound) {
        newFeature.finalizeFeature();
        if (newFeature.getArea() > 0) {
          featureListRow.addFeature(dataFile,
              FeatureConvertors.ManualFeatureToModularFeature(featureList, newFeature));
        }
      } else {
        featureListRow.removeFeature(dataFile);
      }

    }

    // Notify the GUI that feature list contents have changed
    if (featureList != null) {
      // Check if the feature list row has been added to the feature list,
      // and
      // if it has not, add it
      List<FeatureListRow> rows = new ArrayList<>(featureList.getRows());
      if (!rows.contains(featureListRow)) {
        featureList.addRow(featureListRow);
      }

      // Add quality parameters to features
      QualityParameters.calculateAndSetModularQualityParameters((ModularFeatureList) featureList);

      // project.notifyObjectChanged(featureList, true);
    }
    if (table != null) {
      // TODO:
      //((AbstractTableModel) table.getModel()).fireTableDataChanged();
    }

    logger.finest("Finished manual feature picker, " + processedScans + " scans processed");

    setStatus(TaskStatus.FINISHED);

  }

}
