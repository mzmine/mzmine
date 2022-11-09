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

package io.github.mzmine.modules.dataprocessing.norm_linear;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureMeasurementType;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.Hashtable;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class LinearNormalizerTask extends AbstractTask {

  private final OriginalFeatureListOption handleOriginal;
  private Logger logger = Logger.getLogger(this.getClass().getName());

  static final float maximumOverallFeatureHeightAfterNormalization = 100000.0f;

  private final MZmineProject project;
  private ModularFeatureList originalFeatureList, normalizedFeatureList;

  private int processedDataFiles, totalDataFiles;

  private String suffix;
  private NormalizationType normalizationType;
  private FeatureMeasurementType featureMeasurementType;
  private ParameterSet parameters;

  public LinearNormalizerTask(MZmineProject project, FeatureList featureList, ParameterSet parameters, @Nullable
      MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate); // no new data stored -> null

    this.project = project;
    this.originalFeatureList = (ModularFeatureList) featureList;
    this.parameters = parameters;

    totalDataFiles = originalFeatureList.getNumberOfRawDataFiles();

    suffix = parameters.getParameter(LinearNormalizerParameters.suffix).getValue();
    normalizationType =
        parameters.getParameter(LinearNormalizerParameters.normalizationType).getValue();
    featureMeasurementType = parameters.getParameter(
        LinearNormalizerParameters.featureMeasurementType).getValue();
    handleOriginal = parameters.getParameter(LinearNormalizerParameters.handleOriginal).getValue();

  }

  public double getFinishedPercentage() {
    return (double) processedDataFiles / (double) totalDataFiles;
  }

  public String getTaskDescription() {
    return "Linear normalization of " + originalFeatureList + " by " + normalizationType;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running linear normalizer");

    // This hashtable maps rows from original alignment result to rows of
    // the normalized alignment
    Hashtable<FeatureListRow, ModularFeatureListRow> rowMap = new Hashtable<>();

    // Create new feature list
    normalizedFeatureList =
        new ModularFeatureList(originalFeatureList + " " + suffix, getMemoryMapStorage(),
            originalFeatureList.getRawDataFiles());


    originalFeatureList.getRawDataFiles().forEach(file-> normalizedFeatureList.setSelectedScans(file, originalFeatureList.getSeletedScans(file)));
    // Loop through all raw data files, and find the feature with biggest
    // height
    float maxOriginalHeight = 0f;
    for (RawDataFile file : originalFeatureList.getRawDataFiles()) {
      for (FeatureListRow originalFeatureListRow : originalFeatureList.getRows()) {
        Feature p = originalFeatureListRow.getFeature(file);
        if (p != null) {
          if (maxOriginalHeight <= p.getHeight())
            maxOriginalHeight = p.getHeight();
        }
      }
    }

    // Loop through all raw data files, and normalize feature values
    for (RawDataFile file : originalFeatureList.getRawDataFiles()) {

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Determine normalization type and calculate normalization factor
      float normalizationFactor = 1.0f;

      // - normalization by average feature intensity
      if (normalizationType == NormalizationType.AverageIntensity) {
        float intensitySum = 0f;
        int intensityCount = 0;
        for (FeatureListRow featureListRow : originalFeatureList.getRows()) {
          Feature p = featureListRow.getFeature(file);
          if (p != null) {
            if (featureMeasurementType == FeatureMeasurementType.HEIGHT) {
              intensitySum += p.getHeight();
            } else {
              intensitySum += p.getArea();
            }
            intensityCount++;
          }
        }
        normalizationFactor = intensitySum / (float) intensityCount;
      }

      // - normalization by average squared feature intensity
      if (normalizationType == NormalizationType.AverageSquaredIntensity) {
        float intensitySum = 0f;
        int intensityCount = 0;
        for (FeatureListRow featureListRow : originalFeatureList.getRows()) {
          Feature p = featureListRow.getFeature(file);
          if (p != null) {
            if (featureMeasurementType == FeatureMeasurementType.HEIGHT) {
              intensitySum += (p.getHeight() * p.getHeight());
            } else {
              intensitySum += (p.getArea() * p.getArea());
            }
            intensityCount++;
          }
        }
        normalizationFactor = intensitySum / (float) intensityCount;
      }

      // - normalization by maximum feature intensity
      if (normalizationType == NormalizationType.MaximumFeatureHeight) {
        float maximumIntensity = 0;
        for (FeatureListRow featureListRow : originalFeatureList.getRows()) {
          Feature p = featureListRow.getFeature(file);
          if (p != null) {
            if (featureMeasurementType == FeatureMeasurementType.HEIGHT) {
              if (maximumIntensity < p.getHeight())
                maximumIntensity = p.getHeight();
            } else {
              if (maximumIntensity < p.getArea())
                maximumIntensity = p.getArea();
            }

          }
        }
        normalizationFactor = maximumIntensity;
      }

      // - normalization by total raw signal
      if (normalizationType == NormalizationType.TotalRawSignal) {
        normalizationFactor = 0;
        for (Scan scan : file.getScanNumbers(1)) {
          normalizationFactor += Objects.requireNonNullElse(scan.getTIC(), 0f).floatValue();
        }
      }

      // Readjust normalization factor so that maximum height will be
      // equal to maximumOverallFeatureHeightAfterNormalization after
      // normalization
      float maxNormalizedHeight = maxOriginalHeight / normalizationFactor;
      normalizationFactor =
          normalizationFactor * maxNormalizedHeight / maximumOverallFeatureHeightAfterNormalization;

      // Normalize all peak intenisities using the normalization factor
      ObservableList<FeatureListRow> rows = originalFeatureList.getRows();
      for (int i = 0, rowsSize = rows.size(); i < rowsSize; i++) {
        ModularFeatureListRow originalFeatureListRow = (ModularFeatureListRow) rows.get(i);

        // Cancel?
        if (isCanceled()) {
          return;
        }

        Feature originalFeature = originalFeatureListRow.getFeature(file);
        if (originalFeature != null) {

          ModularFeature normalizedFeature = new ModularFeature(normalizedFeatureList, originalFeature);

          float normalizedHeight = originalFeature.getHeight() / normalizationFactor;
          float normalizedArea = originalFeature.getArea() / normalizationFactor;
          normalizedFeature.setHeight(normalizedHeight);
          normalizedFeature.setArea(normalizedArea);

          ModularFeatureListRow normalizedRow = rowMap.get(originalFeatureListRow);

          if (normalizedRow == null) {

            normalizedRow = new ModularFeatureListRow(normalizedFeatureList,
                    originalFeatureListRow, false);

            rowMap.put(originalFeatureListRow, normalizedRow);
          }

          normalizedRow.addFeature(file, normalizedFeature);

        }

      }

      // Progress
      processedDataFiles++;

    }

    // Finally add all normalized rows to normalized alignment result
    for (FeatureListRow originalFeatureListRow : originalFeatureList.getRows()) {
      ModularFeatureListRow normalizedRow = rowMap.get(originalFeatureListRow);
      if (normalizedRow == null)
        continue;
      normalizedFeatureList.addRow(normalizedRow);
    }

    // Add new feature list to the project
    handleOriginal.reflectNewFeatureListToProject(suffix, project, normalizedFeatureList,
        originalFeatureList);

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : originalFeatureList.getAppliedMethods()) {
      normalizedFeatureList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to feature List
    normalizedFeatureList.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        "Linear normalization of by " + normalizationType,
        LinearNormalizerModule.class, parameters, getModuleCallDate()));

    logger.info("Finished linear normalizer");
    setStatus(TaskStatus.FINISHED);

  }

}
