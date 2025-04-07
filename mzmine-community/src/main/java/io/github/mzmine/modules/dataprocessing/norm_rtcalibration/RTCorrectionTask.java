/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteCorrectionType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class RTCorrectionTask extends AbstractTask {

  private final OriginalFeatureListOption handleOriginal;
  private final MZmineProject project;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final ModularFeatureList[] originalFeatureLists;
  private ModularFeatureList[] normalizedFeatureLists;

  private int processedRows, totalRows;

  private final String suffix;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final double minHeight;
  private final ParameterSet parameters;

  public RTCorrectionTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.originalFeatureLists = parameters.getParameter(RTCorrectionParameters.featureLists)
        .getValue().getMatchingFeatureLists();
    this.parameters = parameters;

    suffix = parameters.getParameter(RTCorrectionParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(RTCorrectionParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(RTCorrectionParameters.RTTolerance).getValue();
    minHeight = parameters.getParameter(RTCorrectionParameters.minHeight).getValue();
    handleOriginal = parameters.getParameter(RTCorrectionParameters.handleOriginal).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0f;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Retention time normalization of " + originalFeatureLists.length + " feature lists";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Running retention time normalizer");
    Arrays.sort(originalFeatureLists,
        Comparator.comparingInt(featureList -> featureList.getRows().size()));
    totalRows = originalFeatureLists[0].getNumberOfRows();
    normalizedFeatureLists = new ModularFeatureList[originalFeatureLists.length];

    for (int i = 0; i < originalFeatureLists.length; i++) {
      normalizedFeatureLists[i] = new ModularFeatureList(originalFeatureLists[i] + " " + suffix,
          getMemoryMapStorage(), originalFeatureLists[i].getRawDataFiles());
      totalRows += originalFeatureLists[i].getNumberOfRows();
    }

    List<ModularFeatureListRow[]> goodStandards = findGoodStandards();
    double[] averagedRTs = goodStandards.stream().mapToDouble(this::averageRT).toArray();

    IntStream.range(0, originalFeatureLists.length).parallel().forEach(i -> {
      normalizeFeatureList(originalFeatureLists[i], normalizedFeatureLists[i],
          goodStandards.stream().map(row -> row[i]).toArray(ModularFeatureListRow[]::new),
          averagedRTs);
    });
    if (isCanceled()) {
      return;
    }

    for (int i = 0; i < originalFeatureLists.length; i++) {
      for (FeatureListAppliedMethod proc : originalFeatureLists[i].getAppliedMethods()) {
        normalizedFeatureLists[i].addDescriptionOfAppliedTask(proc);
      }

      for (RawDataFile f : originalFeatureLists[i].getRawDataFiles()) {
        normalizedFeatureLists[i].setSelectedScans(f, originalFeatureLists[i].getSeletedScans(f));
      }

      normalizedFeatureLists[i].addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("Retention time normalization",
              RTCorrectionModule.class, parameters, getModuleCallDate()));

      handleOriginal.reflectNewFeatureListToProject(suffix, project, normalizedFeatureLists[i],
          originalFeatureLists[i]);
    }

    logger.info("Finished retention time normalizer");
    setStatus(TaskStatus.FINISHED);
  }

  private List<ModularFeatureListRow[]> findGoodStandards() {
    List<ModularFeatureListRow[]> goodStandards = new ArrayList<>();
    ObservableList<FeatureListRow> rows = originalFeatureLists[0].getRows();

    for (FeatureListRow row : rows) {
      if (isCanceled()) {
        return goodStandards;
      }

      ModularFeatureListRow candidate = (ModularFeatureListRow) row;
      processedRows++;

      if (candidate.getFeatures().stream().anyMatch(p -> p.getHeight() < minHeight)) {
        continue;
      }

      ModularFeatureListRow[] goodStandardCandidate = new ModularFeatureListRow[originalFeatureLists.length];
      goodStandardCandidate[0] = candidate;

      double candidateMZ = candidate.getAverageMZ();
      float candidateRT = candidate.getAverageRT();

      boolean isGoodStandard = true;
      if (originalFeatureLists.length > 1) {

        for (int i = 1; i < originalFeatureLists.length; i++) {
          Range<Float> rtRange = rtTolerance.getToleranceRange(candidateRT);
          Range<Double> mzRange = mzTolerance.getToleranceRange(candidateMZ);
          ModularFeatureListRow[] matchingRows = originalFeatureLists[i].getRowsInsideScanAndMZRange(
              rtRange, mzRange).toArray(new ModularFeatureListRow[0]);

          if (matchingRows.length != 1
              || Objects.requireNonNull(matchingRows[0].getBestFeature()).getHeight() < minHeight) {
            isGoodStandard = false;
            break;
          }

          goodStandardCandidate[i] = matchingRows[0];
        }
      } else {
        isGoodStandard = false;
      }

      if (isGoodStandard) {
        goodStandards.add(goodStandardCandidate);
        logger.finest("Found a good standard for RT normalization: " + candidate);
      }
    }
    return goodStandards;
  }

  private double averageRT(ModularFeatureListRow[] rows) {
    return Arrays.stream(rows).mapToDouble(ModularFeatureListRow::getAverageRT).average().orElse(0);
  }

  private void normalizeFeatureList(ModularFeatureList originalFeatureList,
      ModularFeatureList normalizedFeatureList, ModularFeatureListRow[] standards,
      double[] normalizedStdRTs) {

    for (FeatureListRow originalRow : originalFeatureList.getRows()) {
      if (isCanceled()) {
        return;
      }

      ModularFeatureListRow normalizedRow = normalizeRow(normalizedFeatureList,
          (ModularFeatureListRow) originalRow,
          standards, normalizedStdRTs);
      normalizedFeatureList.addRow(normalizedRow);
      processedRows++;
    }
  }

  /**
   * Normalize retention time of given row using selected standards
   *
   * @param originalRow      Feature list row to be normalized
   * @param standards        Standard rows in same feature list
   * @param normalizedStdRTs Normalized retention times of standard rows
   * @return New feature list row with normalized retention time
   */
  private ModularFeatureListRow normalizeRow(ModularFeatureList targetFeatureList,
      ModularFeatureListRow originalRow, ModularFeatureListRow[] standards,
      double[] normalizedStdRTs) {

    ModularFeatureListRow normalizedRow = new ModularFeatureListRow(targetFeatureList, originalRow,
        false);

    int prevStdIndex = -1, nextStdIndex = -1;

    for (int stdIndex = 0; stdIndex < standards.length; stdIndex++) {
      if (standards[stdIndex] == originalRow) {
        prevStdIndex = stdIndex;
        nextStdIndex = stdIndex;
        break;
      }

      if (standards[stdIndex].getAverageRT() < originalRow.getAverageRT()) {
        if (prevStdIndex == -1
            || standards[stdIndex].getAverageRT() > standards[prevStdIndex].getAverageRT()) {
          prevStdIndex = stdIndex;
        }
      } else if (standards[stdIndex].getAverageRT() > originalRow.getAverageRT()) {
        if (nextStdIndex == -1
            || standards[stdIndex].getAverageRT() < standards[nextStdIndex].getAverageRT()) {
          nextStdIndex = stdIndex;
        }
      }
    }

    double originalRT = originalRow.getAverageRT();
    double normalizedRT = originalRT;
    if (standards.length >= 1) {
      normalizedRT = calculateNormalizedRT(originalRT, prevStdIndex, nextStdIndex, standards,
          normalizedStdRTs);
    }

    for (RawDataFile file : originalRow.getRawDataFiles()) {
      ModularFeature originalFeature = originalRow.getFeature(file);
      if (originalFeature != null) {
        ModularFeature normalizedFeature = new ModularFeature(targetFeatureList, originalFeature);
        normalizedFeature.setRT((float) normalizedRT);
        float correctedRt = (float) (normalizedRT - originalRT);
        normalizedFeature.set(RtAbsoluteCorrectionType.class, correctedRt);
        normalizedRow.addFeature(file, normalizedFeature);
      }
    }

    return normalizedRow;
  }

  private double calculateNormalizedRT(double originalRT, int prevStdIndex, int nextStdIndex,
      ModularFeatureListRow[] standards, double[] normalizedStdRTs) {

    if (standards.length == 1) {
      return originalRT + (normalizedStdRTs[0] - standards[0].getAverageRT());
    } else if (prevStdIndex == nextStdIndex && prevStdIndex != -1) {
      return normalizedStdRTs[prevStdIndex];
    } else if (prevStdIndex != -1 && nextStdIndex != -1) {
      double weight = (originalRT - standards[prevStdIndex].getAverageRT()) / (
          standards[nextStdIndex].getAverageRT() - standards[prevStdIndex].getAverageRT());
      return normalizedStdRTs[prevStdIndex] + (weight * (normalizedStdRTs[nextStdIndex]
          - normalizedStdRTs[prevStdIndex]));
    } else {
      return originalRT;
    }
  }

}
