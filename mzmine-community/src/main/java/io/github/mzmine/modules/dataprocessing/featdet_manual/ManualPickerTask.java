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
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RangeUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

class ManualPickerTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int processedFiles, totalFiles;

  private final MZmineProject project;
  private final ModularFeatureList featureList;
  private FeatureListRow featureListRow;
  private RawDataFile dataFiles[];
  private Range<Double> mzRange;
  private Range<Float> rtRange;
  private final ParameterSet parameterSet;

  ManualPickerTask(MZmineProject project, FeatureListRow featureListRow, RawDataFile dataFiles[],
      ParameterSet parameters, FeatureList featureList) {
    super(null, Instant.now()); // we get passed a flist, so it should contain a storage

    this.project = project;
    this.featureListRow = featureListRow;
    this.dataFiles = dataFiles;
    this.featureList = (ModularFeatureList) featureList;

    // TODO: FloatRangeParameter
    rtRange = RangeUtils.toFloatRange(
        parameters.getParameter(ManualPickerParameters.retentionTimeRange).getValue());
    mzRange = parameters.getParameter(ManualPickerParameters.mzRange).getValue();
    this.parameterSet = parameters;
    totalFiles = dataFiles.length;
  }

  @Override
  public double getFinishedPercentage() {
    if (totalFiles == 0) {
      return 0;
    }
    return (double) processedFiles / totalFiles;
  }

  @Override
  public String getTaskDescription() {
    return "Manually picking features from " + Arrays.toString(dataFiles);
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.finest("Starting manual feature picker, RT: " + rtRange + ", m/z: " + mzRange);

    for (RawDataFile file : dataFiles) {
      final List<? extends Scan> selectedScans = featureList.getSeletedScans(file);
      final IonTimeSeries<?> series = IonTimeSeriesUtils.extractIonTimeSeries(file, selectedScans,
          mzRange, rtRange, featureList.getMemoryMapStorage());

      final Feature feature = featureListRow.getFeature(file);
      ModularFeature f = (ModularFeature)feature;
      f.set(FeatureDataType.class, series);
      FeatureDataUtils.recalculateIonSeriesDependingTypes(f);
      processedFiles++;
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
    }

    featureList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(ManualFeaturePickerModule.class, parameterSet,
            getModuleCallDate()));

    logger.finest("Finished manual feature picker, " + processedFiles + " files processed");

    setStatus(TaskStatus.FINISHED);

  }

}
