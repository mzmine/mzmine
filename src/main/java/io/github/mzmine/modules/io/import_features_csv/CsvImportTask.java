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

package io.github.mzmine.modules.io.import_features_csv;

import com.google.common.collect.Range;
import com.opencsv.CSVReader;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CsvImportTask extends AbstractTask {

  private final MZmineProject project;
  private final RawDataFile rawDataFile;
  private final File fileName;
  private final ParameterSet parameters;
  private final double percent = 0.0;

  CsvImportTask(MZmineProject project, ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    // first file only
    this.rawDataFile = parameters.getParameter(CsvImportParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];
    this.fileName = parameters.getParameter(CsvImportParameters.filename).getValue()[0];
    this.parameters = parameters;
  }


  @Override
  public String getTaskDescription() {
    return "Import csv file";
  }

  @Override
  public double getFinishedPercentage() {
    return percent;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      FileReader fileReader = new FileReader(fileName);
      CSVReader csvReader = new CSVReader(fileReader);
      ModularFeatureList newFeatureList = new ModularFeatureList(fileName.getName(), storage,
          rawDataFile);
      String[] dataLine;
      int counter = 0;
      while ((dataLine = csvReader.readNext()) != null) {
        if (isCanceled()) {
          return;
        }
        if (counter++ == 0) {
          continue;
        }
        double feature_mz = 0.0, mzMin = 0.0, mzMax = 0.0;
        float feature_rt = 0f, intensity = 0f, rtMin = 0f, rtMax = 0f, feature_height = 0f, abundance = 0f;
        Range<Float> finalRTRange;
        Range<Double> finalMZRange;
        Range<Float> finalIntensityRange;

        ModularFeatureListRow newRow = new ModularFeatureListRow(newFeatureList, counter - 1);
        for (int j = 0; j < dataLine.length; j++) {
          switch (j) {
            case 1:
              feature_mz = Double.parseDouble(dataLine[j]);
              break;
            case 2:
              mzMin = Double.parseDouble(dataLine[j]);
              break;
            case 3:
              mzMax = Double.parseDouble(dataLine[j]);
              break;
            case 4:
              // Retention times are taken in minutes
              feature_rt = (float) (Double.parseDouble(dataLine[j]) / 60.0);
              break;
            case 5:
              rtMin = (float) (Double.parseDouble(dataLine[j]) / 60.0);
              break;
            case 6:
              rtMax = (float) (Double.parseDouble(dataLine[j]) / 60.0);
              break;
            case 9:
              intensity = (float) Double.parseDouble(dataLine[j]);
              feature_height = (float) Double.parseDouble(dataLine[j]);
              break;
          }
        }
        finalMZRange = Range.closed(mzMin, mzMax);
        finalRTRange = Range.closed(rtMin, rtMax);
        finalIntensityRange = Range.singleton(intensity);
        Scan[] scanNumbers = {};
        DataPoint[] finalDataPoint = new DataPoint[1];
        finalDataPoint[0] = new SimpleDataPoint(feature_mz, feature_height);
        FeatureStatus status = FeatureStatus.UNKNOWN; // abundance unknown
        Scan representativeScan = null;

        final Scan s_no = rawDataFile.binarySearchClosestScan(feature_rt, 1);
        if (s_no != null && finalRTRange.contains(s_no.getRetentionTime())) {
          representativeScan = s_no;
          final int peakIndex = s_no.binarySearch(feature_mz, true);
          if (finalMZRange.contains(s_no.getMzValue(peakIndex))) {
            finalDataPoint[0] = new SimpleDataPoint(feature_mz, s_no.getIntensityValue(peakIndex));
          }
          scanNumbers = new Scan[]{representativeScan};
        }

        Feature feature = new ModularFeature(newFeatureList, rawDataFile, feature_mz, feature_rt,
            feature_height, abundance, scanNumbers, finalDataPoint, status, representativeScan,
            List.of(), finalRTRange, finalMZRange, finalIntensityRange);
        newRow.addFeature(rawDataFile, feature);
        newFeatureList.addRow(newRow);
      }

      if (isCanceled()) {
        return;
      }

      newFeatureList.setSelectedScans(rawDataFile, rawDataFile.getScanNumbers(1));
      newFeatureList.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(CsvImportModule.class, parameters,
              getModuleCallDate()));

      fileReader.close();
      project.addFeatureList(newFeatureList);
    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage(
          "Could not import feature list from file " + fileName.getName() + ": " + e.getMessage());
      return;
    }
    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }
}
