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
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarity;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class RTCorrectionTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(RTCorrectionTask.class.getName());

  private final MZmineProject project;
  private final List<FeatureList> flists;
  private final SampleTypeFilter sampleTypeFilter;
  private final double bandwidth;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final double minHeight;
  private final ParameterSet parameters;
  private int processedRows, totalRows;

  public RTCorrectionTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.parameters = parameters;
    this.project = project;
    this.flists = Arrays.stream(
            parameters.getParameter(RTCorrectionParameters.featureLists).getValue()
                .getMatchingFeatureLists()).sorted(Comparator.comparing(FeatureList::getNumberOfRows))
        .map(FeatureList.class::cast).toList();
    mzTolerance = parameters.getParameter(RTCorrectionParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(RTCorrectionParameters.RTTolerance).getValue();
    minHeight = parameters.getParameter(RTCorrectionParameters.minHeight).getValue();
    bandwidth = parameters.getValue(RTCorrectionParameters.correctionBandwidth);
    sampleTypeFilter = new SampleTypeFilter(
        parameters.getParameter(RTCorrectionParameters.sampleTypes).getValue());
  }

  static @NotNull String createUnsatisfiedSampleFilterMessage(SampleTypeFilter sampleTypeFilter,
      List<FeatureList> flists) {
    return "Sample type filter %s does not find any matching feature lists %s.".formatted(
        sampleTypeFilter,
        flists.stream().map(FeatureList::getName).collect(Collectors.joining(", ")));
  }

  static @NotNull String createMoreThanOneFileMessage(List<FeatureList> flistsWithMoreThanOneFile) {
    final String message = "RT recalibration requires feature lists with only one file. Some feature lists contain more than one raw data file (%s).".formatted(
        flistsWithMoreThanOneFile.stream().map(FeatureList::getName)
            .collect(Collectors.joining(", ")));
    return message;
  }

  static @NotNull List<RtCalibrationFunction> interpolateMissingCalibrations(
      List<FeatureList> referenceFlists, List<FeatureList> allFeatureLists, MetadataTable metadata,
      List<RtStandard> monotonousStandards, double bandwidth) {
    final Map<RawDataFile, RtCalibrationFunction> referenceCalibrations = referenceFlists.stream()
        .map(flist -> new RtCalibrationFunction(flist, monotonousStandards, bandwidth))
        .collect(Collectors.toMap(RtCalibrationFunction::getRawDataFile, cali -> cali));

    // the rt calibration automatically increases the bandwidth if the function is not monotonous
    final double iterativeBandwidth = referenceCalibrations.values().stream()
        .mapToDouble(RtCalibrationFunction::getOptimisedBandwidth).max().orElse(bandwidth);
    if (iterativeBandwidth > bandwidth) {
      // if the bandwidth had to be increased, re-run all calibrations with the new bandwidth to make them comparable
      logger.info(
          "While calibrating on reference files files, the bandwidth had to be increased to create a monotonous calibration. Rerunning with bandwidth %.3f".formatted(
              iterativeBandwidth));
      return interpolateMissingCalibrations(referenceFlists, allFeatureLists, metadata,
          monotonousStandards, iterativeBandwidth);
    }

    final List<RtCalibrationFunction> allCalibrations = new ArrayList<>(
        referenceCalibrations.values());
    // calculate calibrations for other files
    for (FeatureList flist : allFeatureLists) {
      final RawDataFile file = flist.getRawDataFile(0);
      final RtCalibrationFunction cali = referenceCalibrations.get(file);
      if (cali != null) {
        continue;
      }

      // if we cannot find one cali, just reuse the same as previous and next. Makes downstream implementation easier.
      RtCalibrationFunction previousCali = getPreviousRun(file, referenceCalibrations, metadata);
      RtCalibrationFunction nextCali = getNextRun(file, referenceCalibrations, metadata);
      previousCali = previousCali == null ? nextCali : previousCali;
      nextCali = nextCali == null ? previousCali : nextCali;
      if (previousCali == null) {
        throw new IllegalStateException(
            "Could not find previous or next run for file %s".formatted(file.getName()));
      }

      final LocalDateTime runDate = metadata.getValue(metadata.getRunDateColumn(), file);
      final LocalDateTime previousRunDate = metadata.getValue(metadata.getRunDateColumn(),
          previousCali.getRawDataFile());
      final LocalDateTime nextRunDate = metadata.getValue(metadata.getRunDateColumn(),
          nextCali.getRawDataFile());

      final long totalTimeDistance =
          Math.abs(runDate.until(nextRunDate, ChronoUnit.SECONDS)) + Math.abs(
              runDate.until(previousRunDate, ChronoUnit.SECONDS));
      final double previousWeight =
          (double) Math.abs(runDate.until(nextRunDate, ChronoUnit.SECONDS)) / totalTimeDistance;
      final double nextRunWeight =
          (double) Math.abs(runDate.until(previousRunDate, ChronoUnit.SECONDS)) / totalTimeDistance;

      final RtCalibrationFunction newCali = new RtCalibrationFunction(file, monotonousStandards,
          previousCali, previousWeight, nextCali, nextRunWeight, bandwidth);
      allCalibrations.add(newCali);
    }

    // the rt calibration automatically increases the bandwidth if the function is not monotonous
    final double allCalisIterativeBandwidth = allCalibrations.stream()
        .mapToDouble(RtCalibrationFunction::getOptimisedBandwidth).max().orElse(bandwidth);
    if (allCalisIterativeBandwidth > bandwidth) {
      logger.info(
          "After transferring calibrations to all files, the bandwidth had to be increased to create a monotonous calibration. Rerunning with bandwidth %.3f".formatted(
              allCalisIterativeBandwidth));
      // if the bandwidth had to be increased, re-run all calibrations with the new bandwidth to make them comparable
      return interpolateMissingCalibrations(referenceFlists, allFeatureLists, metadata,
          monotonousStandards, allCalisIterativeBandwidth);
    }

    return allCalibrations;
  }

  /**
   * @param goodStandardsByRt All detected standards sorted by rt.
   * @param referenceFlists   The reference feature lists of these standards.
   * @return
   */
  static List<RtStandard> removeNonMonotonousStandards(List<RtStandard> goodStandardsByRt,
      List<FeatureList> referenceFlists) {
    final List<RtStandard> monotonousStandards = new ArrayList<>(goodStandardsByRt);
    for (int i = 1; i < monotonousStandards.size(); i++) {
      // check that all rts of this standard are higher than the individual feature lists rts in the previous standard.
      // otherwise we may get non-monotonous scan rts.
      final RtStandard standard = monotonousStandards.get(i);
      final RtStandard previous = monotonousStandards.get(i - 1);

      for (FeatureList referenceFlist : referenceFlists) {
        final FeatureListRow rowA = standard.standards().get(referenceFlist.getRawDataFile(0));
        final FeatureListRow rowB = previous.standards().get(referenceFlist.getRawDataFile(0));
        if (rowA == null || rowB == null) {
          throw new IllegalStateException(
              "Not all standards found in all reference feature lists. This should not be the case.");
        }

        // if the previous rt is smaller than this rt, although it is the other way round for the
        // average across all feature lists, we cannot use this standard.
        if (rowA.getAverageRT() <= rowB.getAverageRT()) {
          monotonousStandards.remove(i);
          i--; // decrement by one so we don't skip a standard
          break;
        }
      }
    }
    monotonousStandards.sort(Comparator.comparingDouble(RtStandard::getMedianRt));
    return monotonousStandards;
  }

  static @NotNull List<RtStandard> findStandards(FeatureList baseList,
      List<FeatureList> referenceFlists, Map<FeatureList, List<FeatureListRow>> mzSortedRows,
      MZTolerance mzTolerance, RTTolerance rtTolerance, double minHeight) {
    final List<FeatureListRow> baseRowsSorted = mzSortedRows.get(baseList);
    final List<RtStandard> goodStandards = new ArrayList<>();
    final WeightedCosineSpectralSimilarity cosine = new WeightedCosineSpectralSimilarity();

    for (FeatureListRow canditateRow : baseRowsSorted) {
      final RtStandard rtStandard = new RtStandard(referenceFlists);
      rtStandard.standards().put(baseList.getRawDataFile(0), canditateRow);

      final Scan ms2 = canditateRow.getMostIntenseFragmentScan();
      final DataPoint[] dataPoints =
          ms2 != null ? ScanUtils.extractDataPoints(ms2, true) : new DataPoint[0];

      for (int i = 1; i < referenceFlists.size(); i++) {
        final FeatureList flist = referenceFlists.get(i);
        final List<FeatureListRow> rows = mzSortedRows.get(flist);
        // todo: check mz uniqueness
        final List<FeatureListRow> candidates = FeatureListUtils.getCandidatesWithinRanges(
            mzTolerance.getToleranceRange(canditateRow.getAverageMZ()),
            rtTolerance.getToleranceRange(canditateRow.getAverageRT()), Range.all(), rows, true);

        final List<FeatureListRow> extendedCandidates = FeatureListUtils.getCandidatesWithinRanges(
            mzTolerance.getToleranceRange(canditateRow.getAverageMZ()),
            new RTTolerance(rtTolerance.getToleranceInMinutes() * 3,
                Unit.MINUTES).getToleranceRange(canditateRow.getAverageRT()), Range.all(), rows,
            true);

        if (candidates.isEmpty()) {
          continue;
        } else if (candidates.size() > 1 && dataPoints.length > 4) {
          // todo revisit in
          FeatureListRow bestCandidate = null;
          double bestSim = 0d;
          for (FeatureListRow candidate : candidates) {
            final Scan scan = candidate.getMostIntenseFragmentScan();
            if (scan == null || candidate.getMaxHeight() < minHeight) {
              continue;
            }

            var sim = cosine.getSimilarity(MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA, 4, dataPoints,
                ScanUtils.extractDataPoints(scan, true));
            if (sim != null && sim.getScore() > bestSim) {
              bestSim = sim.getScore();
              bestCandidate = candidate;
            }
          }
          if (bestCandidate != null && bestSim > 0.9d) {
            rtStandard.standards().put(flist.getRawDataFile(0), bestCandidate);
          }
        } else if (candidates.size() == 1 && candidates.getFirst().getMaxHeight() > minHeight
            && extendedCandidates.size() == 1) {
          rtStandard.standards().put(flist.getRawDataFile(0), candidates.getFirst());
        }
      }

      if (rtStandard.isValid()) {
        goodStandards.add(rtStandard);
      }
    }

    final DoubleSummaryStatistics stats = goodStandards.stream()
        .mapToDouble(RtStandard::getMedianRt).summaryStatistics();
    logger.finest("Found %d good standards that appear in all %d feature lists. %s".formatted(
        goodStandards.size(), referenceFlists.size(), stats.toString()));

    return goodStandards;
  }

  private static RtCalibrationFunction getPreviousRun(@NotNull RawDataFile file,
      @NotNull Map<RawDataFile, @NotNull RtCalibrationFunction> functions,
      @NotNull MetadataTable metadata) {
    final DateMetadataColumn runDateColumn = metadata.getRunDateColumn();
    final LocalDateTime runDate = metadata.getValue(runDateColumn, file);
    if (runDate == null) {
      throw new FileHasNoRunDateException(file);
    }

    long minPreviousRun = Long.MIN_VALUE;
    RtCalibrationFunction previousRunCali = null;

    for (RtCalibrationFunction function : functions.values()) {
      final RawDataFile thatFile = function.getRawDataFile();
      final LocalDateTime thatDate = metadata.getValue(runDateColumn, thatFile);
      if (thatDate == null) {
        throw new FileHasNoRunDateException(thatFile);
      }

      final long diff = runDate.until(thatDate, ChronoUnit.SECONDS);
      if (diff < 0 && diff > minPreviousRun) {
        minPreviousRun = diff;
        previousRunCali = function;
      }
    }

    return previousRunCali;
  }

  private static RtCalibrationFunction getNextRun(@NotNull RawDataFile file,
      @NotNull Map<RawDataFile, @NotNull RtCalibrationFunction> functions,
      @NotNull MetadataTable metadata) {
    final DateMetadataColumn runDateColumn = metadata.getRunDateColumn();
    final LocalDateTime runDate = metadata.getValue(runDateColumn, file);
    if (runDate == null) {
      throw new FileHasNoRunDateException(file);
    }

    long minNextRun = Long.MAX_VALUE;
    RtCalibrationFunction nextRunCali = null;

    for (RtCalibrationFunction function : functions.values()) {
      final RawDataFile thatFile = function.getRawDataFile();
      final LocalDateTime thatDate = metadata.getValue(runDateColumn, thatFile);
      if (thatDate == null) {
        throw new FileHasNoRunDateException(thatFile);
      }

      final long diff = runDate.until(thatDate, ChronoUnit.SECONDS);
      if (diff > 0 && diff < minNextRun) {
        minNextRun = diff;
        nextRunCali = function;
      }
    }

    return nextRunCali;
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

    final List<FeatureList> flistsWithMoreThanOneFile = flists.stream()
        .filter(fl -> fl.getNumberOfRawDataFiles() > 1).toList();
    if (!flistsWithMoreThanOneFile.isEmpty()) {
      final RuntimeException ex = new RuntimeException(
          createMoreThanOneFileMessage(flistsWithMoreThanOneFile));
      error(ex.getMessage(), ex);
      return;
    }

    final List<FeatureList> referenceFlistsByNumRows = flists.stream()
        .filter(flist -> flist.getRawDataFiles().stream().allMatch(sampleTypeFilter::matches))
        .sorted(Comparator.comparingInt(FeatureList::getNumberOfRows)).toList();
    if (referenceFlistsByNumRows.isEmpty()) {
      final RuntimeException ex = new RuntimeException(
          createUnsatisfiedSampleFilterMessage(sampleTypeFilter, flists));
      error(ex.getMessage(), ex);
      return;
    }

    final FeatureList baseList = referenceFlistsByNumRows.getFirst();
    final Map<FeatureList, List<FeatureListRow>> mzSortedRows = new HashMap<>();
    referenceFlistsByNumRows.forEach(flist -> mzSortedRows.put(flist,
        flist.stream().sorted(Comparator.comparingDouble(FeatureListRow::getAverageMZ)).toList()));

    final List<RtStandard> goodStandards = findStandards(baseList, referenceFlistsByNumRows,
        mzSortedRows, mzTolerance, rtTolerance, minHeight);
    goodStandards.sort(Comparator.comparingDouble(RtStandard::getMedianRt));
    final List<RtStandard> monotonousStandards = removeNonMonotonousStandards(goodStandards,
        referenceFlistsByNumRows);
    final List<RtCalibrationFunction> allCalibrations = interpolateMissingCalibrations(
        referenceFlistsByNumRows, flists, project.getProjectMetadata(), monotonousStandards,
        bandwidth);

    for (RtCalibrationFunction cali : allCalibrations) {
      final RawDataFile file = cali.getRawDataFile();
      for (Scan scan : file.getScans()) {
        ((SimpleScan) scan).setCorrectedRetentionTime(
            cali.getCorrectedRtLoess(scan.getRetentionTime()));
      }
    }

    for (FeatureList flist : flists) {
      flist.streamFeatures().forEach(FeatureDataUtils::recalculateIonSeriesDependingTypes);
    }

    setStatus(TaskStatus.FINISHED);
  }
}
