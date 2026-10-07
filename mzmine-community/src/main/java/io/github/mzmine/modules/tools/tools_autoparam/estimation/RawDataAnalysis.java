/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.dataprocessing.align_common.BaseFeatureListAligner;
import io.github.mzmine.modules.dataprocessing.align_common.FeatureCloner.SimpleFeatureCloner;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.align_join.JoinRowAlignScorer;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.FeatureWithIsotopeTraces;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.MzToleranceSearchOptions;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.SimpleRunnableTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Raw measurements only. Cross-file alignment runs once, before parameter preparation.
 */
public record RawDataAnalysis(@NotNull List<DataFileStatistics> files, double @NotNull [] fwhms,
                              double @NotNull [] consecutiveScans,
                              double @NotNull [] edgeIntensities, double @NotNull [] heights,
                              double @NotNull [] rtDeviations,
                              double @NotNull [] fileMedianRtDeviations,
                              @NotNull Map<MZTolerance, Integer> sampleMzToleranceCounts) {

  public RawDataAnalysis {
    files = List.copyOf(files);
    sampleMzToleranceCounts = Map.copyOf(sampleMzToleranceCounts);
    fwhms = positiveSorted(fwhms);
    consecutiveScans = positiveSorted(consecutiveScans);
    edgeIntensities = positiveSorted(edgeIntensities);
    heights = positiveSorted(heights);
    rtDeviations = Arrays.stream(rtDeviations).filter(Double::isFinite).filter(value -> value >= 0d)
        .sorted().toArray();
    fileMedianRtDeviations = Arrays.stream(fileMedianRtDeviations).filter(Double::isFinite)
        .filter(value -> value >= 0d).sorted().toArray();
  }

  public static @NotNull RawDataAnalysis analyze(@NotNull List<DataFileStatistics> files) {
    final double[] fwhms = flatten(files, DataFileStatistics::getIsotopePeakFwhms);
    final double[] points = files.stream()
        .map(DataFileStatistics::getNumberOfLowestIsotopeDataPoints).flatMapToInt(Arrays::stream)
        .mapToDouble(value -> value).toArray();
    final double[] edges = flatten(files, DataFileStatistics::getEdgeIntensities);
    final double[] heights = flatten(files,
        DataFileStatistics::getInjectionTimeCorrectedLowestIsotopeHeights);
    if (files.size() < 2) {
      return new RawDataAnalysis(files, fwhms, points, edges, heights, new double[0], new double[0],
          Map.of());
    }

    final ModularFeatureList aligned = alignBenchmarkFeatures(files, null,
        new SimpleRunnableTask(() -> {
        }));
    final int minimumDetections = (int) (files.size() * 0.8);
    return new RawDataAnalysis(files, fwhms, points, edges, heights,
        extractSampleToSampleRtDeviations(aligned, minimumDetections),
        extractFileMedianRtDeviations(aligned, Math.max(3, (int) Math.ceil(files.size() * 0.8))),
        extractSampleToSampleMzToleranceCounts(aligned, minimumDetections));
  }

  /**
   * Per-file median absolute deviations from aligned row medians, in minutes. Require at least five
   * shared observations per file so isolated alignment matches cannot trigger correction.
   */
  static double @NotNull [] extractFileMedianRtDeviations(final @NotNull ModularFeatureList aligned,
      final int minimumDetections) {
    final Map<RawDataFile, DoubleArrayList> deviations = new HashMap<>();
    for (final FeatureListRow row : aligned.getRows()) {
      final List<? extends Feature> features = row.streamFeatures()
          .filter(feature -> feature.getRT() != null && Float.isFinite(feature.getRT())).toList();
      if (features.size() < Math.max(3, minimumDetections)) {
        continue;
      }
      final double[] rts = features.stream().mapToDouble(Feature::getRT).sorted().toArray();
      final double median = MathUtils.calcQuantileSorted(rts, 0.5);
      for (final Feature feature : features) {
        deviations.computeIfAbsent(feature.getRawDataFile(), _ -> new DoubleArrayList())
            .add(Math.abs(feature.getRT() - median));
      }
    }
    return deviations.values().stream().filter(values -> values.size() >= 5).mapToDouble(
            values -> MathUtils.calcQuantileSorted(values.doubleStream().sorted().toArray(), 0.5))
        .sorted().toArray();
  }

  private static double @NotNull [] flatten(@NotNull List<DataFileStatistics> files,
      @NotNull Function<DataFileStatistics, double[]> values) {
    return files.stream().map(values).flatMapToDouble(Arrays::stream).toArray();
  }

  private static double @NotNull [] positiveSorted(double @NotNull [] values) {
    return Arrays.stream(values).filter(Double::isFinite).filter(value -> value > 0d).sorted()
        .toArray();
  }

  private static @NotNull ModularFeatureList alignBenchmarkFeatures(
      @NotNull List<DataFileStatistics> stats, @Nullable MemoryMapStorage storage,
      @NotNull Task parentTask) {

    final List<FeatureList> flists = new ArrayList<>();
    for (final DataFileStatistics stat : stats) {
      final List<ModularFeature> features = stat.featureStatistics().stream()
          .map(FeatureStatistics::getBestEnvelope).flatMap(FeatureWithIsotopeTraces::streamFeatures)
          .toList();
      final ModularFeatureList flist = new ModularFeatureList(stat.file().getName(), storage,
          features.size(), features.size(), stat.file());
      final List<ModularFeature> clonedFeatures = features.stream()
          .map(f -> new ModularFeature(flist, f)).toList();
      for (int i = 0; i < clonedFeatures.size(); i++) {
        flist.addRow(new ModularFeatureListRow(flist, i, clonedFeatures.get(i)));
      }

      flists.add(flist);
    }

    final Optional<MobilityType> imsType = stats.stream().map(DataFileStatistics::file)
        .filter(IMSRawDataFile.class::isInstance).map(IMSRawDataFile.class::cast)
        .map(IMSRawDataFile::getMobilityType).findFirst();

    final ParameterSet alignmentParam = JoinAlignerParameters.create(new MZTolerance(0.02, 10), 2,
        new RTTolerance(0.2f, Unit.MINUTES), 1,
        imsType.map(type -> new MobilityTolerance(type == MobilityType.TIMS ? 0.01f : 2f))
            .orElse(null), imsType.isPresent() ? 1d : null);

    final BaseFeatureListAligner aligner = new BaseFeatureListAligner(parentTask, flists, "aligned",
        storage, new JoinRowAlignScorer(alignmentParam), new SimpleFeatureCloner(),
        FeatureListRowSorter.MZ_ASCENDING, null);
    return aligner.alignFeatureLists();
  }

  private static @NotNull Map<MZTolerance, Integer> extractSampleToSampleMzToleranceCounts(
      @NotNull ModularFeatureList flist, int minDetections) {
    if (minDetections > flist.getNumberOfRawDataFiles()) {
      throw new IllegalStateException(
          "Minimum detections (%d) larger than number of raw data files (%d)".formatted(
              minDetections, flist.getNumberOfRawDataFiles()));
    }

    final Map<MZTolerance, AtomicInteger> toleranceCounter = new HashMap<>();

    for (final FeatureListRow row : flist.getRows()) {
      if (row.getNumberOfFeatures() < minDetections) {
        continue;
      }

      final DoubleSummaryStatistics mzStats = row.streamFeatures().mapToDouble(Feature::getMZ)
          .summaryStatistics();
      final Double mz = row.getAverageMZ();
      final double maxDeviation = Math.max(Math.abs(mzStats.getMax() - mz),
          Math.abs(mz - mzStats.getMin()));

      for (final MZTolerance tol : MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS) {
        if (tol.getMzToleranceForMass(mz) > maxDeviation) {
          final AtomicInteger counter = toleranceCounter.computeIfAbsent(tol,
              _ -> new AtomicInteger(0));
          counter.incrementAndGet();
          break;
        }
      }
    }

    return toleranceCounter.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(Entry::getKey, entry -> entry.getValue().get()));
  }

  /**
   * Every feature's absolute retention time deviation from the mean of its row, sorted.
   * <p>
   * The inter-sample retention time estimate and its search bounds are derived from this same
   * distribution.
   *
   * @param minDetections rows detected in fewer files are skipped, so a deviation is only counted
   *                      where there was something to align against
   */
  private static double @NotNull [] extractSampleToSampleRtDeviations(
      @NotNull ModularFeatureList flist, int minDetections) {
    if (minDetections > flist.getNumberOfRawDataFiles()) {
      throw new IllegalStateException(
          "Minimum detections (%d) larger than number of raw data files (%d)".formatted(
              minDetections, flist.getNumberOfRawDataFiles()));
    }

    final DoubleArrayList differences = new DoubleArrayList();
    for (final FeatureListRow row : flist.getRows()) {
      if (row.getNumberOfFeatures() < minDetections) {
        continue;
      }

      final Float rt = row.getAverageRT();
      differences.addAll(row.streamFeatures().map(Feature::getRT)
          .map(featureRt -> (double) Math.abs(featureRt - rt)).toList());
    }

    return differences.doubleStream().sorted().toArray();
  }

  @Override
  public double @NotNull [] fwhms() {
    return fwhms.clone();
  }

  @Override
  public double @NotNull [] consecutiveScans() {
    return consecutiveScans.clone();
  }

  @Override
  public double @NotNull [] edgeIntensities() {
    return edgeIntensities.clone();
  }

  @Override
  public double @NotNull [] heights() {
    return heights.clone();
  }

  @Override
  public double @NotNull [] rtDeviations() {
    return rtDeviations.clone();
  }

  @Override
  public double @NotNull [] fileMedianRtDeviations() {
    return fileMedianRtDeviations.clone();
  }

}
