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

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteCorrectionType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class RTCorrectionTask extends AbstractTask {

  private final MZmineProject project;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final List<FeatureList> flists;

  private int processedRows, totalRows;

  private final String suffix;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final double minHeight;
  private final ParameterSet parameters;

  private final WeightedCosineSpectralSimilarity cosine = new WeightedCosineSpectralSimilarity();

  public RTCorrectionTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.flists = Arrays.stream(
            parameters.getParameter(RTCorrectionParameters.featureLists).getValue()
                .getMatchingFeatureLists()).sorted(Comparator.comparing(FeatureList::getNumberOfRows))
        .map(FeatureList.class::cast).toList();
    this.parameters = parameters;

    suffix = parameters.getParameter(RTCorrectionParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(RTCorrectionParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(RTCorrectionParameters.RTTolerance).getValue();
    minHeight = parameters.getParameter(RTCorrectionParameters.minHeight).getValue();
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
    return "Retention time normalization of " + flists.size() + " feature lists";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (flists.size() < 2) {
      setStatus(TaskStatus.FINISHED);
    }

    final List<FeatureListRow> standardCandidates = new ArrayList<>();
    final FeatureList baseList = flists.getFirst();

    final Map<FeatureList, List<FeatureListRow>> mzSortedRows = new HashMap<>();
    flists.forEach(flist -> mzSortedRows.put(flist,
        flist.stream().sorted(Comparator.comparingDouble(FeatureListRow::getAverageMZ)).toList()));

    final List<FeatureListRow> baseRowsSorted = mzSortedRows.get(baseList);
    final List<RtStandard> goodStandards = new ArrayList<>();
    final List<RtStandard> minMatchedStandards = new ArrayList<>();

    // option to only use qcs to create fits

    for (FeatureListRow canditateRow : baseRowsSorted) {
      final RtStandard rtStandard = new RtStandard(canditateRow, flists);
      rtStandard.standards().put(baseList, canditateRow);
      final Scan ms2 = canditateRow.getMostIntenseFragmentScan();
      final DataPoint[] dataPoints =
          ms2 != null ? ScanUtils.extractDataPoints(ms2, true) : new DataPoint[0];

      for (int i = 1; i < flists.size(); i++) {
        final FeatureList flist = flists.get(i);
        final List<FeatureListRow> rows = mzSortedRows.get(flist);
        // todo: check mz uniqueness
        final List<FeatureListRow> candidates = FeatureListUtils.getCandidatesWithinRanges(
            mzTolerance.getToleranceRange(canditateRow.getAverageMZ()),
            rtTolerance.getToleranceRange(canditateRow.getAverageRT()), Range.all(), rows, true);

        if (candidates.isEmpty()) {
          continue;
        } else if (candidates.size() > 1 && dataPoints.length > 4) {
          // todo revisit in
          FeatureListRow bestCandidate = null;
          double bestSim = 0d;
          for (FeatureListRow candidate : candidates) {
            final Scan scan = candidate.getMostIntenseFragmentScan();
            if (scan == null) {
              continue;
            }

            var sim = cosine.getSimilarity(MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA, 4, dataPoints,
                ScanUtils.extractDataPoints(scan, true));
            if (sim != null && sim.getScore() > bestSim) {
              bestSim = sim.getScore();
              bestCandidate = candidate;
            }
          }
          if (bestCandidate != null && bestSim > 0.7d) {
            rtStandard.standards().put(flist, bestCandidate);
          }
        } else if (candidates.size() == 1) {
          rtStandard.standards().put(flist, candidates.getFirst());
        }
      }

      if (rtStandard.isValid()) {
        goodStandards.add(rtStandard);
      }
      if (rtStandard.getNumberOfMatches() > 0.8 * flists.size()) {
        minMatchedStandards.add(rtStandard);
      }
    }

    final DoubleSummaryStatistics stats = goodStandards.stream()
        .mapToDouble(std -> std.row().getAverageRT()).summaryStatistics();
    logger.finest("Found %d good standards that appear in all %d feature lists. %s".formatted(
        goodStandards.size(), flists.size(), stats.toString()));

    setStatus(TaskStatus.FINISHED);
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
    } else if (prevStdIndex == nextStdIndex) {
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
