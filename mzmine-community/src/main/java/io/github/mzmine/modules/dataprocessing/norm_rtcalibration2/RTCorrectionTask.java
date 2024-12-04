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
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DateMetadataColumn;
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

  private final MZmineProject project;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final List<FeatureList> flists;
  private final SampleTypeFilter sampleTypeFilter;

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
    sampleTypeFilter = new SampleTypeFilter(
        parameters.getParameter(RTCorrectionParameters.sampleTypes).getValue());
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
      final String message = "RT recalibration requires feature lists with only one file. Some feature lists contain more than one raw data file (%s).".formatted(
          flistsWithMoreThanOneFile.stream().map(FeatureList::getName)
              .collect(Collectors.joining(", ")));
      final RuntimeException ex = new RuntimeException(message);
      error(message, ex);
      return;
    }

    final Map<FeatureList, List<FeatureListRow>> mzSortedRows = new HashMap<>();
    flists.forEach(flist -> mzSortedRows.put(flist,
        flist.stream().sorted(Comparator.comparingDouble(FeatureListRow::getAverageMZ)).toList()));
    final List<FeatureList> referenceFlists = flists.stream()
        .filter(flist -> flist.getRawDataFiles().stream().allMatch(sampleTypeFilter::matches))
        .sorted(Comparator.comparingInt(FeatureList::getNumberOfRows)).toList();
    final FeatureList baseList = flists.getFirst();

    if (referenceFlists.isEmpty()) {
      throw new RuntimeException(
          "Sample type filter %s does not find any matching feature lists %s.".formatted(
              sampleTypeFilter,
              flists.stream().map(FeatureList::getName).collect(Collectors.joining(", "))));
    }

    final List<RtStandard> goodStandards = findStandards(baseList, referenceFlists, mzSortedRows);
    goodStandards.sort(Comparator.comparingDouble(RtStandard::getMedianRt));
    final List<RtStandard> monotonousStandards = removeNonMonotonousStandards(goodStandards,
        referenceFlists);

    final Map<RawDataFile, RtCalibrationFunction> referenceCalibrations = referenceFlists.stream()
        .map(flist -> new RtCalibrationFunction(flist, monotonousStandards))
        .collect(Collectors.toMap(cali -> cali.getRawDataFile(), cali -> cali));

    // calculate calibrations for other files
    for (FeatureList flist : flists) {
      final RawDataFile file = flist.getRawDataFile(0);
      final RtCalibrationFunction cali = referenceCalibrations.get(file);
      final MetadataTable metadata = project.getProjectMetadata();
      if (cali != null) {
        continue;
      }

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


    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * @param goodStandardsByRt All detected standards sorted by rt.
   * @param referenceFlists   The reference feature lists of these standards.
   * @return
   */
  private static List<RtStandard> removeNonMonotonousStandards(List<RtStandard> goodStandardsByRt,
      List<FeatureList> referenceFlists) {
    final List<RtStandard> monotonousStandards = new ArrayList<>(goodStandardsByRt);
    for (int i = 1; i < monotonousStandards.size(); i++) {
      // check that all rts of this standard are higher than the individual feature lists rts in the previous standard.
      // otherwise we may get non-monotonous scan rts.
      final RtStandard standard = monotonousStandards.get(i);
      final RtStandard previous = monotonousStandards.get(i - 1);

      for (FeatureList referenceFlist : referenceFlists) {
        final FeatureListRow rowA = standard.standards().get(referenceFlist);
        final FeatureListRow rowB = previous.standards().get(referenceFlist);
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

  private @NotNull List<RtStandard> findStandards(FeatureList baseList,
      List<FeatureList> referenceFlists, Map<FeatureList, List<FeatureListRow>> mzSortedRows) {
    final List<FeatureListRow> baseRowsSorted = mzSortedRows.get(baseList);
    final List<RtStandard> goodStandards = new ArrayList<>();

    for (FeatureListRow canditateRow : baseRowsSorted) {
      final RtStandard rtStandard = new RtStandard(flists);
      rtStandard.standards().put(baseList, canditateRow);

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
            rtStandard.standards().put(flist, bestCandidate);
          }
        } else if (candidates.size() == 1 && candidates.getFirst().getMaxHeight() > minHeight) {
          rtStandard.standards().put(flist, candidates.getFirst());
        }
      }

      if (rtStandard.isValid()) {
        goodStandards.add(rtStandard);
      }
    }

    final DoubleSummaryStatistics stats = goodStandards.stream()
        .mapToDouble(RtStandard::getMedianRt).summaryStatistics();
    logger.finest("Found %d good standards that appear in all %d feature lists. %s".formatted(
        goodStandards.size(), flists.size(), stats.toString()));

    return goodStandards;
  }

  private RtCalibrationFunction getPreviousRun(@NotNull RawDataFile file,
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

  private RtCalibrationFunction getNextRun(@NotNull RawDataFile file,
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
}
