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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureConvertors;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
  private final ParameterSet parameterSet;

  ManualPickerTask(MZmineProject project, FeatureListRow featureListRow, RawDataFile dataFiles[],
      ManualPickerParameters parameters, FeatureList featureList, FeatureTableFX table) {
    super(null, Instant.now()); // we get passed a flist, so it should contain a storage

    this.project = project;
    this.featureListRow = featureListRow;
    this.dataFiles = dataFiles;
    this.featureList = (ModularFeatureList) featureList;
    this.table = table;

    // TODO: FloatRangeParameter
    rtRange = RangeUtils.toFloatRange(parameters.getParameter(ManualPickerParameters.retentionTimeRange).getValue());
    mzRange = parameters.getParameter(ManualPickerParameters.mzRange).getValue();
    this.parameterSet = parameters;
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

    Scan[][] scanNumbersRaw = new Scan[dataFiles.length][];
    // Calculate total number of scans to process
    for (int i = 0; i < dataFiles.length; i++) {
      scanNumbersRaw[i] = dataFiles[i].getScanNumbers(1, rtRange);
      totalScans += scanNumbersRaw.length;
    }

    // Find feature in each data file
    for (int i = 0; i < dataFiles.length; i++) {
      RawDataFile dataFile = dataFiles[i];
      ManualFeature newFeature = new ManualFeature(dataFile);
      boolean dataPointFound = false;

      Scan[] scanNumbers = scanNumbersRaw[i];

      for (Scan scan : scanNumbers) {
        if (isCanceled())
          return;

        // Find most intense m/z feature
        DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

        if (basePeak != null) {
          if (basePeak.getIntensity() > 0)
            dataPointFound = true;
          newFeature.addDatapoint(scan, basePeak);
        } else {
          final double mzCenter = (mzRange.lowerEndpoint() + mzRange.upperEndpoint()) / 2.0;
          DataPoint fakeDataPoint = new SimpleDataPoint(mzCenter, 0);
          newFeature.addDatapoint(scan, fakeDataPoint);
        }
        processedScans++;
      }

      if (dataPointFound) {
        newFeature.finalizeFeature();
        newFeature.setFeatureList(featureList);
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

    featureList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        ManualFeaturePickerModule.class, parameterSet, getModuleCallDate()));

    logger.finest("Finished manual feature picker, " + processedScans + " scans processed");

    setStatus(TaskStatus.FINISHED);

  }

}
